package com.example.moneymapping.ui.expense

// Step 5 of the Add Expense wizard
// Shows each item in the expense and lets the user assign people to each one independently.
// Each item has its own search field to find existing users or add guests.
// If an item has quantity > 1, each person can use + and - buttons to pick how many units they take.
// The share is calculated proportionally based on quantity, or equally if quantity is 1.
// People limit per item = item quantity. Total assigned quantities cannot exceed item quantity.

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StepFive(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    val items by viewModel.items.collectAsState()             // observes the current items list
    val searchState by viewModel.searchState.collectAsState() // observes the current search state

    // Tracks which item's search field is currently active
    var activeSearchIndex by remember { mutableStateOf(-1) }

    // Tracks the search query per item index — fixes the reset bug
    var searchQueryPerItem by remember { mutableStateOf(mapOf<Int, String>()) }

    // Tracks guest name input per item index
    var guestNamePerItem by remember { mutableStateOf(mapOf<Int, String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // allows scrolling if list is long
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between elements
    ) {

        Text(
            text = "Assign People to Items",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        Text(
            text = "Search for a user or add a guest for each item. If quantity > 1, set how many units each person takes. Total units assigned cannot exceed the item quantity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter description color
        )

        items.forEachIndexed { index, item ->

            // Calculates how many units are already assigned across all people for this item
            val totalAssigned = item.assignedQuantities.values.sum()

            // The number of people that can still be added — limited by item quantity
            val canAddMore = item.assignedTo.size < item.quantity

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Shows item name, quantity and total price
                    Text(
                        text = "${item.name} — qty: ${item.quantity} — ${String.format("%.2f", item.totalPrice)}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search field — only shown if more people can be added
                    if (canAddMore) {
                        ExposedDropdownMenuBox(
                            expanded = activeSearchIndex == index && searchState is SearchState.Results,
                            onExpandedChange = {} // controlled manually
                        ) {
                            OutlinedTextField(
                                value = searchQueryPerItem[index] ?: "", // uses per-item query — fixes reset bug
                                onValueChange = { query ->
                                    searchQueryPerItem = searchQueryPerItem + (index to query) // stores query per item
                                    activeSearchIndex = index                                   // marks this item as active
                                    if (query.length >= 2) {
                                        viewModel.searchUsers(query) // searches if query is long enough
                                    } else {
                                        viewModel.resetSearch() // clears results if too short
                                    }
                                },
                                label = { Text("Search user by username or email") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(), // anchors the dropdown to this field
                                trailingIcon = {
                                    if (activeSearchIndex == index && searchState is SearchState.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.padding(8.dp)) // spinner while searching
                                    }
                                }
                            )

                            // Shows search results as dropdown for this item only
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
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                if (!item.assignedTo.contains(user.username)) {
                                                    val updatedAssigned = item.assignedTo + user.username // adds user to assigned list
                                                    viewModel.updateItem(index, item.copy(assignedTo = updatedAssigned))
                                                }
                                                searchQueryPerItem = searchQueryPerItem + (index to "") // clears this item's search field
                                                activeSearchIndex = -1                                  // clears active search
                                                viewModel.resetSearch()                                 // clears search results
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Guest add row — only shown if more people can be added
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = guestNamePerItem[index] ?: "", // guest name for this item only
                                onValueChange = {
                                    guestNamePerItem = guestNamePerItem + (index to it) // updates guest name for this item
                                },
                                label = { Text("Or add guest name") },
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    val name = guestNamePerItem[index] ?: ""
                                    if (name.isNotEmpty() && !item.assignedTo.contains(name)) {
                                        val updatedAssigned = item.assignedTo + name // adds guest to assigned list
                                        viewModel.updateItem(index, item.copy(assignedTo = updatedAssigned))
                                        guestNamePerItem = guestNamePerItem + (index to "") // clears guest name field
                                    }
                                }
                            ) {
                                Text("+") // plus button to add guest
                            }
                        }
                    } else {
                        // Shows a message when the people limit has been reached
                        Text(
                            text = "Max people reached (${item.quantity})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter text
                        )
                    }

                    // Shows assigned people section if anyone is assigned
                    if (item.assignedTo.isNotEmpty()) {

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Assigned to:",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        item.assignedTo.forEach { personName ->

                            val personQty = item.assignedQuantities[personName] ?: 1 // current quantity for this person

                            // Calculates how many units this person can still take
                            // Total of everyone else + this person's current qty cannot exceed item quantity
                            val othersTotal = totalAssigned - (item.assignedQuantities[personName] ?: 0) // total assigned to everyone else
                            val maxForThisPerson = item.quantity - othersTotal // max this person can take

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                // Person name chip — tap to remove
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        val updatedAssigned = item.assignedTo - personName          // removes from assigned list
                                        val updatedQuantities = item.assignedQuantities - personName // removes their quantity
                                        viewModel.updateItem(
                                            index,
                                            item.copy(
                                                assignedTo = updatedAssigned,
                                                assignedQuantities = updatedQuantities
                                            )
                                        )
                                    },
                                    label = { Text("$personName ✕") }
                                )

                                // Quantity picker — only shown if item has more than 1 unit
                                if (item.quantity > 1) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {

                                        // Minus button — decreases quantity down to 1
                                        OutlinedButton(
                                            onClick = {
                                                val newQty = (personQty - 1).coerceAtLeast(1) // never goes below 1
                                                val updatedQuantities = item.assignedQuantities + (personName to newQty)
                                                viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                            }
                                        ) {
                                            Text("-") // minus label
                                        }

                                        // Editable quantity field — user can type a number directly
                                        OutlinedTextField(
                                            value = personQty.toString(),
                                            onValueChange = { input ->
                                                val typed = input.toIntOrNull() ?: 1                     // converts to int safely
                                                val clamped = typed.coerceIn(1, maxForThisPerson)        // clamps between 1 and max for this person
                                                val updatedQuantities = item.assignedQuantities + (personName to clamped)
                                                viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // shows number keyboard
                                            modifier = Modifier.width(64.dp), // fixed width for the number field
                                            singleLine = true                  // keeps it on one line
                                        )

                                        // Plus button — increases quantity but never exceeds max for this person
                                        OutlinedButton(
                                            onClick = {
                                                val newQty = (personQty + 1).coerceAtMost(maxForThisPerson) // never exceeds max
                                                val updatedQuantities = item.assignedQuantities + (personName to newQty)
                                                viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                            }
                                        ) {
                                            Text("+") // plus label
                                        }

                                        // Shows this person's calculated share
                                        val totalAssignedQty = item.assignedQuantities.values.sum().takeIf { it > 0 } ?: item.assignedTo.size
                                        val personShare = (personQty.toDouble() / totalAssignedQty) * item.totalPrice
                                        Text(
                                            text = String.format("%.2f", personShare),
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
                                text = "Each pays: ${String.format("%.2f", item.totalPrice / item.assignedTo.size)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Next button to move to Step 6
        Button(
            onClick = { viewModel.nextStep() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}