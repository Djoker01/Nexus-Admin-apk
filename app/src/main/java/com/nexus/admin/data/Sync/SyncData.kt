package com.nexus.admin.data.sync

data class SyncPackage(
    val businessCode: String = "",
    val workerName: String = "",
    val timestamp: Long = 0,
    val sales: List<SyncSaleQr> = emptyList(),
    val cashMovements: List<SyncCashMovementQr> = emptyList(),
    val products: List<SyncProductQr> = emptyList(),
    val receivables: List<SyncReceivableQr> = emptyList()
)

data class SyncSaleQr(
    val client: String,
    val total: Double,
    val cost: Double,
    val paymentMethod: String,
    val date: Long,
    val products: String,
    val isReceivable: Boolean = false
)

data class SyncCashMovementQr(
    val type: String,
    val amount: Double,
    val description: String,
    val date: Long
)

data class SyncProductQr(
    val name: String,
    val sku: String,
    val price: Double,
    val cost: Double,
    val stock: Int,
    val minStock: Int
)

data class SyncReceivableQr(
    val clientName: String,
    val concept: String,
    val totalAmount: Double,
    val balance: Double,
    val status: String,
    val date: Long
)

data class ImportResult(
    val success: Boolean = false,
    val salesImported: Int = 0,
    val cashImported: Int = 0,
    val productsUpdated: Int = 0,
    val receivablesImported: Int = 0,
    val workerName: String = ""
)
