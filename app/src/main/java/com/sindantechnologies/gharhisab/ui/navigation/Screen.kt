package com.sindantechnologies.gharhisab.ui.navigation

/**
 * Navigation route definitions for GharHisab.
 *
 * Each object represents a unique screen with its route string.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object LanguageSelection : Screen("language_selection")
    data object ThemeSelection : Screen("theme_selection")
    data object StorageSelection : Screen("storage_selection")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object AddTransaction : Screen("add_transaction")
    data object Transactions : Screen("transactions")
    data object Budgets : Screen("budgets")
    data object PlannedExpenses : Screen("planned_expenses")
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
}
