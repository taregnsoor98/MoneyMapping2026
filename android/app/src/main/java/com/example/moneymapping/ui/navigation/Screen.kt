package com.example.moneymapping.ui.navigation

// Defines all the screens in the app as sealed class objects
// Each screen has a unique route string used for navigation
sealed class Screen(val route: String) {

    object Home : Screen("home")         // Main dashboard screen
    object History : Screen("history")   // Expense history screen
    object Groups : Screen("groups")     // Groups management screen
    object Profile : Screen("profile")   // User profile & settings screen
    object AddExpense : Screen("add_expense") // route for the add expense screen
    object DetailExpense : Screen("detail_expense/{expenseId}") // route for the expense detail screen //
    object EditExpense : Screen("edit_expense/{expenseId}") // route for the edit expense screen
}