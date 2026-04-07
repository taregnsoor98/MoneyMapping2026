package com.example.moneymapping.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.moneymapping.network.ExpenseResponse
import com.example.moneymapping.ui.navigation.Screen

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(navController: NavController) { // receives navController to handle navigation

    val viewModel: HomeViewModel = viewModel()                    // creates the HomeViewModel
    val expensesState by viewModel.expensesState.collectAsState() // observes the expenses state
    val navBackStackEntry by navController.currentBackStackEntryAsState() // observes navigation changes

    // Re-fetches expenses every time we return to this screen
    LaunchedEffect(navBackStackEntry) {
        viewModel.fetchExpenses()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddExpense.route) } // opens Add Expense screen
            ) {
                Text("+") // plus icon on the FAB
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
                fontWeight = FontWeight.Bold // bold page title
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = expensesState) {
                is ExpensesState.Loading -> LoadingContent()   // shows spinner while loading
                is ExpensesState.Error -> ErrorContent(state.message) // shows error message
                is ExpensesState.Success -> ExpenseListContent(
                    expenses = state.expenses,
                    onExpenseClick = { expense ->
                        // navigates to the edit screen with the expense ID
                        navController.navigate(
                            Screen.DetailExpense.route.replace("{expenseId}", expense.id)
                        )
                    },
                    onDeleteClick = { expense -> viewModel.deleteExpense(expense.id) }
                )
            }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

// Shows a centered loading spinner
@Composable
private fun LoadingContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
    }
}

// Shows a centered error message
@Composable
private fun ErrorContent(message: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error // red error text
        )
    }
}

// Shows the list of expenses or an empty state message
@Composable
private fun ExpenseListContent(
    expenses: List<ExpenseResponse>,              // the list of expenses to display
    onExpenseClick: (ExpenseResponse) -> Unit,    // called when user taps an expense card
    onDeleteClick: (ExpenseResponse) -> Unit      // called when user taps the delete button
) {
    if (expenses.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "No expenses yet. Tap + to add one!",
                color = MaterialTheme.colorScheme.onSurfaceVariant // lighter text
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between cards
        ) {
            items(expenses) { expense ->
                ExpenseCard(
                    expense = expense,
                    onClick = { onExpenseClick(expense) },
                    onDeleteClick = { onDeleteClick(expense) }
                )
            }
        }
    }
}

// Shows a single expense as a tappable card with description, category, date, type and amount
@Composable
private fun ExpenseCard(
    expense: ExpenseResponse,  // the expense to display
    onClick: () -> Unit,       // called when the card is tapped
    onDeleteClick: () -> Unit  // called when the delete button is tapped
) {
    // Determines the expense type label based on its properties
    val expenseType = when {
        expense.groupId != null && !expense.isOneTimeSplit -> "Group" // belongs to an existing group
        expense.isOneTimeSplit -> "One-time Split"                     // temporary split with others
        else -> "Solo"                                                 // personal expense
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // tapping the card opens the edit screen
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

                // Shows the expense description
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Shows category, date and expense type
                Text(
                    text = "${expense.category} · ${expense.date} · $expenseType",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // lighter text
                )
            }

            Column(horizontalAlignment = Alignment.End) {

                // Shows the amount and currency
                Text(
                    text = "${expense.currency} ${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // highlights amount in primary color
                )

                // Delete button
                IconButton(onClick = onDeleteClick) {
                    Text("🗑️") // trash icon
                }
            }
        }
    }
}