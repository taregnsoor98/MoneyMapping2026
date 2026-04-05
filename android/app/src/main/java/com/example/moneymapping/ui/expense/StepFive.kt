package com.example.moneymapping.ui.expense

// Step 5 of the Add Expense wizard
// Shows each item in the expense and lets the user assign people to each one independently.
// Each item has its own search field to find existing users or add guests.
// If an item has quantity > 1, each person can use + and - buttons to pick how many units they take.
// The share is calculated proportionally based on quantity, or equally if quantity is 1.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StepFive(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    // Observes the current items and search state from the ViewModel
    val items by viewModel.items.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

    // Tracks which item is currently being searched
    var activeSearchIndex by remember { mutableStateOf(-1) }

    // Tracks the current search query
    var searchQuery by remember { mutableStateOf("") }

    // Tracks guest name input per item index
    var guestNamePerItem by remember { mutableStateOf(mapOf<Int, String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // allows scrolling if list is long
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between elements
    ) {

        // Step title
        Text(
            text = "Assign People to Items",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        Text(
            text = "For each item, search for a user or add a guest. If quantity > 1, use + and - to set how many units each person takes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter description color
        )

        // Shows each item as its own card with independent assignment
        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Shows the item name, quantity and total price
                    Text(
                        text = "${item.name} — qty: ${item.quantity} — ${String.format("%.2f", item.totalPrice)}",
                        style = MaterialTheme.typography.titleSmall // small title for item
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search for existing users for this specific item
                    ExposedDropdownMenuBox(
                        expanded = activeSearchIndex == index && searchState is SearchState.Results,
                        onExpandedChange = {} // controlled manually
                    ) {
                        OutlinedTextField(
                            value = if (activeSearchIndex == index) searchQuery else "", // shows query only for active item
                            onValueChange = { query ->
                                searchQuery = query       // updates search query
                                activeSearchIndex = index // marks this item as the active search
                                if (query.length >= 2) {
                                    viewModel.searchUsers(query) // only searches if query is at least 2 characters
                                } else {
                                    viewModel.resetSearch() // clears results if query is too short
                                }
                            },
                            label = { Text("Search user by username or email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(), // anchors the dropdown to this field
                            trailingIcon = {
                                if (activeSearchIndex == index && searchState is SearchState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.padding(8.dp)) // shows spinner while searching
                                }
                            }
                        )

                        // Shows search results as dropdown items for this item only
                        if (activeSearchIndex == index && searchState is SearchState.Results) {
                            ExposedDropdownMenu(
                                expanded = true,
                                onDismissRequest = {
                                    activeSearchIndex = -1  // clears active search
                                    viewModel.resetSearch() // clears search results
                                }
                            ) {
                                (searchState as SearchState.Results).users.forEach { user ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = user.username,
                                                style = MaterialTheme.typography.bodyMedium // shows username only, no email for privacy
                                            )
                                        },
                                        onClick = {
                                            // Adds user to this item's assignedTo list if not already there
                                            if (!item.assignedTo.contains(user.username)) {
                                                val updatedAssigned = item.assignedTo + user.username
                                                viewModel.updateItem(index, item.copy(assignedTo = updatedAssigned))
                                            }
                                            searchQuery = ""        // clears search field
                                            activeSearchIndex = -1  // clears active search
                                            viewModel.resetSearch() // clears search results
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Guest add row for this specific item
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = guestNamePerItem[index] ?: "", // shows guest name for this item only
                            onValueChange = {
                                guestNamePerItem = guestNamePerItem + (index to it) // updates guest name for this item
                            },
                            label = { Text("Or add guest name") },
                            modifier = Modifier.weight(1f) // takes most of the row width
                        )

                        // Button to add the guest to this item
                        Button(
                            onClick = {
                                val name = guestNamePerItem[index] ?: ""
                                if (name.isNotEmpty() && !item.assignedTo.contains(name)) {
                                    val updatedAssigned = item.assignedTo + name // adds guest to this item's list
                                    viewModel.updateItem(index, item.copy(assignedTo = updatedAssigned))
                                    guestNamePerItem = guestNamePerItem + (index to "") // clears guest name field
                                }
                            }
                        ) {
                            Text("+") // plus button to add guest
                        }
                    }

                    // Shows assigned people with quantity picker if item quantity > 1
                    if (item.assignedTo.isNotEmpty()) {

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider() // divides search section from assignment section
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Assigned to:",
                            style = MaterialTheme.typography.bodySmall // small label
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Shows each assigned person with + and - quantity picker
                        item.assignedTo.forEach { personName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                // Shows person name as a removable chip
                                FilterChip(
                                    selected = true, // always selected since they are assigned
                                    onClick = {
                                        // Removes this person from assignedTo and assignedQuantities
                                        val updatedAssigned = item.assignedTo - personName
                                        val updatedQuantities = item.assignedQuantities - personName
                                        viewModel.updateItem(
                                            index,
                                            item.copy(
                                                assignedTo = updatedAssigned,
                                                assignedQuantities = updatedQuantities
                                            )
                                        )
                                    },
                                    label = { Text("$personName ✕") } // shows name with remove icon
                                )

                                // Shows quantity picker only if item has more than 1 unit
                                if (item.quantity > 1) {
                                    Spacer(modifier = Modifier.width(8.dp))

                                    val personQty = item.assignedQuantities[personName] ?: 1 // current quantity for this person

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Minus button — decreases quantity down to 1
                                        Button(
                                            onClick = {
                                                val newQty = (personQty - 1).coerceAtLeast(1) // never goes below 1
                                                val updatedQuantities = item.assignedQuantities + (personName to newQty) // updates quantity
                                                viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                            },
                                            modifier = Modifier.width(36.dp) // small button width
                                        ) {
                                            Text("-") // minus label
                                        }

                                        // Shows current quantity
                                        Text(
                                            text = personQty.toString(),
                                            style = MaterialTheme.typography.bodyMedium // medium text for quantity
                                        )

                                        // Plus button — increases quantity up to item max
                                        Button(
                                            onClick = {
                                                val newQty = (personQty + 1).coerceAtMost(item.quantity) // never goes above item quantity
                                                val updatedQuantities = item.assignedQuantities + (personName to newQty) // updates quantity
                                                viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                            },
                                            modifier = Modifier.width(36.dp) // small button width
                                        ) {
                                            Text("+") // plus label
                                        }

                                        // Shows how much this person owes based on their quantity
                                        val totalAssignedQty = item.assignedQuantities.values.sum().takeIf { it > 0 } ?: item.assignedTo.size // total assigned quantity
                                        val personShare = (personQty.toDouble() / totalAssignedQty) * item.totalPrice // proportional share
                                        Text(
                                            text = String.format("%.2f", personShare), // shows their calculated share
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary // highlights share in primary color
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Shows equal split summary if quantity is 1
                        if (item.quantity == 1) {
                            Text(
                                text = "Each pays: ${String.format("%.2f", item.totalPrice / item.assignedTo.size)}", // equal split
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary // highlights the share amount
                            )
                        }
                    }
                }
            }
        }

        // Next button to move to Step 6
        Button(
            onClick = { viewModel.nextStep() }, // moves to next step
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}