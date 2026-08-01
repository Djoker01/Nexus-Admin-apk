package com.nexus.admin.data.sync

import android.content.Context
import android.util.Base64
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nexus.admin.data.AppDatabase
import com.nexus.admin.data.entity.*
import com.nexus.admin.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

class OfflineSyncManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val gson = GsonBuilder().create()

    /**
     * EXPORTAR: Generar string comprimido con ventas, caja, stock y cuentas por cobrar
     */
    suspend fun exportSalesToQr(workerName: String, businessCode: String): String = withContext(Dispatchers.IO) {
        try {
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            
            // Ventas del día
            val todaySales = db.saleDao().getAllSales().first()
                .filter { it.date in todayStart..todayEnd && !it.isReturned }
            
            // Movimientos de caja del día
            val todayCash = db.cashMovementDao().getAllMovements().first()
                .filter { it.date in todayStart..todayEnd }
            
            // Stock actual
            val products = db.productDao().getAllProducts().first()
            
            // Cuentas por cobrar activas (pendientes y parciales)
            val activeReceivables = db.receivableDao().getAllReceivables().first()
                .filter { it.status != "paid" }

            val syncPackage = SyncPackage(
                businessCode = businessCode,
                workerName = workerName,
                timestamp = System.currentTimeMillis(),
                sales = todaySales.map { sale ->
                    SyncSaleQr(
                        client = sale.client,
                        total = sale.total,
                        cost = sale.cost,
                        paymentMethod = sale.paymentMethod,
                        date = sale.date,
                        products = sale.products.joinToString(";") { sp ->
                            "${sp.name},${sp.quantity},${sp.price},${sp.cost}"
                        },
                        isReceivable = sale.isReceivable
                    )
                },
                cashMovements = todayCash.map { m ->
                    SyncCashMovementQr(
                        type = m.type,
                        amount = m.amount,
                        description = m.description,
                        date = m.date
                    )
                },
                products = products.map { p ->
                    SyncProductQr(
                        name = p.name,
                        sku = p.sku,
                        price = p.price,
                        cost = p.cost,
                        stock = p.stock,
                        minStock = p.minStock
                    )
                },
                receivables = activeReceivables.map { r ->
                    SyncReceivableQr(
                        clientName = r.clientName,
                        concept = r.concept,
                        totalAmount = r.totalAmount,
                        balance = r.balance,
                        status = r.status,
                        date = r.date
                    )
                }
            )

            val json = gson.toJson(syncPackage)
            val compressed = compress(json)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "✅ ${todaySales.size} ventas, ${activeReceivables.size} ctas por cobrar exportadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            compressed
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
            }
            ""
        }
    }

    /**
     * IMPORTAR: Recibir string comprimido y actualizar BD local
     */
    suspend fun importSalesFromQr(compressedData: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json = decompress(compressedData)
            val syncPackage = gson.fromJson(json, SyncPackage::class.java)

            var importedSales = 0
            var importedCash = 0
            var updatedProducts = 0
            var importedReceivables = 0

            // 1. Importar ventas
            syncPackage.sales.forEach { s ->
                val saleProducts = s.products.split(";").mapNotNull { sp ->
                    val parts = sp.split(",")
                    if (parts.size >= 4) {
                        SaleProduct(
                            productId = 0,
                            name = parts[0],
                            quantity = parts[1].toIntOrNull() ?: 1,
                            price = parts[2].toDoubleOrNull() ?: 0.0,
                            cost = parts[3].toDoubleOrNull() ?: 0.0
                        )
                    } else null
                }
                if (saleProducts.isNotEmpty()) {
                    // Verificar si ya existe esta venta (evitar duplicados)
                    val existingSales = db.saleDao().getAllSales().first()
                    val isDuplicate = existingSales.any { existing ->
                        existing.client == s.client &&
                        existing.total == s.total &&
                        existing.date == s.date
                    }
                    
                    if (!isDuplicate) {
                        db.saleDao().insert(
                            Sale(
                                client = s.client,
                                products = saleProducts,
                                total = s.total,
                                cost = s.cost,
                                paymentMethod = s.paymentMethod,
                                date = s.date,
                                isReceivable = s.isReceivable
                            )
                        )
                        importedSales++
                    }
                }
            }

            // 2. Importar movimientos de caja
            syncPackage.cashMovements.forEach { m ->
                val existingCash = db.cashMovementDao().getAllMovements().first()
                val isDuplicate = existingCash.any { existing ->
                    existing.amount == m.amount &&
                    existing.description == m.description &&
                    existing.date == m.date
                }
                
                if (!isDuplicate) {
                    db.cashMovementDao().insert(
                        CashMovement(
                            type = m.type,
                            amount = m.amount,
                            description = "${m.description} (Sync: ${syncPackage.workerName})",
                            date = m.date
                        )
                    )
                    importedCash++
                }
            }

            // 3. Actualizar stock (solo si el stock remoto es menor = más reciente)
            syncPackage.products.forEach { p ->
                val existing = db.productDao().getAllProducts().first()
                    .find { it.sku == p.sku }
                if (existing != null && p.stock < existing.stock) {
                    db.productDao().update(existing.copy(stock = p.stock))
                    updatedProducts++
                }
            }

            // 4. Importar cuentas por cobrar (NUEVO)
            syncPackage.receivables.forEach { r ->
                val existingReceivables = db.receivableDao().getAllReceivables().first()
                val isDuplicate = existingReceivables.any { existing ->
                    existing.clientName == r.clientName &&
                    existing.concept == r.concept &&
                    existing.totalAmount == r.totalAmount
                }
                
                if (!isDuplicate) {
                    db.receivableDao().insert(
                        Receivable(
                            clientName = r.clientName,
                            concept = r.concept,
                            totalAmount = r.totalAmount,
                            balance = r.balance,
                            status = r.status,
                            date = r.date
                        )
                    )
                    importedReceivables++
                }
            }

            val result = ImportResult(
                success = true,
                salesImported = importedSales,
                cashImported = importedCash,
                productsUpdated = updatedProducts,
                receivablesImported = importedReceivables,
                workerName = syncPackage.workerName
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "✅ ${result.salesImported} ventas, ${result.receivablesImported} ctas por cobrar de ${result.workerName}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            result
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
            }
            ImportResult(success = false)
        }
    }

    /**
     * Comprimir string para que quepa en QR
     */
    private fun compress(data: String): String {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            baos.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Descomprimir string desde QR
     */
    private fun decompress(data: String): String {
        val inflater = Inflater()
        inflater.setInput(Base64.decode(data, Base64.NO_WRAP))
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            baos.write(buffer, 0, inflater.inflate(buffer))
        }
        inflater.end()
        return String(baos.toByteArray(), Charsets.UTF_8)
    }
}
