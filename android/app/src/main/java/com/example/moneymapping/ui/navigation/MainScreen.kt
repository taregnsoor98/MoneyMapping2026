package com.example.moneymapping.ui.navigation

// Imports needed for the main screen scaffold and navigation
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moneymapping.ui.expense.AddExpenseScreen // imports the add expense screen
import com.example.moneymapping.ui.screens.GroupsScreen
import com.example.moneymapping.ui.screens.HistoryScreen
import com.example.moneymapping.ui.screens.HomeScreen
import com.example.moneymapping.ui.screens.ProfileScreen

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
            composable(Screen.Groups.route) { GroupsScreen() }    // loads Groups screen
            composable(Screen.Profile.route) { ProfileScreen() }  // loads Profile screen
            composable(Screen.AddExpense.route) {
                AddExpenseScreen(
                    onExpenseAdded = { navController.popBackStack() }, // goes back after adding
                    onBack = { navController.popBackStack() }          // goes back when back button is pressed
                )
            }
        }
    }
}