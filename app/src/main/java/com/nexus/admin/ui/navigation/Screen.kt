package com.nexus.admin.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Inventory : Screen("inventory", "Inventario")
    object Sales : Screen("sales", "Ventas")
    object Cash : Screen("cash", "Caja")
    object Expenses : Screen("expenses", "Gastos")
    object Receivables : Screen("receivables", "Ctas por Cobrar")
    object Shrinkage : Screen("shrinkage", "Mermas")
    object Restock : Screen("restock", "Reabastecimiento")
    object Reports : Screen("reports", "Reportes")
    object Backup : Screen("backup", "Respaldos")

    companion object {
        val items = listOf(
            Dashboard, Inventory, Sales, Cash, Expenses,
            Receivables, Shrinkage, Restock, Reports, Backup
        )
    }
}
