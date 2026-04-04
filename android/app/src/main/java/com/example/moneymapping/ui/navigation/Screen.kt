package com.example.moneymapping.ui.navigation

// Defines all the screens in the app as sealed class objects
// Each screen has a unique route string used for navigation
sealed class Screen(val route: String) {

    object Home : Screen("home")         // Main dashboard screen
    object History : Screen("history")   // Expense history screen
    object Groups : Screen("groups")     // Groups management screen
    object Profile : Screen("profile")   // User profile & settings screen
}