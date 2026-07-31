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

    suspend fun exportSalesToQr(workerName: String, businessCode: String): String = withContext(Dispatchers.IO) {
        try {
            val todayStart = Utils.getTodayStart()
            val todayEnd = Utils.getTodayEnd()
            val todaySales = db.saleDao().getAllSales().first()
                .filter { it.date in todayStart..todayEnd && !it.isReturned }
            val todayCash = db.cashMovementDao().getAllMovements().first()
                .filter { it.date in todayStart..todayEnd }
            val products = db.productDao().getAllProducts().first()

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
                        }
                    )
                },
                cashMovements = todayCash.map { m ->
                    SyncCashMovementQr(m.type, m.amount, m.description, m.date)
                },
                products = products.map { p ->
                    SyncProductQr(p.name, p.sku, p.price, p.cost, p.stock, p.minStock)
                }
            )

            val json = gson.toJson(syncPackage)
            val compressed = compress(json)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ ${todaySales.size} ventas exportadas", Toast.LENGTH_SHORT).show()
            }
            compressed
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            ""
        }
    }

    suspend fun importSalesFromQr(compressedData: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json = decompress(compressedData)
            val syncPackage = gson.fromJson(json, SyncPackage::class.java)

            var importedSales = 0
            var importedCash = 0
            var updatedProducts = 0

            syncPackage.sales.forEach { s ->
                val saleProducts = s.products.split(";").mapNotNull { sp ->
                    val parts = sp.split(",")
                    if (parts.size >= 4) {
                        SaleProduct(0, parts[0], parts[1].toIntOrNull() ?: 1, parts[2].toDoubleOrNull() ?: 0.0, parts[3].toDoubleOrNull() ?: 0.0)
                    } else null
                }
                if (saleProducts.isNotEmpty()) {
                    db.saleDao().insert(Sale(client = s.client, products = saleProducts, total = s.total, cost = s.cost, paymentMethod = s.paymentMethod, date = s.date))
                    importedSales++
                }
            }

            syncPackage.cashMovements.forEach { m ->
                db.cashMovementDao().insert(CashMovement(type = m.type, amount = m.amount, description = "${m.description} (${syncPackage.workerName})", date = m.date))
                importedCash++
            }

            syncPackage.products.forEach { p ->
                val existing = db.productDao().getAllProducts().first().find { it.sku == p.sku }
                if (existing != null && p.stock < existing.stock) {
                    db.productDao().update(existing.copy(stock = p.stock))
                    updatedProducts++
                }
            }

            val result = ImportResult(true, importedSales, importedCash, updatedProducts, syncPackage.workerName)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ ${result.salesImported} ventas de ${result.workerName}", Toast.LENGTH_SHORT).show()
            }
            result
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            ImportResult()
        }
    }

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
