package com.example.moneymapping.ui.expense

// Step 6 of the Add Expense wizard — the final step
// Shows a full summary of the expense including all items, their assignments,
// and how much each person owes in total broken down by item.
// The user can review everything and hit Confirm to save the expense.
// Later, the user will be able to set up installment plans for each person's share.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StepSix(
    viewModel: ExpenseViewModel, // receives the shared ViewModel
    onExpenseAdded: () -> Unit   // callback to navigate back after successful submission
) {

    // Observes all the data from the ViewModel for the summary
    val description by viewModel.description.collectAsState()
    val date by viewModel.date.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val category by viewModel.category.collectAsState()
    val items by viewModel.items.collectAsState()
    val expenseState by viewModel.expenseState.collectAsState()

    // Calculates how much each person owes based on item assignments
    val shares = viewModel.calculateShares()

    // Calculates the total expense amount by summing all item totals
    val totalAmount = items.sumOf { it.totalPrice }

    // Navigates back automatically when submission is successful
    LaunchedEffect(expenseState) {
        if (expenseState is ExpenseState.Success) {
            viewModel.resetAll()  // resets the ViewModel back to initial state
            onExpenseAdded()      // navigates back to home screen
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // allows scrolling if content is long
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between elements
    ) {

        // Step title
        Text(
            text = "Review & Confirm",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        // Basic expense info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                Text(
                    text = "Expense Info",
                    style = MaterialTheme.typography.titleMedium // medium title style
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Shows description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Description", style = MaterialTheme.typography.bodyMedium)
                    Text(text = description, style = MaterialTheme.typography.bodyMedium)
                }

                // Shows date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Date", style = MaterialTheme.typography.bodyMedium)
                    Text(text = date, style = MaterialTheme.typography.bodyMedium)
                }

                // Shows currency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Currency", style = MaterialTheme.typography.bodyMedium)
                    Text(text = currency, style = MaterialTheme.typography.bodyMedium)
                }

                // Shows category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Category", style = MaterialTheme.typography.bodyMedium)
                    Text(text = category, style = MaterialTheme.typography.bodyMedium)
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp)) // divider line

                // Shows the total amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold // bold for emphasis
                    )
                    Text(
                        text = "$currency ${String.format("%.2f", totalAmount)}", // formats total to 2 decimal places
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary // highlights total in primary color
                    )
                }
            }
        }

        // Items breakdown card — shows each item with who is assigned and their share
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                Text(
                    text = "Items Breakdown",
                    style = MaterialTheme.typography.titleMedium // medium title style
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Shows each item with assigned people and per person share
                items.forEach { item ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold // bold item name
                        )
                        Text(
                            text = "$currency ${String.format("%.2f", item.totalPrice)}", // formats price
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Shows who is assigned to this item and their share
                    if (item.assignedTo.isNotEmpty()) {
                        val totalAssignedQty = item.assignedQuantities.values.sum().takeIf { it > 0 } ?: item.assignedTo.size // total assigned quantity
                        item.assignedTo.forEach { person ->
                            val personQty = item.assignedQuantities[person] ?: 1 // quantity this person is taking
                            val personShare = (personQty.toDouble() / totalAssignedQty) * item.totalPrice // proportional share
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp), // indents to show it belongs to the item above
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (item.quantity > 1) "$person (x$personQty)" else person, // shows quantity if item has more than 1
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // lighter color for person name
                                )
                                Text(
                                    text = "$currency ${String.format("%.2f", personShare)}", // shows their proportional share
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Shows a warning if no one is assigned to this item
                        Text(
                            text = "No one assigned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error, // red warning color
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp)) // divides items
                }
            }
        }

        // Total per person summary card — shows how much each person owes in total
        if (shares.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    Text(
                        text = "Each Person Owes",
                        style = MaterialTheme.typography.titleMedium // medium title style
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shows each person's total share across all items
                    shares.forEach { (person, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = person,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold // bold person name
                            )
                            Text(
                                text = "$currency ${String.format("%.2f", amount)}", // formats amount to 2 decimal places
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary // highlights amount in primary color
                            )
                        }
                    }
                }
            }
        }

        // Shows error message if submission fails
        if (expenseState is ExpenseState.Error) {
            Text(
                text = (expenseState as ExpenseState.Error).message,
                color = MaterialTheme.colorScheme.error // red error text
            )
        }

        // Confirm button to submit the expense
        Button(
            onClick = { viewModel.submitExpense() }, // submits the expense to the backend
            modifier = Modifier.fillMaxWidth(),
            enabled = expenseState !is ExpenseState.Loading // disables button while loading
        ) {
            if (expenseState is ExpenseState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp)) // shows spinner while loading
            } else {
                Text("Confirm & Save") // normal button text
            }
        }
    }
}