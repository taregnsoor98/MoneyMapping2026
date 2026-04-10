package com.example.moneymapping.ui.expense

// Step 3 of the Add Expense wizard
// Asks the user whether this expense is solo (only affects their own budget)
// or shared with an existing group (FRIEND or FAMILY only).
// ONE_TIME groups are excluded — expenses for those are added from inside the group.
// If the user picks an existing group, it fetches their real groups from the backend.
// Shows a loading state while fetching and an error if the fetch fails.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepThree(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    // observes the current expense type and groups state from the ViewModel
    val expenseType by viewModel.expenseType.collectAsState()
    val groupsState by viewModel.groupsState.collectAsState()

    // tracks whether the group dropdown is expanded
    var groupExpanded by remember { mutableStateOf(false) }

    // tracks the currently selected group name for display
    var selectedGroupName by remember { mutableStateOf("") }

    // fetches groups when the user selects existing group option
    LaunchedEffect(expenseType) {
        if (expenseType is ExpenseType.ExistingGroup) {
            viewModel.fetchGroups() // fetches groups using the stored token from DataStore
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),                        // takes up the full screen
        verticalArrangement = Arrangement.Center,                 // centers content vertically
        horizontalAlignment = Alignment.CenterHorizontally        // centers content horizontally
    ) {

        // title asking the user how this expense is shared
        Text(
            text = "Is this expense solo or shared?",
            style = MaterialTheme.typography.titleLarge            // large title style
        )

        Spacer(modifier = Modifier.height(32.dp))                 // space between title and buttons

        // button for solo expense — only affects the user's own budget
        Button(
            onClick = {
                viewModel.setExpenseType(ExpenseType.Solo)        // sets type to solo
                viewModel.nextStep()                               // moves to step 4
            },
            modifier = Modifier.fillMaxWidth()                    // button takes full width
        ) {
            Text("Solo — just me")                                // label for solo option
        }

        Spacer(modifier = Modifier.height(16.dp))                 // space between buttons

        // button for sharing with an existing group
        OutlinedButton(
            onClick = {
                viewModel.setExpenseType(ExpenseType.ExistingGroup(0)) // sets type to existing group
            },
            modifier = Modifier.fillMaxWidth()                    // button takes full width
        ) {
            Text("Existing Group")                                // label for existing group option
        }

        // shows group picker only when existing group is selected
        if (expenseType is ExpenseType.ExistingGroup) {

            Spacer(modifier = Modifier.height(12.dp))

            when (groupsState) {

                // shows loading spinner while fetching groups
                is GroupsState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp)) // spinner while loading
                }

                // shows error message if fetch failed
                is GroupsState.Error -> {
                    Text(
                        text = (groupsState as GroupsState.Error).message,
                        color = MaterialTheme.colorScheme.error,  // red error text
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // shows group dropdown when groups are loaded
                is GroupsState.Success -> {
                    // filters out ONE_TIME groups — those only accept expenses from inside the group
                    val groups = (groupsState as GroupsState.Success).groups
                        .filter { it.type != "ONE_TIME" } // excludes ONE_TIME groups from the list

                    if (groups.isEmpty()) {
                        // shows message if user has no eligible groups yet
                        Text(
                            text = "You have no groups yet. Create one in the Groups tab!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // shows dropdown to pick a group
                        ExposedDropdownMenuBox(
                            expanded = groupExpanded,
                            onExpandedChange = { groupExpanded = !groupExpanded } // toggles dropdown
                        ) {
                            OutlinedTextField(
                                value = selectedGroupName.ifEmpty { "Select a group" }, // shows selected group name
                                onValueChange = {},
                                readOnly = true,                  // user can only select, not type
                                label = { Text("Group") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()                 // anchors the dropdown to this field
                            )
                            ExposedDropdownMenu(
                                expanded = groupExpanded,
                                onDismissRequest = { groupExpanded = false } // closes dropdown on dismiss
                            ) {
                                groups.forEach { group ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = group.name,                             // shows group name
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = group.type,                             // shows group type below name
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedGroupName = group.name                         // updates display name
                                            viewModel.setExpenseType(ExpenseType.ExistingGroup(group.id)) // updates ViewModel with real group id
                                            groupExpanded = false                                  // closes dropdown
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // next button — only enabled if a group is selected
                        Button(
                            onClick = { viewModel.nextStep() },   // moves to step 4
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedGroupName.isNotEmpty() // only enabled if a group is picked
                        ) {
                            Text("Next")                          // next button label
                        }
                    }
                }

                else -> {}                                        // idle state — shows nothing
            }
        }
    }
}