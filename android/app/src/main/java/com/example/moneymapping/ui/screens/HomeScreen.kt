package com.example.moneymapping.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen() {

    // A full screen box that centers its content
    Box(
        contentAlignment = Alignment.Center, // centers the text in the middle of the screen
        modifier = Modifier.fillMaxSize()     // makes the box take up the full screen
    ) {
        Text(text = "Home Screen") // placeholder text until we build the real Home screen
    }
}