package com.example.moneymapping.ui.navigation

// Imports needed for the main screen scaffold and navigation
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moneymapping.ui.expense.AddExpenseScreen        // imports the add expense screen
import com.example.moneymapping.ui.screens.EditExpenseScreen       // imports the edit expense screen
import com.example.moneymapping.ui.screens.ExpenseDetailScreen     // imports the expense detail screen
import com.example.moneymapping.ui.screens.GroupsScreen
import com.example.moneymapping.ui.screens.HistoryScreen
import com.example.moneymapping.ui.screens.HomeScreen
import com.example.moneymapping.ui.screens.ProfileScreen
import com.example.moneymapping.ui.screens.CreateGroupScreen       // imports the create group screen
import com.example.moneymapping.ui.screens.GroupDetailScreen       // imports the group detail screen
import com.example.moneymapping.ui.screens.ManageLimitsScreen      // imports the manage limits screen

@Composable
fun MainScreen() {

    // Creates and remembers the nav controller for the whole app
    val navController = rememberNavController()

    // Scaffold provides the basic layout structure with a bottom bar
    Scaffold(
        bottomBar = {
            // Attaches our bottom nav bar and passes the nav controller to it
            BottomNavBar(navController = navController)
        }
    ) { innerPadding ->

        // NavHost manages which screen is currently shown
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route, // app starts on the Home screen
            modifier = Modifier.padding(innerPadding) // avoids content going behind the nav bar
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController) // passes navController so Home can navigate
            }
            composable(Screen.History.route) { HistoryScreen() }  // loads History screen
            composable(Screen.Groups.route) {
                GroupsScreen(
                    onNavigateToCreateGroup = { navController.navigate(Screen.CreateGroup.route) }, // navigates to create group screen
                    onNavigateToGroupDetail = { groupId ->
                        navController.navigate(Screen.GroupDetail.route.replace("{groupId}", groupId.toString())) // navigates to group detail screen
                    }
                )
            }    // loads Groups screen
            composable(Screen.Profile.route) { ProfileScreen() }  // loads Profile screen
            composable(Screen.AddExpense.route) {
                AddExpenseScreen(
                    onExpenseAdded = { navController.popBackStack() }, // goes back after adding
                    onBack = { navController.popBackStack() }          // goes back when back button is pressed
                )
            }
            composable(Screen.DetailExpense.route) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable // gets the expense ID from the route
                ExpenseDetailScreen(
                    expenseId = expenseId,                             // passes the expense ID to the detail screen
                    onEditClick = {
                        navController.navigate(
                            Screen.EditExpense.route.replace("{expenseId}", expenseId) // navigates to edit screen
                        )
                    },
                    onBack = { navController.popBackStack() }          // goes back when back is pressed
                )
            }
            composable(Screen.CreateGroup.route) {
                CreateGroupScreen(
                    onGroupCreated = { navController.popBackStack() }, // goes back after creating group
                    onBack = { navController.popBackStack() }          // goes back when back is pressed
                )
            }
            composable(Screen.GroupDetail.route) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable // gets the group ID from the route
                GroupDetailScreen(
                    groupId = groupId.toLong(),                        // passes the group ID to the detail screen
                    onBack = { navController.popBackStack() },         // goes back when back is pressed
                    onAddExpense = { gId, memberNames ->
                        NavigationArgs.pendingGroupMembers = memberNames // stores member names temporarily
                        navController.navigate(Screen.AddGroupExpense.route.replace("{groupId}", gId.toString())) // navigates to add group expense screen
                    },
                    onExpenseClick = { expenseId ->
                        navController.navigate(Screen.DetailExpense.route.replace("{expenseId}", expenseId)) // navigates to expense detail screen
                    },
                    onManageLimits = { gId ->
                        navController.navigate(Screen.ManageLimits.route.replace("{groupId}", gId.toString())) // navigates to manage limits screen
                    }
                )
            }
            composable(Screen.ManageLimits.route) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable // gets the group ID from the route
                ManageLimitsScreen(
                    groupId = groupId.toLong(),                        // passes the group ID to the manage limits screen
                    onBack = { navController.popBackStack() }          // goes back when back is pressed
                )
            }
            composable(Screen.AddGroupExpense.route) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable // gets the group ID from the route
                val memberNames = NavigationArgs.pendingGroupMembers  // reads the stored member names
                NavigationArgs.pendingGroupMembers = emptyList()      // clears the stored member names after reading
                AddExpenseScreen(
                    onExpenseAdded = { navController.popBackStack() }, // goes back after adding
                    onBack = { navController.popBackStack() },         // goes back when back is pressed
                    groupId = groupId.toLong(),                        // passes the group ID to the expense wizard
                    memberNames = memberNames                          // passes the member names to the expense wizard
                )
            }
            composable(Screen.EditExpense.route) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable // gets the expense ID from the route
                EditExpenseScreen(
                    expenseId = expenseId,                             // passes the expense ID to the edit screen
                    onExpenseSaved = { navController.popBackStack() }, // goes back after saving
                    onBack = { navController.popBackStack() }          // goes back when back button is pressed
                )
            }
        }
    }
}