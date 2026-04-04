package com.example.moneymapping.ui.expense

// Step 1 of the Add Expense wizard
// Asks the user how they want to add the expense — either by scanning a receipt
// using the camera and ML Kit, or by entering the details manually.
// Based on the choice, it sets the scanning flag in the ViewModel and moves to Step 2.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StepOne(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    Column(
        modifier = Modifier.fillMaxSize(), // takes up the full screen
        verticalArrangement = Arrangement.Center, // centers content vertically
        horizontalAlignment = Alignment.CenterHorizontally // centers content horizontally
    ) {

        // Title asking the user how they want to add the expense
        Text(
            text = "How would you like to add this expense?",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        Spacer(modifier = Modifier.height(32.dp)) // space between title and buttons

        // Button to scan a receipt using the camera
        Button(
            onClick = {
                viewModel.setScanning(true) // marks that user chose to scan
                viewModel.nextStep()        // moves to step 2
            },
            modifier = Modifier.fillMaxWidth() // button takes full width
        ) {
            Text("Scan a Receipt") // label for scan option
        }

        Spacer(modifier = Modifier.height(16.dp)) // space between buttons

        // Button to enter the expense manually
        OutlinedButton(
            onClick = {
                viewModel.setScanning(false) // marks that user chose manual entry
                viewModel.nextStep()         // moves to step 2
            },
            modifier = Modifier.fillMaxWidth() // button takes full width
        ) {
            Text("Enter Manually") // label for manual entry option
        }
    }
}