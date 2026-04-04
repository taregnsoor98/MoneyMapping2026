package com.example.moneymapping.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.moneymapping.ui.navigation.Screen

@Composable
fun HomeScreen(navController: NavController) { // receives navController to handle navigation

    // Scaffold gives us a FAB slot at the bottom right
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddExpense.route) } // opens Add Expense screen
            ) {
                Text("+") // plus icon on the FAB
            }
        }
    ) { paddingValues ->

        // A full screen box that centers its content
        Box(
            contentAlignment = Alignment.Center, // centers the text in the middle of the screen
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // avoids content going behind the FAB
        ) {
            Text(text = "Home Screen") // placeholder text until we build the real Home screen
        }
    }
}