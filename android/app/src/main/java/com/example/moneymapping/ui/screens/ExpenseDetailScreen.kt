package com.example.moneymapping.ui.screens

// Expense Detail Screen
// Shows the full details of a single expense — basic info, items, assignments, and who owes what.
// Has an Edit text link in the top right corner to navigate to the edit screen.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,       // the ID of the expense to show
    onEditClick: () -> Unit, // called when the user taps Edit
    onBack: () -> Unit       // called when the user taps back
) {
    val viewModel: ExpenseDetailViewModel = viewModel() // creates the ExpenseDetailViewModel
    val state by viewModel.state.collectAsState()       // observes the current state

    // Loads the expense when the screen first opens
    LaunchedEffect(expenseId) {
        viewModel.loadExpense(expenseId) // fetches the expense by ID
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Details") }, // top bar title
                navigationIcon = {
                    IconButton(onClick = onBack) { // back button
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Edit text link in the top right corner
                    TextButton(onClick = onEditClick) {
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.Underline // underlined Edit text
                            ),
                            color = MaterialTheme.colorScheme.primary // primary color
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when (val currentState = state) {

            // Shows a spinner while loading
            is ExpenseDetailState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator() // spinning loader
                }
            }

            // Shows an error message if fetch failed
            is ExpenseDetailState.Error -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error // red error text
                    )
                }
            }

            // Shows the expense details when loaded
            is ExpenseDetailState.Loaded -> {
                val expense = currentState.expense // the loaded expense

                // Determines the expense type label
                val expenseType = when {
                    expense.groupId != null && !expense.isOneTimeSplit -> "Group" // belongs to an existing group
                    expense.isOneTimeSplit -> "One-time Split"                     // temporary split with others
                    else -> "Solo"                                                 // personal expense
                }

                // Calculates how much each person owes in total across all items
                val totalShares = mutableMapOf<String, Double>()
                expense.items.forEach { item ->
                    item.assignments.forEach { assignment ->
                        totalShares[assignment.personName] = (totalShares[assignment.personName] ?: 0.0) + assignment.shareAmount // adds their share for this item
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()), // allows scrolling if content is long
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Basic info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Text(
                                text = "Expense Info",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold // bold section title
                            )

                            Divider()

                            // Description row
                            InfoRow(label = "Description", value = expense.description)

                            // Date row
                            InfoRow(label = "Date", value = expense.date)

                            // Currency row
                            InfoRow(label = "Currency", value = expense.currency)

                            // Category row
                            InfoRow(label = "Category", value = expense.category)

                            // Type row
                            InfoRow(label = "Type", value = expenseType)

                            Divider()

                            // Total amount row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${expense.currency} ${String.format("%.2f", expense.amount)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary // highlights total
                                )
                            }
                        }
                    }

                    // Items breakdown card — shows each item, its cost, and who is assigned
                    if (expense.items.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Text(
                                    text = "Items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold // bold section title
                                )

                                Divider()

                                // Shows each item with its assignments
                                expense.items.forEach { item ->

                                    // Item name and total price row
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
                                            text = "${expense.currency} ${String.format("%.2f", item.totalPrice)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Shows unit price and quantity below the item name
                                    Text(
                                        text = "${expense.currency} ${String.format("%.2f", item.unitPrice)} × ${item.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant // lighter text
                                    )

                                    // Shows who is assigned to this item and how much they owe
                                    if (item.assignments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        item.assignments.forEach { assignment ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 12.dp), // indents to show it belongs to the item
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = assignment.personName, // the person's name
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${expense.currency} ${String.format("%.2f", assignment.shareAmount)}", // how much they owe
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary // highlights amount
                                                )
                                            }
                                        }
                                    } else {
                                        // Shows a note if no one is assigned to this item
                                        Text(
                                            text = "No one assigned",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }

                                    Divider() // divides items from each other
                                }
                            }
                        }
                    }

                    // Total per person card — shows how much each person owes in total
                    if (totalShares.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Text(
                                    text = "Each Person Owes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold // bold section title
                                )

                                Divider()

                                // Shows each person's total share across all items
                                totalShares.forEach { (personName, totalAmount) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = personName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold // bold person name
                                        )
                                        Text(
                                            text = "${expense.currency} ${String.format("%.2f", totalAmount)}", // total they owe
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary // highlights amount
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Receipt images section — only shown if there are images
                    if (expense.receiptImages.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Receipt Images",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${expense.receiptImages.size} image(s) attached",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Shows a single label-value row — used for expense info fields
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter label color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium // slightly bold value
        )
    }
}