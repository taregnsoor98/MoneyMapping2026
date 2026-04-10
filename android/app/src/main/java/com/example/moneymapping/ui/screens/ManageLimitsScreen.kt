package com.example.moneymapping.ui.screens

import android.app.Application                                     // needed to access context
import androidx.compose.foundation.layout.Arrangement              // controls spacing between items
import androidx.compose.foundation.layout.Column                   // arranges children vertically
import androidx.compose.foundation.layout.Row                      // arranges children horizontally
import androidx.compose.foundation.layout.Spacer                   // adds empty space
import androidx.compose.foundation.layout.fillMaxSize              // makes a composable fill all available space
import androidx.compose.foundation.layout.fillMaxWidth             // makes a composable fill the full width
import androidx.compose.foundation.layout.height                   // sets the height of a composable
import androidx.compose.foundation.layout.padding                  // adds padding around a composable
import androidx.compose.foundation.layout.width                    // sets the width of a composable
import androidx.compose.foundation.lazy.LazyColumn                 // a scrollable list
import androidx.compose.foundation.lazy.items                      // renders a list of items in LazyColumn
import androidx.compose.material.icons.Icons                       // provides built-in icons
import androidx.compose.material.icons.filled.ArrowBack            // back arrow icon
import androidx.compose.material3.Button                           // a filled button
import androidx.compose.material3.Card                             // a card container
import androidx.compose.material3.CircularProgressIndicator        // a loading spinner
import androidx.compose.material3.DropdownMenuItem                 // an item inside a dropdown menu
import androidx.compose.material3.ExperimentalMaterial3Api         // needed for some Material3 components
import androidx.compose.material3.ExposedDropdownMenuBox           // a dropdown menu box
import androidx.compose.material3.ExposedDropdownMenuDefaults      // default styles for dropdown
import androidx.compose.material3.Icon                             // displays an icon
import androidx.compose.material3.IconButton                       // a button that only contains an icon
import androidx.compose.material3.MaterialTheme                    // provides theme colors and typography
import androidx.compose.material3.OutlinedTextField                // a text input with an outline
import androidx.compose.material3.Scaffold                         // provides basic screen structure
import androidx.compose.material3.Text                             // displays text
import androidx.compose.material3.TopAppBar                        // the top bar with title and back button
import androidx.compose.runtime.Composable                         // marks a function as a composable
import androidx.compose.runtime.LaunchedEffect                     // runs a side effect when the composable is first shown
import androidx.compose.runtime.collectAsState                     // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                           // allows using 'by' delegation for state
import androidx.compose.runtime.mutableStateOf                     // creates mutable state
import androidx.compose.runtime.remember                           // remembers state across recompositions
import androidx.compose.runtime.setValue                           // allows setting state with 'by' delegation
import androidx.compose.ui.Alignment                               // controls alignment of composables
import androidx.compose.ui.Modifier                                // used to style and layout composables
import androidx.compose.ui.platform.LocalContext                   // gets the current context
import androidx.compose.ui.unit.dp                                 // density-independent pixels
import androidx.lifecycle.ViewModelProvider                        // creates ViewModels with parameters
import androidx.lifecycle.viewmodel.compose.viewModel              // creates a ViewModel in a composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLimitsScreen(
    groupId: Long,       // the ID of the group to manage limits for
    onBack: () -> Unit   // called when back button is pressed
) {
    val context = LocalContext.current // gets the current context
    val viewModel: GroupDetailViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    ) // reuses the GroupDetailViewModel since it already has the group and limits data

    val state by viewModel.state.collectAsState()              // observes the group state
    val limits by viewModel.limits.collectAsState()            // observes the limits list
    val currentUserId by viewModel.currentUserId.collectAsState() // observes the current user's ID

    // loads the group and limits when the screen first appears
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId) // fetches the group data and limits
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Limits") }, // screen title
                navigationIcon = {
                    IconButton(onClick = onBack) { // goes back when back button is pressed
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back") // back arrow
                    }
                }
            )
        }
    ) { paddingValues ->
        when (state) {

            // shows a loading spinner while fetching the group
            is GroupDetailState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator() // centered spinner
                }
            }

            // shows an error message if something went wrong
            is GroupDetailState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = (state as GroupDetailState.Error).message, // the error message
                        color = MaterialTheme.colorScheme.error            // red error color
                    )
                }
            }

            // shows the manage limits content when loaded
            is GroupDetailState.Success -> {
                val group = (state as GroupDetailState.Success).group // gets the loaded group

                // checks if the current user is an admin
                val isAdmin = group.members.any {
                    it.userId == currentUserId && it.role == "ADMIN" // checks admin role
                }

                // for FAMILY groups — only admins can edit limits
                val canEdit = when (group.type) {
                    "FAMILY" -> isAdmin  // only admins in family groups
                    "FRIEND" -> true     // everyone in friend groups
                    else -> false        // no limits for ONE_TIME groups
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),                               // padding around the list
                    verticalArrangement = Arrangement.spacedBy(16.dp) // space between items
                ) {

                    // shows different limit management based on group type
                    when (group.type) {

                        "FRIEND" -> {
                            // FRIEND groups — one group-wide limit
                            item {
                                Text(
                                    text = "Group Limit",              // section title
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // finds the existing group-wide limit
                                val existingLimit = limits.firstOrNull { it.userId == null }
                                LimitEditor(
                                    label = "Group Limit",             // label for the editor
                                    existingAmount = existingLimit?.amount?.toString() ?: "", // existing amount if any
                                    existingPeriod = existingLimit?.period ?: "MONTHLY",      // existing period if any
                                    canEdit = canEdit,                 // whether editing is allowed
                                    onSave = { amount, period ->
                                        viewModel.setLimit(            // saves the group-wide limit
                                            groupId = groupId,
                                            amount = amount,
                                            period = period,
                                            targetUserId = null        // null means group-wide
                                        ) {}
                                    }
                                )
                            }
                        }

                        "FAMILY" -> {
                            // FAMILY groups — per-member limits
                            item {
                                Text(
                                    text = if (isAdmin) "Member Limits" else "Your Limit", // title based on role
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // shows limits for each member
                            val visibleMembers = if (isAdmin) group.members // admins see all members
                            else group.members.filter { it.userId == currentUserId } // members see only themselves

                            items(visibleMembers) { member ->
                                val memberName = member.guestName ?: member.username ?: "Unknown" // gets display name
                                val existingLimit = limits.firstOrNull { it.userId == member.userId } // finds existing limit for this member
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (member.userId == currentUserId) "Your Limit" else memberName, // shows name or "Your Limit"
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LimitEditor(
                                            label = memberName,        // label for the editor
                                            existingAmount = existingLimit?.amount?.toString() ?: "", // existing amount if any
                                            existingPeriod = existingLimit?.period ?: "MONTHLY",      // existing period if any
                                            canEdit = canEdit,         // only admins can edit in family groups
                                            onSave = { amount, period ->
                                                viewModel.setLimit(    // saves the per-member limit
                                                    groupId = groupId,
                                                    amount = amount,
                                                    period = period,
                                                    targetUserId = member.userId // sets limit for this specific member
                                                ) {}
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// a reusable composable for editing a limit — shows amount and period inputs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitEditor(
    label: String,           // label shown above the editor
    existingAmount: String,  // the current amount — empty if no limit set
    existingPeriod: String,  // the current period — defaults to MONTHLY
    canEdit: Boolean,        // whether the user can edit this limit
    onSave: (Double, String) -> Unit // called when the user saves the limit
) {
    var amount by remember(existingAmount) { mutableStateOf(existingAmount) } // holds the typed amount
    var period by remember(existingPeriod) { mutableStateOf(existingPeriod) } // holds the selected period
    var expanded by remember { mutableStateOf(false) }                         // controls dropdown visibility

    val periods = listOf("DAILY", "WEEKLY", "MONTHLY") // available periods

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // amount input
        OutlinedTextField(
            value = amount,                                    // current typed amount
            onValueChange = { if (canEdit) amount = it },     // updates amount if editing is allowed
            label = { Text("Amount") },                        // label for the field
            modifier = Modifier.fillMaxWidth(),
            enabled = canEdit                                  // disables field if user cannot edit
        )

        // period dropdown
        ExposedDropdownMenuBox(
            expanded = expanded && canEdit,                    // only expands if editing is allowed
            onExpandedChange = { if (canEdit) expanded = !expanded } // toggles dropdown
        ) {
            OutlinedTextField(
                value = period,                                // shows selected period
                onValueChange = {},
                readOnly = true,                               // user can only select, not type
                label = { Text("Period") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && canEdit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),                             // anchors the dropdown to this field
                enabled = canEdit                              // disables field if user cannot edit
            )
            ExposedDropdownMenu(
                expanded = expanded && canEdit,
                onDismissRequest = { expanded = false }        // closes dropdown on dismiss
            ) {
                periods.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p) },                    // shows the period name
                        onClick = {
                            period = p                         // updates selected period
                            expanded = false                   // closes dropdown
                        }
                    )
                }
            }
        }

        // save button — only shown if user can edit
        if (canEdit) {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() // parses amount as a number
                    if (parsedAmount != null && parsedAmount > 0) {
                        onSave(parsedAmount, period)           // saves the limit
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank()                  // only enabled if amount is filled
            ) {
                Text("Save Limit")                            // save button label
            }
        }
    }
}