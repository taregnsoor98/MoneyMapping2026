package com.example.moneymapping.ui.screens

// Edit Expense Screen
// Loads the existing expense by ID and lets the user edit description, date, currency, category, items and assignments.
// Saves the updated expense back to the backend via PUT /expenses/{id}.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import com.example.moneymapping.network.CreateExpenseRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    expenseId: String,           // the ID of the expense to edit
    onExpenseSaved: () -> Unit,  // callback to navigate back after saving
    onBack: () -> Unit           // callback to navigate back when back is pressed
) {
    val viewModel: EditExpenseViewModel = viewModel() // creates the EditExpenseViewModel
    val state by viewModel.state.collectAsState()             // observes the current state
    val editableItems by viewModel.editableItems.collectAsState() // observes the editable items list
    val searchState by viewModel.searchState.collectAsState()     // observes the user search state

    // Available currency options
    val currencies = listOf("USD", "EUR", "GBP", "JOD", "RUB")

    // Available category options
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Health", "Education", "Other")

    // Editable fields — pre-filled when the expense loads
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var category by remember { mutableStateOf("") }

    // Tracks whether dropdowns are open
    var currencyExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Tracks which item's search field is currently active — -1 means none
    var activeSearchIndex by remember { mutableStateOf(-1) }

    // Tracks the search query per item index
    var searchQueryPerItem by remember { mutableStateOf(mapOf<Int, String>()) }

    // Tracks the guest name input per item index
    var guestNamePerItem by remember { mutableStateOf(mapOf<Int, String>()) }

    // State for the date picker
    val datePickerState = rememberDatePickerState()

    // Loads the expense when the screen first opens
    LaunchedEffect(expenseId) {
        viewModel.loadExpense(expenseId) // fetches the expense by ID
    }

    // Pre-fills the fields when the expense is loaded, and navigates back when saved
    LaunchedEffect(state) {
        when (val currentState = state) {
            is EditExpenseState.Loaded -> {
                description = currentState.expense.description // pre-fills description
                date = currentState.expense.date               // pre-fills date
                currency = currentState.expense.currency       // pre-fills currency
                category = currentState.expense.category       // pre-fills category
            }
            is EditExpenseState.Saved -> onExpenseSaved() // navigates back after saving
            else -> {}
        }
    }

    // Shows the date picker dialog when triggered
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        date = sdf.format(Date(millis)) // saves the formatted date
                    }
                    showDatePicker = false // closes the dialog
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState) // the actual date picker UI
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Expense") }, // top bar title
                navigationIcon = {
                    IconButton(onClick = onBack) { // back button
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back" // accessibility description
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // allows scrolling when items list is long
            verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between fields
        ) {

            when (state) {

                // Shows a spinner while loading the expense
                is EditExpenseState.Loading -> {
                    CircularProgressIndicator() // spinning loader
                }

                // Shows the edit form when expense is loaded or while saving
                is EditExpenseState.Loaded, is EditExpenseState.Saving, is EditExpenseState.Error -> {

                    // Description field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it }, // updates description
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date picker field
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true, // user opens date picker instead of typing
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
                        }
                    )

                    // Currency dropdown
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded }
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
                                        currency = option        // saves selected currency
                                        currencyExpanded = false // closes dropdown
                                    }
                                )
                            }
                        }
                    }

                    // Category dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
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
                                        category = option        // saves selected category
                                        categoryExpanded = false // closes dropdown
                                    }
                                )
                            }
                        }
                    }

                    Divider() // divides basic info from items section

                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold // bold section title
                    )

                    // Shows each editable item as a card
                    editableItems.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {

                                // Item name field
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = {
                                        viewModel.updateItem(index, item.copy(name = it)) // updates item name
                                    },
                                    label = { Text("Item Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                    // Unit price field
                                    OutlinedTextField(
                                        value = item.unitPrice.toString(),
                                        onValueChange = { input ->
                                            val price = input.toDoubleOrNull() ?: 0.0 // converts safely
                                            val total = price * item.quantity          // recalculates total
                                            viewModel.updateItem(index, item.copy(unitPrice = price, totalPrice = total))
                                        },
                                        label = { Text("Unit Price") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Quantity field
                                    OutlinedTextField(
                                        value = item.quantity.toString(),
                                        onValueChange = { input ->
                                            val qty = input.toDoubleOrNull() ?: 1.0 // converts safely
                                            val total = item.unitPrice * qty         // recalculates total
                                            viewModel.updateItem(index, item.copy(quantity = qty, totalPrice = total))
                                        },
                                        label = { Text("Qty") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Shows the calculated total for this item
                                    Text(
                                        text = "Total: ${String.format("%.2f", item.totalPrice)}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    // Delete button for this item
                                    IconButton(onClick = { viewModel.removeItem(index) }) {
                                        Text("🗑️") // trash icon
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp)) // divides item fields from assignments

                                Text(
                                    text = "Assigned to:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold // bold label
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Shows each assigned person as a removable chip
                                item.assignments.forEachIndexed { assignmentIndex, assignment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Person name chip — tap to remove
                                        FilterChip(
                                            selected = true,
                                            onClick = {
                                                // Removes this assignment from the item
                                                val updatedAssignments = item.assignments.toMutableList()
                                                updatedAssignments.removeAt(assignmentIndex) // removes by index
                                                viewModel.updateItem(index, item.copy(assignments = updatedAssignments))
                                            },
                                            label = { Text("${assignment.personName} ✕") } // shows name with remove icon
                                        )

                                        // Shows quantity field for this person
                                        OutlinedTextField(
                                            value = assignment.quantity.toString(),
                                            onValueChange = { input ->
                                                val qty = input.toDoubleOrNull() ?: 1.0 // converts safely
                                                val updatedAssignments = item.assignments.toMutableList()
                                                updatedAssignments[assignmentIndex] = assignment.copy(quantity = qty) // updates quantity
                                                viewModel.updateItem(index, item.copy(assignments = updatedAssignments))
                                            },
                                            label = { Text("Qty") },
                                            modifier = Modifier.width(80.dp), // fixed width for quantity field
                                            singleLine = true                  // keeps it on one line
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Search field to add an existing user to this item
                                ExposedDropdownMenuBox(
                                    expanded = activeSearchIndex == index && searchState is EditSearchState.Results,
                                    onExpandedChange = {} // controlled manually
                                ) {
                                    OutlinedTextField(
                                        value = searchQueryPerItem[index] ?: "", // per-item search query
                                        onValueChange = { query ->
                                            searchQueryPerItem = searchQueryPerItem + (index to query) // stores query per item
                                            activeSearchIndex = index                                   // marks this item as active
                                            if (query.length >= 2) {
                                                viewModel.searchUsers(query) // searches if query is long enough
                                            } else {
                                                viewModel.resetSearch() // clears results if too short
                                            }
                                        },
                                        label = { Text("Search user to assign") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(), // anchors dropdown to this field
                                        trailingIcon = {
                                            if (activeSearchIndex == index && searchState is EditSearchState.Loading) {
                                                CircularProgressIndicator(modifier = Modifier.padding(8.dp)) // spinner while searching
                                            }
                                        }
                                    )

                                    // Shows search results as dropdown for this item only
                                    if (activeSearchIndex == index && searchState is EditSearchState.Results) {
                                        ExposedDropdownMenu(
                                            expanded = true,
                                            onDismissRequest = {
                                                activeSearchIndex = -1  // clears active search
                                                viewModel.resetSearch() // clears search results
                                            }
                                        ) {
                                            (searchState as EditSearchState.Results).users.forEach { user ->
                                                DropdownMenuItem(
                                                    text = { Text(user.username) },
                                                    onClick = {
                                                        // Adds user as a new assignment if not already assigned
                                                        val alreadyAssigned = item.assignments.any { it.personName == user.username }
                                                        if (!alreadyAssigned) {
                                                            val updatedAssignments = item.assignments + EditableAssignment(personName = user.username)
                                                            viewModel.updateItem(index, item.copy(assignments = updatedAssignments))
                                                        }
                                                        searchQueryPerItem = searchQueryPerItem + (index to "") // clears search field
                                                        activeSearchIndex = -1                                  // clears active search
                                                        viewModel.resetSearch()                                 // clears search results
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Guest add row — add a person without an account
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
                                            val alreadyAssigned = item.assignments.any { it.personName == name }
                                            if (name.isNotEmpty() && !alreadyAssigned) {
                                                val updatedAssignments = item.assignments + EditableAssignment(personName = name) // adds guest as assignment
                                                viewModel.updateItem(index, item.copy(assignments = updatedAssignments))
                                                guestNamePerItem = guestNamePerItem + (index to "") // clears guest name field
                                            }
                                        }
                                    ) {
                                        Text("+") // plus button to add guest
                                    }
                                }
                            }
                        }
                    }

                    // Button to add a new empty item
                    OutlinedButton(
                        onClick = { viewModel.addItem() }, // adds a new empty item
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Item") // label for add item button
                    }

                    // Shows error message if something went wrong
                    if (state is EditExpenseState.Error) {
                        Text(
                            text = (state as EditExpenseState.Error).message,
                            color = MaterialTheme.colorScheme.error // red error text
                        )
                    }

                    // Save button
                    Button(
                        onClick = {
                            val currentState = state
                            if (currentState is EditExpenseState.Loaded) {
                                viewModel.saveExpense(
                                    expenseId = expenseId,
                                    request = CreateExpenseRequest(
                                        groupId = currentState.expense.groupId,               // keeps existing group ID
                                        amount = currentState.expense.amount,                // keeps existing amount
                                        currency = currency,                                 // updated currency
                                        description = description,                           // updated description
                                        category = category,                                 // updated category
                                        date = date,                                         // updated date
                                        isOneTimeSplit = currentState.expense.isOneTimeSplit, // keeps existing split flag
                                        receiptImages = currentState.expense.receiptImages   // keeps existing images
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state !is EditExpenseState.Saving && description.isNotEmpty() && date.isNotEmpty() && category.isNotEmpty() // only enabled if fields are filled
                    ) {
                        if (state is EditExpenseState.Saving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp)) // shows spinner while saving
                        } else {
                            Text("Save Changes") // normal button text
                        }
                    }
                }

                else -> {} // saved state is handled by LaunchedEffect above
            }
        }
    }
}