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
    object CreateGroup : Screen("create_group") // route for the create group screen
    object GroupDetail : Screen("group_detail/{groupId}") // route for the group detail screen
    object AddGroupExpense : Screen("add_group_expense/{groupId}") // route for adding an expense from inside a group
    object ManageLimits : Screen("manage_limits/{groupId}") // route for the manage limits screen

}