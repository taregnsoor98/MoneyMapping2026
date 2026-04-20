package com.example.moneymapping.ui.screens

import androidx.compose.foundation.clickable                          // makes a composable respond to tap events
import androidx.compose.foundation.layout.Arrangement                 // controls spacing between items in a Row or Column
import androidx.compose.foundation.layout.Column                      // arranges children vertically
import androidx.compose.foundation.layout.Row                         // arranges children horizontally
import androidx.compose.foundation.layout.Spacer                      // adds empty space between composables
import androidx.compose.foundation.layout.fillMaxSize                 // makes a composable fill all available space
import androidx.compose.foundation.layout.fillMaxWidth                // makes a composable fill the full width
import androidx.compose.foundation.layout.height                      // sets the height of a composable
import androidx.compose.foundation.layout.padding                     // adds padding around a composable
import androidx.compose.foundation.lazy.LazyColumn                    // a scrollable list that only renders visible items
import androidx.compose.foundation.lazy.items                         // renders each item in a list inside a LazyColumn
import androidx.compose.foundation.text.KeyboardOptions               // configures the keyboard type for text fields
import androidx.compose.material3.Button                              // a filled button
import androidx.compose.material3.Card                                // a card container with elevation and rounded corners
import androidx.compose.material3.CardDefaults                        // provides default card styling like elevation
import androidx.compose.material3.DropdownMenuItem                    // a single item inside a dropdown menu
import androidx.compose.material3.ExperimentalMaterial3Api            // needed for some Material3 components still in experimental state
import androidx.compose.material3.ExposedDropdownMenuBox              // a dropdown menu anchored to a text field
import androidx.compose.material3.ExposedDropdownMenuDefaults         // provides default trailing icon for the dropdown
import androidx.compose.material3.IconButton                          // a button that wraps an icon
import androidx.compose.material3.MaterialTheme                       // provides theme colors and typography
import androidx.compose.material3.OutlinedButton                      // a button with an outline instead of a filled background
import androidx.compose.material3.OutlinedTextField                   // a text input field with an outline border
import androidx.compose.material3.Scaffold                            // provides the basic screen structure
import androidx.compose.material3.Text                                // displays text
import androidx.compose.material3.TextButton                          // a button with no background — just text
import androidx.compose.runtime.Composable                            // marks a function as a Compose UI component
import androidx.compose.runtime.collectAsState                        // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                              // allows using 'by' delegation for state
import androidx.compose.runtime.mutableStateOf                        // creates a mutable state value
import androidx.compose.runtime.remember                              // remembers a value across recompositions
import androidx.compose.runtime.setValue                              // allows setting state with 'by' delegation
import androidx.compose.ui.Alignment                                  // controls alignment of composables
import androidx.compose.ui.Modifier                                   // used to style and layout composables
import androidx.compose.ui.text.font.FontWeight                       // used to make text bold
import androidx.compose.ui.text.input.KeyboardType                    // sets the keyboard type for text fields
import androidx.compose.ui.unit.dp                                    // density-independent pixels for sizing
import androidx.lifecycle.viewmodel.compose.viewModel                 // creates a ViewModel scoped to this composable
import androidx.navigation.NavController                              // handles navigation between screens
import com.example.moneymapping.data.CurrencyPreferenceManager       // provides the list of supported currencies for the searchable dropdown
import com.example.moneymapping.network.RecurringExpenseResponse     // the response model for a recurring expense

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RecurringExpensesScreen(navController: NavController) {           // receives navController to handle navigation back

    val viewModel: RecurringExpenseViewModel = viewModel()            // creates or retrieves the RecurringExpenseViewModel
    val recurringExpenses by viewModel.recurringExpenses.collectAsState() // observes the full list of recurring expenses
    val errorMessage by viewModel.errorMessage.collectAsState()       // observes the error message state

    var showAddForm by remember { mutableStateOf(false) }            // tracks whether the add form is visible
    var editingExpense by remember { mutableStateOf<RecurringExpenseResponse?>(null) } // holds the expense currently being edited — null means none

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recurring Expenses",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold                       // bold page title
                )
                TextButton(onClick = { navController.popBackStack() }) { // navigates back to the HomeScreen
                    Text("Back")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Error message ─────────────────────────────────────────────────
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,                            // shows the error message from the ViewModel
                    color = MaterialTheme.colorScheme.error,          // red error text
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                viewModel.clearError()                                // clears the error after showing it
            }

            // ── Add form or Add button ────────────────────────────────────────
            if (showAddForm) {
                RecurringExpenseForm(
                    title = "Add Recurring Expense",                  // form title for adding
                    onSave = { name, amount, currency, category, frequency, dayOfMonth, dayOfWeek ->
                        viewModel.addRecurringExpense(                // saves the new recurring expense via the ViewModel
                            name, amount, currency, category, frequency, dayOfMonth, dayOfWeek
                        )
                        showAddForm = false                           // closes the form after saving
                    },
                    onCancel = { showAddForm = false }               // closes the form without saving
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else if (editingExpense != null) {
                val exp = editingExpense!!                            // the expense currently being edited
                RecurringExpenseForm(
                    title = "Edit Recurring Expense",                 // form title for editing
                    initialName = exp.name,                           // pre-fills the current name
                    initialAmount = exp.amount.toString(),            // pre-fills the current amount
                    initialCurrency = exp.currency,                   // pre-fills the current currency
                    initialCategory = exp.category,                   // pre-fills the current category
                    initialFrequency = exp.frequency,                 // pre-selects the current frequency
                    initialDayOfMonth = exp.dayOfMonth?.toString() ?: "", // pre-fills the current day of month
                    initialDayOfWeek = exp.dayOfWeek?.toString() ?: "",   // pre-fills the current day of week
                    onSave = { name, amount, currency, category, frequency, dayOfMonth, dayOfWeek ->
                        viewModel.updateRecurringExpense(             // updates the expense via the ViewModel
                            exp.id, name, amount, currency, category, frequency, dayOfMonth, dayOfWeek
                        )
                        editingExpense = null                         // closes the form after saving
                    },
                    onCancel = { editingExpense = null }              // closes the form without saving
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Button(
                    onClick = { showAddForm = true },                 // opens the add form
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Recurring Expense")                   // button label
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Recurring expense list ────────────────────────────────────────
            if (recurringExpenses.isEmpty()) {
                Text(
                    text = "No recurring expenses yet. Tap the button above to add one.", // shown when the list is empty
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between cards
                ) {
                    items(recurringExpenses) { expense ->
                        RecurringExpenseCard(
                            expense = expense,
                            onEditClick = { editingExpense = expense }, // opens the edit form for this expense
                            onDeleteClick = { viewModel.deleteRecurringExpense(expense.id) } // deletes this expense
                        )
                    }
                }
            }
        }
    }
}

// ─── Recurring expense card ───────────────────────────────────────────────────

// shows a single recurring expense with its details and edit/delete buttons
@Composable
private fun RecurringExpenseCard(
    expense: RecurringExpenseResponse,  // the recurring expense to display
    onEditClick: () -> Unit,            // called when the user taps the edit button
    onDeleteClick: () -> Unit           // called when the user taps the delete button
) {
    // builds a human-readable schedule label e.g. "Monthly on the 15th" or "Weekly on Monday"
    val scheduleLabel = when (expense.frequency) {
        "DAILY"   -> "Every day"                                      // daily — no day needed
        "WEEKLY"  -> {
            val dayNames = mapOf(                                      // maps day numbers to names — 1=Monday through 7=Sunday
                1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
                4 to "Thursday", 5 to "Friday", 6 to "Saturday", 7 to "Sunday"
            )
            "Weekly on ${dayNames[expense.dayOfWeek] ?: "?"}"        // e.g. "Weekly on Monday"
        }
        "MONTHLY" -> "Monthly on the ${expense.dayOfMonth}${daySuffix(expense.dayOfMonth ?: 1)}" // e.g. "Monthly on the 15th"
        else      -> expense.frequency                                // fallback — shows the raw frequency string
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.name,                              // e.g. "Netflix"
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${expense.category} · $scheduleLabel",   // e.g. "Entertainment · Monthly on the 15th"
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${expense.currency} ${"%.2f".format(expense.amount)}", // e.g. "USD 9.99"
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary         // highlighted in theme color
                )
                Row {
                    IconButton(onClick = onEditClick) {
                        Text("✏️")                                    // edit icon
                    }
                    IconButton(onClick = onDeleteClick) {
                        Text("🗑️")                                   // delete icon
                    }
                }
            }
        }
    }
}

// ─── Add / Edit form ──────────────────────────────────────────────────────────

// reusable form used for both adding and editing a recurring expense
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringExpenseForm(
    title: String,                        // form title — "Add Recurring Expense" or "Edit Recurring Expense"
    initialName: String = "",             // pre-filled name — empty for add, existing value for edit
    initialAmount: String = "",           // pre-filled amount
    initialCurrency: String = "USD",      // pre-filled currency
    initialCategory: String = "Entertainment", // pre-filled category
    initialFrequency: String = "MONTHLY", // pre-selected frequency
    initialDayOfMonth: String = "",       // pre-filled day of month — only shown when frequency is MONTHLY
    initialDayOfWeek: String = "",        // pre-filled day of week — only shown when frequency is WEEKLY
    onSave: (String, Double, String, String, String, Int?, Int?) -> Unit, // called with all values when user taps Save
    onCancel: () -> Unit                  // called when the user taps Cancel
) {
    var name by remember(initialName) { mutableStateOf(initialName) }               // holds the typed name
    var amount by remember(initialAmount) { mutableStateOf(initialAmount) }         // holds the typed amount
    var currency by remember(initialCurrency) { mutableStateOf(initialCurrency) }   // holds the selected currency
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }   // holds the selected category
    var frequency by remember(initialFrequency) { mutableStateOf(initialFrequency) } // holds the selected frequency
    var dayOfMonth by remember(initialDayOfMonth) { mutableStateOf(initialDayOfMonth) } // holds the typed day of month
    var dayOfWeek by remember(initialDayOfWeek) { mutableStateOf(initialDayOfWeek) }    // holds the typed day of week

    var frequencyExpanded by remember { mutableStateOf(false) }     // tracks whether the frequency dropdown is open
    var categoryExpanded by remember { mutableStateOf(false) }      // tracks whether the category dropdown is open
    var currencyExpanded by remember { mutableStateOf(false) }      // tracks whether the currency dropdown is open
    var currencySearch by remember { mutableStateOf("") }           // holds the search query for filtering currencies

    val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY")          // available frequency options
    val categories = listOf(                                         // available category options
        "Food", "Transport", "Housing", "Entertainment",
        "Health", "Shopping", "Education", "Travel", "Other"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold                          // bold form title
            )

            Spacer(modifier = Modifier.height(8.dp))

            // name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },                        // updates name as the user types
                label = { Text("Name (e.g. Netflix)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // amount and currency fields — amount on the left, currency searchable dropdown on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },                  // updates amount as the user types
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(2f),                   // takes up 2/3 of the row
                    singleLine = true
                )
                // searchable currency dropdown
                val filteredCurrencies = CurrencyPreferenceManager.SUPPORTED_CURRENCIES
                    .filter { it.contains(currencySearch.uppercase(), ignoreCase = true) } // filters list as user types
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (currencyExpanded) currencySearch else currency, // shows search query when open, selection when closed
                        onValueChange = { currencySearch = it.uppercase() },        // updates search query as user types
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded && filteredCurrencies.isNotEmpty(),
                        onDismissRequest = { currencyExpanded = false; currencySearch = "" }
                    ) {
                        filteredCurrencies.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur) },
                                onClick = {
                                    currency = cur                    // selects the currency
                                    currencyExpanded = false          // closes the dropdown
                                    currencySearch = ""               // clears the search query
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},                               // read-only — user picks from dropdown
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { category = cat; categoryExpanded = false } // selects the category and closes the dropdown
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // frequency dropdown
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {},                               // read-only — user picks from dropdown
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    frequencies.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = { frequency = freq; frequencyExpanded = false } // selects the frequency and closes the dropdown
                        )
                    }
                }
            }

            // day of month field — only shown when frequency is MONTHLY
            if (frequency == "MONTHLY") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dayOfMonth,
                    onValueChange = { dayOfMonth = it },              // updates day of month as the user types
                    label = { Text("Day of month (1–31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // day of week field — only shown when frequency is WEEKLY
            if (frequency == "WEEKLY") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dayOfWeek,
                    onValueChange = { dayOfWeek = it },               // updates day of week as the user types
                    label = { Text("Day of week (1=Mon, 7=Sun)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // save and cancel buttons side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull()    // tries to parse the amount as a number
                        if (name.isNotBlank() && parsedAmount != null && parsedAmount > 0) { // only saves if name and amount are valid
                            val dom = if (frequency == "MONTHLY") dayOfMonth.toIntOrNull() else null // parses day of month only for MONTHLY
                            val dow = if (frequency == "WEEKLY") dayOfWeek.toIntOrNull() else null   // parses day of week only for WEEKLY
                            onSave(name.trim(), parsedAmount, currency.trim(), category, frequency, dom, dow) // calls save with all parsed values
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

// returns the ordinal suffix for a day number e.g. 1 → "st", 2 → "nd", 3 → "rd", 15 → "th"
private fun daySuffix(day: Int): String {
    return when {
        day in 11..13 -> "th"                                        // 11th, 12th, 13th are exceptions to the normal rule
        day % 10 == 1 -> "st"                                        // 1st, 21st, 31st
        day % 10 == 2 -> "nd"                                        // 2nd, 22nd
        day % 10 == 3 -> "rd"                                        // 3rd, 23rd
        else           -> "th"                                        // everything else
    }
}