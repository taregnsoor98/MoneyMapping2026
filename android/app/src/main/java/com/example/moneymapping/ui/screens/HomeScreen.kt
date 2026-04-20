package com.example.moneymapping.ui.screens

import androidx.compose.animation.AnimatedVisibility                  // animates the expand/collapse of the limit editor
import androidx.compose.foundation.clickable                          // makes a composable respond to tap events
import androidx.compose.foundation.layout.Arrangement                 // controls spacing between items in a Row or Column
import androidx.compose.foundation.layout.Box                         // a layout that stacks children on top of each other
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
import androidx.compose.material3.CircularProgressIndicator           // a spinning loading indicator
import androidx.compose.material3.DropdownMenuItem                    // a single item inside a dropdown menu
import androidx.compose.material3.ExperimentalMaterial3Api            // needed for some Material3 components still in experimental state
import androidx.compose.material3.ExposedDropdownMenuBox              // a dropdown menu anchored to a text field
import androidx.compose.material3.ExposedDropdownMenuDefaults         // provides default trailing icon for the dropdown
import androidx.compose.material3.FloatingActionButton                // the floating + button at the bottom right
import androidx.compose.material3.LinearProgressIndicator             // a horizontal progress bar
import androidx.compose.material3.MaterialTheme                       // provides theme colors and typography
import androidx.compose.material3.OutlinedButton                      // a button with an outline instead of a filled background
import androidx.compose.material3.OutlinedTextField                   // a text input field with an outline border
import androidx.compose.material3.Scaffold                            // provides the basic screen structure with FAB support
import androidx.compose.material3.Text                                // displays text
import androidx.compose.material3.TextButton                          // a button with no background — just text
import androidx.compose.runtime.Composable                            // marks a function as a Compose UI component
import androidx.compose.runtime.LaunchedEffect                        // runs a side effect when the composable first appears
import androidx.compose.runtime.collectAsState                        // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                              // allows using 'by' delegation for state
import androidx.compose.runtime.mutableStateOf                        // creates a mutable state value
import androidx.compose.runtime.remember                              // remembers a value across recompositions
import androidx.compose.runtime.setValue                              // allows setting state with 'by' delegation
import androidx.compose.ui.Alignment                                  // controls alignment of composables
import androidx.compose.ui.Modifier                                   // used to style and layout composables
import androidx.compose.ui.graphics.Color                             // used to specify custom colors
import androidx.compose.ui.text.font.FontWeight                       // used to make text bold
import androidx.compose.ui.text.input.KeyboardType                    // sets the keyboard to number mode for amount input
import androidx.compose.ui.unit.dp                                    // density-independent pixels for sizing
import androidx.compose.ui.unit.sp                                    // scale-independent pixels for font sizes
import androidx.lifecycle.viewmodel.compose.viewModel                 // creates a ViewModel scoped to this composable
import androidx.navigation.NavController                              // handles navigation between screens
import androidx.navigation.compose.currentBackStackEntryAsState       // observes navigation changes to trigger re-fetches
import com.example.moneymapping.network.ExpenseResponse              // the expense data model
import com.example.moneymapping.network.SpendingLimitResponse        // the limit data model
import com.example.moneymapping.ui.navigation.Screen                 // the sealed class that defines all navigation routes
import androidx.compose.foundation.Canvas                             // used to draw the spending bars directly on screen
import androidx.compose.foundation.layout.width                       // sets a fixed width for the category label column
import androidx.compose.ui.geometry.Size                              // used inside Canvas to set the size of each drawn bar
import com.example.moneymapping.network.RecurringExpenseResponse     // the response model for a recurring expense — used by the HomeScreen card
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymapping.data.CurrencyPreferenceManager
// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(navController: NavController) {                        // receives navController to handle navigation

    val viewModel: HomeViewModel = viewModel()                        // creates or retrieves the HomeViewModel
    val expensesState by viewModel.expensesState.collectAsState()     // observes the expenses state from the ViewModel
    val limitState by viewModel.limitState.collectAsState()           // observes the personal limit state from the ViewModel
    val spentAmount by viewModel.spentAmount.collectAsState()         // observes the calculated spent amount from the ViewModel
    val navBackStackEntry by navController.currentBackStackEntryAsState() // observes navigation changes
    val exchangeRates by viewModel.exchangeRates.collectAsState()     // observes the fetched exchange rates
    val homeCurrency by viewModel.homeCurrency.collectAsState()       // observes the user's home currency
    val localCurrency by viewModel.localCurrency.collectAsState()     // observes the user's local currency
    val credits by viewModel.credits.collectAsState()                 // observes the list of credits from the ViewModel
    val recurringViewModel: RecurringExpenseViewModel = viewModel()   // creates or retrieves the RecurringExpenseViewModel
    val upcomingExpenses by recurringViewModel.upcomingExpenses.collectAsState() // observes recurring expenses due in the next 7 days

    // re-fetches expenses whenever a recurring expense auto-executes today
    LaunchedEffect(Unit) {
        recurringViewModel.expensesNeedRefresh.collect {
            viewModel.fetchExpenses()
        }
    }

    // re-fetches all data every time the user returns to this screen
    LaunchedEffect(navBackStackEntry) {
        viewModel.fetchAll()                                          // refreshes both expenses and limit on screen return
        recurringViewModel.fetchRecurringExpenses()                   // refreshes recurring expenses so the card shows up-to-date data
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddExpense.route) } // opens Add Expense screen
            ) {
                Text("+")                                             // plus icon label on the FAB
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Text(
                text = "My Expenses",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold                          // bold page title
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Safe-to-spend card ────────────────────────────────────────────
            SafeToSpendCard(
                limitState = limitState,
                spentAmount = spentAmount,
                exchangeRates = exchangeRates,
                homeCurrency = homeCurrency,
                localCurrency = localCurrency,
                onSave = { amount, period ->
                    viewModel.savePersonalLimit(amount, period)
                },
                onAddToScore = { amount, comment, currency ->
                    viewModel.addCredit(amount, comment, currency)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // ── Recurring Expenses card ───────────────────────────────────────
            RecurringExpensesCard(
                upcomingExpenses = upcomingExpenses,                  // passes the expenses due in the next 7 days
                onViewAllClick = {
                    navController.navigate(Screen.RecurringExpenses.route) // opens the full recurring expenses management screen
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Recent Transactions card ──────────────────────────────────────
            when (val state = expensesState) {
                is ExpensesState.Success -> RecentTransactionsCard(
                    expenses = state.expenses,                        // passes the loaded personal expenses — card takes the last 5
                    onTransactionClick = { expense ->
                        navController.navigate(                       // navigates to the detail screen for the tapped expense
                            Screen.DetailExpense.route.replace("{expenseId}", expense.id)
                        )
                    },
                    onViewAllClick = {
                        navController.navigate(Screen.AllExpenses.route) // opens the full searchable expense list
                    }
                )
                else -> {}                                            // nothing shown while loading or on error — Safe-to-Spend card is already visible
            }
        }
    }
}

// ─── Safe-to-spend card ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SafeToSpendCard(
    limitState: LimitState,
    spentAmount: Double,
    exchangeRates: Map<String, Double>,
    homeCurrency: String,
    localCurrency: String,
    onSave: (Double, String) -> Unit,
    onAddToScore: (Double, String, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }              // tracks whether the inline editor is open or closed

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // slightly raised card for visual prominence
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            when (limitState) {

                // ── Still loading the limit ───────────────────────────────────
                is LimitState.Loading -> {
                    CircularProgressIndicator(                        // shows a small spinner while the limit is being fetched
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // ── No limit set yet ─────────────────────────────────────────
                is LimitState.NotSet -> {
                    if (!isEditing) {
                        // prompt the user to set their first limit
                        Text(
                            text = "💰 Safe to Spend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold             // bold card title
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set a spending limit to see how much you can safely spend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter hint text
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { isEditing = true },          // opens the inline editor
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set my limit")                     // button label
                        }
                    } else {
                        // inline editor — shown when user taps "Set my limit"
                        LimitEditorInline(
                            existingAmount = "",                     // no existing amount — this is a fresh setup
                            existingPeriod = "MONTHLY",              // defaults to monthly as a sensible starting point
                            onSave = { amount, period ->
                                onSave(amount, period)               // saves the limit via the ViewModel
                                isEditing = false                    // closes the editor after saving
                            },
                            onCancel = { isEditing = false }         // closes the editor without saving
                        )
                    }
                }

                // ── Limit is set — show the card ─────────────────────────────
                is LimitState.Set -> {
                    val limit = limitState.limit                     // the loaded limit object
                    val remaining = limit.amount - spentAmount       // how much the user has left to spend
                    val usagePercent = (spentAmount / limit.amount)  // usage as a fraction between 0.0 and 1.0+
                        .coerceIn(0.0, 1.0)                          // clamps to 1.0 max so the bar never overflows

                    // determines the color based on how much has been spent
                    val statusColor = when {
                        usagePercent >= 1.0  -> Color(0xFFE53935)    // red — over budget
                        usagePercent >= 0.90 -> Color(0xFFE53935)    // red — 90%+ used
                        usagePercent >= 0.75 -> Color(0xFFFFA726)    // orange — 75%+ used, approaching limit
                        else                 -> Color(0xFF43A047)    // green — safe to spend
                    }

                    if (!isEditing) {

                        // ── Card display mode ─────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💰 Safe to Spend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold         // bold card title
                            )
                            TextButton(onClick = { isEditing = true }) { // opens the editor to change the limit
                                Text("Edit")                         // edit button label
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // shows the period label e.g. "Monthly limit"
                        Text(
                            text = "${limit.period.lowercase().replaceFirstChar { it.uppercase() }} limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter text for the label
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // determine the second currency to show (whichever isn't the limit currency)
                        val limitCurrency = limit.currency.takeIf { it.isNotEmpty() } ?: homeCurrency // uses the limit's currency, falls back to home currency if null or empty
                        val otherCurrency = if (limitCurrency == homeCurrency) localCurrency else homeCurrency // picks the other currency to show
                        val otherRate = if (exchangeRates.isNotEmpty()) {                   // calculates the conversion rate between the two currencies
                            val fromRate = exchangeRates[limitCurrency] ?: 1.0              // rate of limit currency relative to base
                            val toRate = exchangeRates[otherCurrency] ?: 1.0                // rate of other currency relative to base
                            toRate / fromRate                                               // conversion rate from limit currency to other currency
                        } else 1.0                                                          // falls back to 1.0 if rates are not loaded yet
                        val remainingInOther = remaining * otherRate                        // remaining amount converted to the other currency
                        val limitInOther = limit.amount * otherRate                         // limit amount converted to the other currency
                        val spentInOther = spentAmount * otherRate                          // spent amount converted to the other currency

                        // shows the remaining amount in the limit currency — the main number the user needs
                        Text(
                            text = if (remaining >= 0)
                                "$limitCurrency ${"%.2f".format(remaining)} left"           // e.g. "JOD 142.50 left"
                            else
                                "$limitCurrency ${"%.2f".format(-remaining)} over budget",  // e.g. "JOD 12.00 over budget"
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor                                             // green, orange, or red based on usage
                        )

                        // shows the remaining amount converted to the other currency
                        if (exchangeRates.isNotEmpty()) {
                            Text(
                                text = if (remaining >= 0)
                                    "≈ $otherCurrency ${"%.2f".format(remainingInOther)} left"          // e.g. "≈ RUB 8,500.00 left"
                                else
                                    "≈ $otherCurrency ${"%.2f".format(-remainingInOther)} over budget", // e.g. "≈ RUB 720.00 over budget"
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusColor.copy(alpha = 0.7f)                     // slightly faded version of the status color
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // shows spent vs limit in the limit currency
                        Text(
                            text = "Spent $limitCurrency ${"%.2f".format(spentAmount)} of ${"%.2f".format(limit.amount)}", // e.g. "Spent JOD 358.00 of 500.00"
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant              // lighter text for the subtitle
                        )

                        // shows spent vs limit converted to the other currency
                        if (exchangeRates.isNotEmpty()) {
                            Text(
                                text = "≈ $otherCurrency ${"%.2f".format(spentInOther)} of ${"%.2f".format(limitInOther)}", // e.g. "≈ RUB 21,480.00 of 30,000.00"
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant          // lighter text for the subtitle
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // horizontal progress bar showing how much of the limit is used
                        LinearProgressIndicator(
                            progress = { usagePercent.toFloat() },   // progress value between 0.0 and 1.0
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),                       // slightly thicker bar for visibility
                            color = statusColor,                     // bar color matches the status color
                            trackColor = MaterialTheme.colorScheme.surfaceVariant // light grey track behind the bar
                        )

                        var isAddingToScore by remember { mutableStateOf(false) }       // tracks whether the "Add to score" form is open

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isAddingToScore) {
                            // ── "Add to score" button ─────────────────────────
                            OutlinedButton(
                                onClick = { isAddingToScore = true },                    // opens the inline form
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("➕ Add to score")                                  // button label
                            }
                        } else {
                            // ── Inline "Add to score" form ────────────────────
                            AddToScoreForm(
                                onSave = { amount, comment, currency ->
                                    onAddToScore(amount, comment, currency)
                                    isAddingToScore = false
                                },
                                onCancel = { isAddingToScore = false }
                            )
                        }


                    } else {

                        // ── Inline editor mode ────────────────────────────────
                        LimitEditorInline(
                            existingAmount = limit.amount.toString(), // pre-fills the current amount
                            existingPeriod = limit.period,            // pre-selects the current period
                            onSave = { amount, period ->
                                onSave(amount, period)                // saves the updated limit via the ViewModel
                                isEditing = false                     // closes the editor after saving
                            },
                            onCancel = { isEditing = false }          // closes the editor without saving
                        )
                    }
                }

                // ── Error state ───────────────────────────────────────────────
                is LimitState.Error -> {
                    Text(
                        text = "Could not load limit: ${limitState.message}", // shows what went wrong
                        color = MaterialTheme.colorScheme.error,      // red error color
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ─── Inline limit editor ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LimitEditorInline(
    existingAmount: String,              // the current amount — empty string if no limit set yet
    existingPeriod: String,              // the current period — used to pre-select the dropdown
    onSave: (Double, String) -> Unit,    // called with the parsed amount and selected period when user taps Save
    onCancel: () -> Unit                 // called when the user taps Cancel to close without saving
) {
    var amount by remember(existingAmount) { mutableStateOf(existingAmount) } // holds the typed amount — re-initializes if existingAmount changes
    var period by remember(existingPeriod) { mutableStateOf(existingPeriod) } // holds the selected period — re-initializes if existingPeriod changes
    var expanded by remember { mutableStateOf(false) }                         // tracks whether the period dropdown is open

    val periods = listOf("DAILY", "WEEKLY", "MONTHLY")                        // the three available period options

    Text(
        text = "Set spending limit",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold                                           // bold section title inside the editor
    )

    Spacer(modifier = Modifier.height(8.dp))

    // amount input field — opens a number keyboard
    OutlinedTextField(
        value = amount,                                                        // shows the currently typed amount
        onValueChange = { amount = it },                                       // updates the amount as the user types
        label = { Text("Amount") },                                            // floating label inside the field
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), // shows a decimal number keyboard
        modifier = Modifier.fillMaxWidth(),
        singleLine = true                                                      // prevents the field from expanding to multiple lines
    )

    Spacer(modifier = Modifier.height(8.dp))

    // period dropdown — lets the user pick Daily, Weekly, or Monthly
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }                            // toggles the dropdown open/closed on tap
    ) {
        OutlinedTextField(
            value = period,                                                    // shows the currently selected period
            onValueChange = {},                                                // read-only — user can only select from the dropdown
            readOnly = true,                                                   // prevents keyboard from appearing
            label = { Text("Period") },                                        // floating label inside the field
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)  // shows the chevron arrow icon
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()                                                  // anchors the dropdown menu to this text field
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }                            // closes the dropdown if the user taps outside
        ) {
            periods.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p) },                                        // shows the period name e.g. "MONTHLY"
                    onClick = {
                        period = p                                             // updates the selected period
                        expanded = false                                       // closes the dropdown after selection
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // save and cancel buttons side by side
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)                    // adds space between the two buttons
    ) {
        OutlinedButton(
            onClick = onCancel,                                                // closes the editor without saving
            modifier = Modifier.weight(1f)                                    // takes up half the available width
        ) {
            Text("Cancel")                                                     // cancel button label
        }
        Button(
            onClick = {
                val parsed = amount.toDoubleOrNull()                           // tries to parse the typed amount as a number
                if (parsed != null && parsed > 0) {                           // only saves if the amount is a valid positive number
                    onSave(parsed, period)                                     // calls the save callback with the parsed amount and period
                }
            },
            modifier = Modifier.weight(1f)                                    // takes up the other half of the available width
        ) {
            Text("Save")                                                       // save button label
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

// shows a centered loading spinner while expenses are being fetched
@Composable
private fun LoadingContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()                                            // centered spinner
    }
}

// shows a centered error message if the expense fetch fails
@Composable
private fun ErrorContent(message: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error                            // red error text
        )
    }
}

// shows the list of expenses or an empty state message if the list is empty
@Composable
private fun ExpenseListContent(
    expenses: List<ExpenseResponse>,              // the list of expenses to display
    exchangeRates: Map<String, Double>,           // the latest exchange rates fetched from the backend
    homeCurrency: String,                         // the user's home currency e.g. "USD"
    localCurrency: String,                        // the user's local currency e.g. "EUR"
    onExpenseClick: (ExpenseResponse) -> Unit,    // called when the user taps an expense card
    onDeleteClick: (ExpenseResponse) -> Unit      // called when the user taps the delete button on a card
) {
    if (expenses.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "No expenses yet. Tap + to add one!",
                color = MaterialTheme.colorScheme.onSurfaceVariant             // lighter hint text
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)                 // spacing between expense cards
        ) {
            items(expenses) { expense ->
                ExpenseCard(
                    expense = expense,
                    exchangeRates = exchangeRates,                    // passes exchange rates to each card
                    homeCurrency = homeCurrency,                      // passes home currency to each card
                    localCurrency = localCurrency,                    // passes local currency to each card
                    onClick = { onExpenseClick(expense) },            // passes the tapped expense to the callback
                    onDeleteClick = { onDeleteClick(expense) }        // passes the expense to the delete callback
                )
            }
        }
    }
}

// ─── Add to score form ────────────────────────────────────────────────────────

// inline form shown when the user taps "Add to score" — lets them enter an amount and a comment
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToScoreForm(
    onSave: (Double, String, String) -> Unit, // called with the parsed amount, comment and currency when the user taps Save
    onCancel: () -> Unit                      // called when the user taps Cancel to close without saving
) {
    var amount by remember { mutableStateOf("") }        // holds the typed amount — starts empty
    var comment by remember { mutableStateOf("") }       // holds the typed comment — starts empty
    var currency by remember { mutableStateOf("") }      // holds the selected currency — starts empty
    var currencyExpanded by remember { mutableStateOf(false) } // tracks whether the currency dropdown is open
    var currencySearch by remember { mutableStateOf("") }      // holds the search query for filtering currencies
    Text(
        text = "Add to score",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold                  // bold section title inside the form
    )

    Spacer(modifier = Modifier.height(8.dp))

    // amount and currency fields side by side
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },              // updates the amount as the user types
            label = { Text("Amount received") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(2f),               // takes up 2/3 of the row
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
                            currency = cur             // selects the currency
                            currencyExpanded = false   // closes the dropdown
                            currencySearch = ""        // clears the search query
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // comment input field — free text e.g. "Ahmad paid me back $50 for dinner"
    OutlinedTextField(
        value = comment,                              // shows the currently typed comment
        onValueChange = { comment = it },             // updates the comment as the user types
        label = { Text("Note (e.g. Ahmad paid me back)") }, // floating label inside the field
        modifier = Modifier.fillMaxWidth(),
        singleLine = true                             // keeps it to one line
    )

    Spacer(modifier = Modifier.height(8.dp))

    // save and cancel buttons side by side
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // adds space between the two buttons
    ) {
        OutlinedButton(
            onClick = onCancel,                       // closes the form without saving
            modifier = Modifier.weight(1f)            // takes up half the available width
        ) {
            Text("Cancel")                            // cancel button label
        }
        Button(
            onClick = {
                val parsed = amount.toDoubleOrNull()  // tries to parse the typed amount as a number
                if (parsed != null && parsed > 0 && currency.isNotEmpty()) { // only saves if amount and currency are valid
                    onSave(parsed, comment, currency)  // calls the save callback with the amount, comment and currency
                }
            },
            modifier = Modifier.weight(1f)            // takes up the other half of the available width
        ) {
            Text("Save")                              // save button label
        }
    }
}

// shows the last 5 personal expenses as a compact, read-only mini feed with a "View All" button
@Composable
private fun RecentTransactionsCard(
    expenses: List<ExpenseResponse>,              // the full personal expense list — only the last 5 are shown
    onTransactionClick: (ExpenseResponse) -> Unit, // called when the user taps a row — navigates to the detail screen
    onViewAllClick: () -> Unit                    // called when the user taps "View All" — opens AllExpensesScreen
) {
    val recent = expenses
        .sortedByDescending { it.date }            // sorts newest first — lexicographic sort works correctly for "yyyy-MM-dd" format
        .take(5)                                   // keeps only the 5 most recent entries

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // same elevation as the Safe-to-Spend card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold                        // bold card title
                )
                TextButton(onClick = onViewAllClick) {
                    Text("View All")                                    // tapping opens AllExpensesScreen
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (recent.isEmpty()) {
                Text(
                    text = "No expenses yet. Tap + to add one!",       // shown when the user has no expenses
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recent.forEach { expense ->                             // loops through each of the 5 most recent expenses
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTransactionClick(expense) } // tapping navigates to the detail screen
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.description,            // expense name e.g. "Groceries"
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${expense.category} · ${expense.date}", // e.g. "Food · 2025-04-16"
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${expense.currency} ${"%.2f".format(expense.amount)}", // e.g. "USD 24.00"
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary  // highlighted in theme color
                        )
                    }
                }
            }
        }
    }
}

// shows upcoming recurring expenses due in the next 7 days — tapping "Manage" opens the full management screen
@Composable
private fun RecurringExpensesCard(
    upcomingExpenses: List<RecurringExpenseResponse>, // the recurring expenses due in the next 7 days
    onViewAllClick: () -> Unit                        // called when the user taps "Manage" — opens RecurringExpensesScreen
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // same elevation as other HomeScreen cards
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔁 Recurring Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold                        // bold card title
                )
                TextButton(onClick = onViewAllClick) {
                    Text("Manage")                                      // tapping opens RecurringExpensesScreen
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Due in the next 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant     // muted subtitle
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (upcomingExpenses.isEmpty()) {
                Text(
                    text = "Nothing due in the next 7 days.",           // shown when no recurring expenses are upcoming
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                upcomingExpenses.forEach { expense ->                   // loops through each upcoming recurring expense
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expense.name,                        // e.g. "Netflix"
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${expense.currency} ${"%.2f".format(expense.amount)}", // e.g. "USD 9.99"
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary   // highlighted in theme color
                        )
                    }
                }
            }
        }
    }
}

// shows a horizontal bar chart of personal spending grouped by category for the current month
@Composable
private fun SpendingBreakdownCard(expenses: List<ExpenseResponse>) {  // receives the full personal expense list — filters to current month internally

    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) // parses date strings from the backend
    val today = java.util.Calendar.getInstance()                       // gets today's date
    val currentMonth = today.get(java.util.Calendar.MONTH)            // current month as 0-based integer
    val currentYear = today.get(java.util.Calendar.YEAR)              // current year e.g. 2025

    // keeps only expenses from the current calendar month
    val thisMonth = expenses.filter { expense ->
        try {
            val cal = java.util.Calendar.getInstance().apply {
                time = dateFormat.parse(expense.date)!!                // parses the expense date string into a Calendar
            }
            cal.get(java.util.Calendar.MONTH) == currentMonth         // must match current month
                    && cal.get(java.util.Calendar.YEAR) == currentYear // must match current year
        } catch (e: Exception) { false }                              // skips any expense with an unparseable date
    }

    if (thisMonth.isEmpty()) return                                    // hides the card entirely if no expenses this month

    // groups expenses by category and sums the amounts — sorted highest to lowest
    val categoryTotals = thisMonth
        .groupBy { it.category }                                       // groups expenses into buckets by category name
        .mapValues { (_, list) -> list.sumOf { it.amount } }          // replaces each bucket with the total amount
        .entries.sortedByDescending { it.value }                      // shows the biggest spending categories first

    val maxAmount = categoryTotals.maxOf { it.value }                 // the largest category total — used to scale all bars proportionally

    // one color per category — cycles back to the start if there are more categories than colors
    val barColors = listOf(
        Color(0xFF1E88E5), // blue
        Color(0xFF43A047), // green
        Color(0xFFFFA726), // orange
        Color(0xFFE53935), // red
        Color(0xFF8E24AA), // purple
        Color(0xFF00ACC1), // cyan
        Color(0xFFFFB300), // amber
        Color(0xFF6D4C41)  // brown
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // same elevation as other HomeScreen cards
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Spending Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold                           // bold card title
            )
            Text(
                text = "This month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant    // muted subtitle below the title
            )

            Spacer(modifier = Modifier.height(12.dp))

            categoryTotals.forEachIndexed { index, (category, amount) -> // loops through each category row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // spacing between label, bar, and amount
                ) {
                    Text(
                        text = category,                               // e.g. "Food"
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),             // fixed width so all bars start at the same x position
                        maxLines = 1                                   // keeps the label on one line
                    )
                    Canvas(
                        modifier = Modifier
                            .weight(1f)                                // takes up all remaining horizontal space
                            .height(14.dp)                            // bar height
                    ) {
                        val barWidth = (amount / maxAmount * size.width).toFloat() // scales bar width proportionally to the max category
                        drawRect(
                            color = barColors[index % barColors.size], // cycles through the color list
                            size = Size(barWidth, size.height)         // draws a filled rectangle for the bar
                        )
                    }
                    Text(
                        text = "%.0f".format(amount),                 // e.g. "142" — no decimals to keep the row compact
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface   // standard text color for the amount
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))              // breathing room between category rows
            }
        }
    }
}

// shows a single expense as a tappable card with description, category, date, type and amount
@Composable
private fun ExpenseCard(
    expense: ExpenseResponse,              // the expense to display
    exchangeRates: Map<String, Double>,    // the latest exchange rates fetched from the backend
    homeCurrency: String,                  // the user's home currency e.g. "USD"
    localCurrency: String,                 // the user's local currency e.g. "EUR"
    onClick: () -> Unit,                   // called when the card is tapped
    onDeleteClick: () -> Unit              // called when the delete button is tapped
) {
    // determines the expense type label based on its properties
    val expenseType = when {
        expense.groupId != null && !expense.isOneTimeSplit -> "Group"   // belongs to an existing group
        expense.isOneTimeSplit -> "One-time Split"                       // temporary split with others
        else -> "Solo"                                                   // personal expense
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),                                     // tapping the card opens the detail screen
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)       // subtle shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {

                // shows the expense description as the card title
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // shows category, date and expense type as a subtitle
                Text(
                    text = "${expense.category} · ${expense.date} · $expenseType",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant         // lighter text for the subtitle
                )
            }

            Column(horizontalAlignment = Alignment.End) {

                // shows the amount and currency highlighted in the primary color
                Text(
                    text = "${expense.currency} ${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary                  // highlights the amount
                )

                // shows the converted amount in the local currency if it differs from the expense currency
                val shouldConvert = expense.currency != localCurrency          // only convert if currencies are different
                if (shouldConvert && exchangeRates.isNotEmpty()) {
                    val rateToHome = exchangeRates[expense.currency]           // rate from expense currency to home currency
                    val rateToLocal = exchangeRates[localCurrency]             // rate from home currency to local currency
                    if (rateToHome != null && rateToLocal != null) {
                        val converted = expense.amount * rateToHome * rateToLocal // converts via home currency as the bridge
                        Text(
                            text = "≈ $localCurrency ${String.format("%.2f", converted)}", // shows the converted amount
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // muted text to distinguish from the main amount
                        )
                    }
                }

                // delete button with a trash emoji icon
                androidx.compose.material3.IconButton(onClick = onDeleteClick) {
                    Text("🗑️")                                                 // trash icon
                }
            }
        }
    }
}