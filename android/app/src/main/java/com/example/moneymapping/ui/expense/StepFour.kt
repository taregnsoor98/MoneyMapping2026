package com.example.moneymapping.ui.expense

// Step 4 of the Add Expense wizard
// Shows the list of items in the expense. Each item has a name, unit price, quantity, and total price.
// The user can add new items one by one using the "+" button, edit existing ones, or delete them.
// If the user chose to scan in Step 1, items will be pre-filled by ML Kit (coming later).
// Once done, the user moves to Step 5 to assign people to each item.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StepFour(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    // Observes the current items list from the ViewModel
    val items by viewModel.items.collectAsState()

    // Tracks whether the add item form is visible
    var showAddForm by remember { mutableStateOf(false) }

    // Tracks the fields in the add item form
    var newItemName by remember { mutableStateOf("") }
    var newItemUnitPrice by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // allows scrolling if list is long
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between elements
    ) {

        // Step title
        Text(
            text = "Expense Items",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        // Shows each item in the list as a card
        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(), // card takes full width
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Item name field — editable
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { viewModel.updateItem(index, item.copy(name = it)) }, // updates name in ViewModel
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // spacing between price and quantity fields
                    ) {

                        // Unit price field — editable
                        OutlinedTextField(
                            value = item.unitPrice.toString(),
                            onValueChange = {
                                val price = it.toDoubleOrNull() ?: 0.0 // converts to double safely
                                val total = price * item.quantity       // recalculates total
                                viewModel.updateItem(index, item.copy(unitPrice = price, totalPrice = total)) // updates ViewModel
                            },
                            label = { Text("Unit Price") },
                            modifier = Modifier.weight(1f) // takes half the row width
                        )

                        // Quantity field — editable
                        OutlinedTextField(
                            value = item.quantity.toString(),
                            onValueChange = {
                                val qty = it.toIntOrNull() ?: 1          // converts to int safely
                                val total = item.unitPrice * qty          // recalculates total
                                viewModel.updateItem(index, item.copy(quantity = qty, totalPrice = total)) // updates ViewModel
                            },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f) // takes half the row width
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, // pushes total and delete to opposite ends
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shows the calculated total price for this item
                        Text(
                            text = "Total: ${item.totalPrice}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Button to delete this item from the list
                        IconButton(onClick = { viewModel.removeItem(index) }) {
                            Text("🗑️") // trash icon for delete
                        }
                    }
                }
            }
        }

        // Shows the add item form when the "+" button is tapped
        if (showAddForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // New item name field
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // New item unit price field
                        OutlinedTextField(
                            value = newItemUnitPrice,
                            onValueChange = { newItemUnitPrice = it },
                            label = { Text("Unit Price") },
                            modifier = Modifier.weight(1f)
                        )

                        // New item quantity field
                        OutlinedTextField(
                            value = newItemQuantity,
                            onValueChange = { newItemQuantity = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // Button to confirm adding the new item
                        Button(
                            onClick = {
                                val price = newItemUnitPrice.toDoubleOrNull() ?: 0.0 // converts price safely
                                val qty = newItemQuantity.toIntOrNull() ?: 1          // converts quantity safely
                                viewModel.addItem(
                                    ExpenseItem(
                                        name = newItemName,
                                        unitPrice = price,
                                        quantity = qty,
                                        totalPrice = price * qty // calculates total automatically
                                    )
                                )
                                // Resets the form fields after adding
                                newItemName = ""
                                newItemUnitPrice = ""
                                newItemQuantity = "1"
                                showAddForm = false // hides the form after adding
                            },
                            modifier = Modifier.weight(1f),
                            enabled = newItemName.isNotEmpty() && newItemUnitPrice.isNotEmpty() // only enabled if fields are filled
                        ) {
                            Text("Add") // confirm add button label
                        }

                        // Button to cancel adding a new item
                        OutlinedButton(
                            onClick = { showAddForm = false }, // hides the form without adding
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel") // cancel button label
                        }
                    }
                }
            }
        }

        // Button to show the add item form
        OutlinedButton(
            onClick = { showAddForm = true }, // shows the add item form
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Item") // label for add item button
        }

        // Next button to move to Step 5
        Button(
            onClick = { viewModel.nextStep() }, // moves to next step
            modifier = Modifier.fillMaxWidth(),
            enabled = items.isNotEmpty() // only enabled if at least one item is added
        ) {
            Text("Next")
        }
    }
}