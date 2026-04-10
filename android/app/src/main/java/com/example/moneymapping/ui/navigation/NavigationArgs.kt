package com.example.moneymapping.ui.navigation

// A temporary storage object for passing complex data between screens during navigation
// This is needed because Android navigation routes only support simple types like String and Long
object NavigationArgs {

    // temporarily holds the group member names when navigating from a group to the add expense screen
    var pendingGroupMembers: List<String> = emptyList() // cleared after being read by AddExpenseScreen
}