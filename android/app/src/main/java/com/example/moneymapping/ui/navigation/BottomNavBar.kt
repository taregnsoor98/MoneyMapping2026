package com.example.moneymapping.ui.navigation

// Imports for the bottom navigation bar UI components
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState // lets us observe the current screen
import androidx.compose.runtime.getValue
import com.example.moneymapping.R // gives access to our drawable icons

// Data class to hold each nav item's label, route, and icon
data class NavItem(val label: String, val route: String, val icon: Int)

// List of all bottom nav items
val bottomNavItems = listOf(
    NavItem("Home", Screen.Home.route, R.drawable.ic_home),           // Home tab
    NavItem("History", Screen.History.route, R.drawable.ic_history),  // History tab
    NavItem("Groups", Screen.Groups.route, R.drawable.ic_groups),     // Groups tab
    NavItem("Profile", Screen.Profile.route, R.drawable.ic_profile)   // Profile tab
)

@Composable
fun BottomNavBar(navController: NavController) { // takes navController to handle tab switching

    // Observes the current back stack to know which screen we are on
    val currentBackStack by navController.currentBackStackEntryAsState()

    // Extracts the current route string from the back stack
    val currentRoute = currentBackStack?.destination?.route

    // The actual bottom navigation bar container
    NavigationBar {

        // Loops through each nav item and creates a tab for it
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route, // highlights this tab if it matches current screen
                onClick = {
                    navController.navigate(item.route) { // navigates to the tapped screen
                        launchSingleTop = true // prevents creating duplicate screens
                        restoreState = true // restores previous state when switching back
                        popUpTo(Screen.Home.route) { saveState = true } // clears back stack up to Home
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon), // loads the icon from drawable
                        contentDescription = item.label // accessibility description for the icon
                    )
                },
                label = { Text(item.label) } // shows the tab label below the icon
            )
        }
    }
}