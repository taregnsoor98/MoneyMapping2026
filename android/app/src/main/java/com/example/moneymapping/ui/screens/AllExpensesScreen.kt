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
import androidx.compose.material3.Card                                // a card container with elevation and rounded corners
import androidx.compose.material3.CardDefaults                        // provides default card styling like elevation
import androidx.compose.material3.DropdownMenuItem                    // a single item inside a dropdown menu
import androidx.compose.material3.ExperimentalMaterial3Api            // needed for some Material3 components still in experimental state
import androidx.compose.material3.ExposedDropdownMenuBox              // a dropdown menu anchored to a text field
import androidx.compose.material3.ExposedDropdownMenuDefaults         // provides default trailing icon for the dropdown
import androidx.compose.material3.IconButton                          // a button that wraps an icon
import androidx.compose.material3.MaterialTheme                       // provides theme colors and typography
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
import androidx.compose.ui.text.input.KeyboardType                    // sets the keyboard to number mode for amount input
import androidx.compose.ui.unit.dp                                    // density-independent pixels for sizing
import androidx.lifecycle.viewmodel.compose.viewModel                 // creates a ViewModel scoped to this composable
import androidx.navigation.NavController                              // handles navigation between screens
import com.example.moneymapping.network.ExpenseResponse              // the expense data model
import com.example.moneymapping.ui.navigation.Screen                 // the sealed class that defines all navigation routes

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExpensesScreen(navController: NavController) {               // receives navController to handle navigation back and to detail screen

    val viewModel: HomeViewModel = viewModel()                       // reuses HomeViewModel — expenses are already loaded, no extra API calls needed
    val expensesState by viewModel.expensesState.collectAsState()    // observes the expenses state from the shared ViewModel

    // ── Filter state ──────────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }              // holds the text the user types in the search bar
    var selectedCategory by remember { mutableStateOf("All") }      // holds the selected category filter — "All" means no filter
    var sortOption by remember { mutableStateOf("Newest") }         // holds the selected sort option
    var categoryExpanded by remember { mutableStateOf(false) }      // tracks whether the category dropdown is open
    var sortExpanded by remember { mutableStateOf(false) }          // tracks whether the sort dropdown is open

    val categories = listOf(                                         // all available category filter options
        "All", "Food", "Transport", "Housing", "Entertainment",
        "Health", "Shopping", "Education", "Travel", "Other"
    )

    val sortOptions = listOf("Newest", "Oldest", "Highest Amount", "Lowest Amount") // all available sort options

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // ── Header row with title and back button ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "All Expenses",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold                      // bold page title
                )
                TextButton(onClick = { navController.popBackStack() }) { // navigates back to the HomeScreen
                    Text("Back")                                      // back button label
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Search bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,                                  // shows the currently typed search text
                onValueChange = { searchQuery = it },                 // updates the search query as the user types
                label = { Text("Search by name or amount") },        // floating label inside the field
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,                                    // keeps the search bar to one line
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text) // standard text keyboard
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Filter row — category and sort dropdowns side by side ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)   // adds space between the two dropdowns
            ) {

                // category filter dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }, // toggles the dropdown on tap
                    modifier = Modifier.weight(1f)                    // takes up half the row width
                ) {
                    OutlinedTextField(
                        value = selectedCategory,                     // shows the currently selected category
                        onValueChange = {},                           // read-only — user can only pick from the dropdown
                        readOnly = true,                              // prevents keyboard from appearing
                        label = { Text("Category") },                 // floating label inside the field
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) // chevron icon
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()                             // anchors the dropdown to this text field
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false } // closes the dropdown if the user taps outside
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },                 // shows the category name
                                onClick = {
                                    selectedCategory = cat            // updates the selected category
                                    categoryExpanded = false          // closes the dropdown after selection
                                }
                            )
                        }
                    }
                }

                // sort dropdown
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = !sortExpanded }, // toggles the dropdown on tap
                    modifier = Modifier.weight(1f)                    // takes up the other half of the row width
                ) {
                    OutlinedTextField(
                        value = sortOption,                           // shows the currently selected sort option
                        onValueChange = {},                           // read-only — user can only pick from the dropdown
                        readOnly = true,                              // prevents keyboard from appearing
                        label = { Text("Sort by") },                  // floating label inside the field
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) // chevron icon
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()                             // anchors the dropdown to this text field
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }   // closes the dropdown if the user taps outside
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },              // shows the sort option name
                                onClick = {
                                    sortOption = option               // updates the selected sort option
                                    sortExpanded = false              // closes the dropdown after selection
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Expense list ──────────────────────────────────────────────────
            when (val state = expensesState) {

                is ExpensesState.Loading -> {
                    Text(                                             // simple loading message — no spinner needed since data is usually already loaded
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is ExpensesState.Error -> {
                    Text(
                        text = state.message,                         // shows the error message from the ViewModel
                        color = MaterialTheme.colorScheme.error,      // red error text
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is ExpensesState.Success -> {

                    // applies search, category filter, and sort to the full expense list
                    val filtered = state.expenses
                        .filter { expense ->                          // keeps only expenses matching the search query
                            val query = searchQuery.trim().lowercase()
                            if (query.isEmpty()) true                 // no query — include everything
                            else expense.description.lowercase().contains(query) // matches by description name
                                    || expense.amount.toString().contains(query)     // matches by amount e.g. typing "24"
                        }
                        .filter { expense ->                          // keeps only expenses matching the selected category
                            selectedCategory == "All" || expense.category == selectedCategory
                        }
                        .let { list ->                               // sorts the filtered list based on the selected sort option
                            when (sortOption) {
                                "Newest"         -> list.sortedByDescending { it.date }   // newest date first
                                "Oldest"         -> list.sortedBy { it.date }             // oldest date first
                                "Highest Amount" -> list.sortedByDescending { it.amount } // largest amount first
                                "Lowest Amount"  -> list.sortedBy { it.amount }           // smallest amount first
                                else             -> list                                  // fallback — no sort applied
                            }
                        }

                    if (filtered.isEmpty()) {
                        Text(
                            text = "No expenses match your filters.",  // shown when the filter returns no results
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between expense cards
                        ) {
                            items(filtered) { expense ->
                                AllExpenseCard(
                                    expense = expense,
                                    onClick = {
                                        navController.navigate(       // navigates to the existing detail screen
                                            Screen.DetailExpense.route.replace("{expenseId}", expense.id)
                                        )
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteExpense(expense.id) // deletes the expense and refreshes the list
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

// ─── Expense card ─────────────────────────────────────────────────────────────

// shows a single expense row — tapping opens the detail/edit screen
@Composable
private fun AllExpenseCard(
    expense: ExpenseResponse,        // the expense to display
    onClick: () -> Unit,             // called when the card is tapped — opens the detail screen
    onDeleteClick: () -> Unit        // called when the delete button is tapped
) {
    val expenseType = when {
        expense.groupId != null && !expense.isOneTimeSplit -> "Group"  // belongs to an existing group
        expense.isOneTimeSplit -> "One-time Split"                      // temporary split with others
        else -> "Solo"                                                  // personal expense
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),                              // tapping the card opens the detail screen
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
                    text = expense.description,                         // expense name e.g. "Groceries"
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${expense.category} · ${expense.date} · $expenseType", // e.g. "Food · 2025-04-16 · Solo"
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant  // muted grey subtitle
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${expense.currency} ${"%.2f".format(expense.amount)}", // e.g. "USD 24.00"
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary            // highlighted in theme color
                )
                IconButton(onClick = onDeleteClick) {
                    Text("🗑️")                                          // trash icon for delete
                }
            }
        }
    }
}