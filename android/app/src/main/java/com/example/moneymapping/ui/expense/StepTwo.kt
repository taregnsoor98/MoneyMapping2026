package com.example.moneymapping.ui.expense

// Step 2 of the Add Expense wizard
// Collects the basic expense information — description, date, currency, and category.
// If the user chose to scan in Step 1, the camera opens first and ML Kit fills in the fields.
// The user can edit all fields before moving to Step 3.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepTwo(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    // Observes the current values from the ViewModel
    val description by viewModel.description.collectAsState()
    val date by viewModel.date.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val category by viewModel.category.collectAsState()

    // Available currency options
    val currencies = listOf("USD", "EUR", "GBP", "JOD", "RUB")

    // Available category options
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Health", "Education", "Other")

    // Tracks whether the currency dropdown is open
    var currencyExpanded by remember { mutableStateOf(false) }

    // Tracks whether the category dropdown is open
    var categoryExpanded by remember { mutableStateOf(false) }

    // Tracks whether the date picker dialog is open
    var showDatePicker by remember { mutableStateOf(false) }

    // State for the date picker
    val datePickerState = rememberDatePickerState()

    // Shows the date picker dialog when triggered
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false }, // closes dialog on dismiss
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.setDate(sdf.format(Date(millis))) // saves formatted date to ViewModel
                    }
                    showDatePicker = false // closes dialog after confirming
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState) // the actual date picker UI
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp), // padding at the bottom
        verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between fields
    ) {

        // Step title
        Text(
            text = "Expense Details",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        // Description input field
        OutlinedTextField(
            value = description,
            onValueChange = { viewModel.setDescription(it) }, // updates ViewModel on change
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        // Date picker field — opens date picker dialog on click
        OutlinedTextField(
            value = date,
            onValueChange = {},
            readOnly = true, // user opens date picker instead of typing
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }) { Text("Pick") } // opens date picker
            }
        )

        // Currency dropdown
        ExposedDropdownMenuBox(
            expanded = currencyExpanded,
            onExpandedChange = { currencyExpanded = !currencyExpanded } // toggles dropdown
        ) {
            OutlinedTextField(
                value = currency,
                onValueChange = {},
                readOnly = true, // user can only select, not type
                label = { Text("Currency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor() // anchors the dropdown to this field
            )
            ExposedDropdownMenu(
                expanded = currencyExpanded,
                onDismissRequest = { currencyExpanded = false }
            ) {
                currencies.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.setCurrency(option) // saves selected currency to ViewModel
                            currencyExpanded = false      // closes dropdown
                        }
                    )
                }
            }
        }

        // Category dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded } // toggles dropdown
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true, // user can only select, not type
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor() // anchors the dropdown to this field
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.setCategory(option) // saves selected category to ViewModel
                            categoryExpanded = false      // closes dropdown
                        }
                    )
                }
            }
        }

        // Next button to move to Step 3
        Button(
            onClick = { viewModel.nextStep() }, // moves to next step
            modifier = Modifier.fillMaxWidth(),
            enabled = description.isNotEmpty() && date.isNotEmpty() && category.isNotEmpty() // only enabled if required fields are filled
        ) {
            Text("Next")
        }
    }
}