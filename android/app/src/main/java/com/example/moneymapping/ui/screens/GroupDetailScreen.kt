package com.example.moneymapping.ui.screens

import android.app.Application                                     // needed to access context
import androidx.compose.foundation.clickable                       // makes a composable tappable
import androidx.compose.foundation.layout.Arrangement              // controls spacing between items
import androidx.compose.foundation.layout.Box                      // a container that stacks its children
import androidx.compose.foundation.layout.Column                   // arranges children vertically
import androidx.compose.foundation.layout.Row                      // arranges children horizontally
import androidx.compose.foundation.layout.Spacer                   // adds empty space
import androidx.compose.foundation.layout.fillMaxSize              // makes a composable fill all available space
import androidx.compose.foundation.layout.fillMaxWidth             // makes a composable fill the full width
import androidx.compose.foundation.layout.height                   // sets the height of a composable
import androidx.compose.foundation.layout.padding                  // adds padding around a composable
import androidx.compose.foundation.lazy.LazyColumn                 // a scrollable list
import androidx.compose.foundation.lazy.items                      // renders a list of items in LazyColumn
import androidx.compose.foundation.text.KeyboardOptions            // controls keyboard type
import androidx.compose.material.icons.Icons                       // provides built-in icons
import androidx.compose.material.icons.filled.Add                  // the + icon
import androidx.compose.material.icons.filled.ArrowBack            // back arrow icon
import androidx.compose.material3.AlertDialog                      // the popup dialog
import androidx.compose.material3.Button                           // a filled button
import androidx.compose.material3.Card                             // a card container
import androidx.compose.material3.CircularProgressIndicator        // a loading spinner
import androidx.compose.material3.ExperimentalMaterial3Api         // needed for some Material3 components
import androidx.compose.material3.FloatingActionButton             // the floating + button
import androidx.compose.material3.Icon                             // displays an icon
import androidx.compose.material3.IconButton                       // a button that only contains an icon
import androidx.compose.material3.MaterialTheme                    // provides theme colors and typography
import androidx.compose.material3.OutlinedTextField                // a text input field
import androidx.compose.material3.Scaffold                         // provides basic screen structure
import androidx.compose.material3.Text                             // displays text
import androidx.compose.material3.TextButton                       // a text-only button
import androidx.compose.material3.TopAppBar                        // the top bar with title and back button
import androidx.compose.runtime.Composable                         // marks a function as a composable
import androidx.compose.runtime.LaunchedEffect                     // runs a side effect when the composable is first shown
import androidx.compose.runtime.collectAsState                     // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                           // allows using 'by' delegation for state
import androidx.compose.runtime.mutableStateOf                     // creates a mutable state variable
import androidx.compose.runtime.remember                           // remembers state across recompositions
import androidx.compose.runtime.setValue                           // allows using 'by' delegation for mutable state
import androidx.compose.ui.Alignment                               // controls alignment of composables
import androidx.compose.ui.Modifier                                // used to style and layout composables
import androidx.compose.ui.platform.LocalContext                   // gets the current context
import androidx.compose.ui.text.input.KeyboardType                 // specifies number keyboard
import androidx.compose.ui.unit.dp                                 // density-independent pixels
import androidx.lifecycle.ViewModelProvider                        // creates ViewModels with parameters
import androidx.lifecycle.viewmodel.compose.viewModel              // creates a ViewModel in a composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,                                                 // the ID of the group to show
    onBack: () -> Unit,                                            // called when back button is pressed
    onAddExpense: (Long, List<String>) -> Unit,                    // called when + is tapped — passes group ID and member names
    onExpenseClick: (String) -> Unit,                              // called when an expense card is tapped — passes expense ID
    onManageLimits: (Long) -> Unit,                                // called when Manage Limits button is tapped
    onPaymentPlan: (Long, String, String, Double) -> Unit,         // called when Payment Plan button is tapped — passes groupId, fromUserId, toUserId, amount
    onViewPaymentPlan: (Long, Long) -> Unit                        // called when a payment plan card is tapped — passes planId and groupId
) {
    val context = LocalContext.current // gets the current context
    val viewModel: GroupDetailViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    ) // creates the ViewModel with application context

    val state by viewModel.state.collectAsState()                     // observes the current screen state
    val currentUserId by viewModel.currentUserId.collectAsState()     // observes the current user's ID
    val expenses by viewModel.expenses.collectAsState()               // observes the expenses list
    val expensesLoading by viewModel.expensesLoading.collectAsState() // observes the expenses loading state
    val limits by viewModel.limits.collectAsState()                   // observes the limits list
    val balances by viewModel.balances.collectAsState()               // observes the list of who owes whom
    val balancesLoading by viewModel.balancesLoading.collectAsState() // observes the balances loading state
    val paymentPlans by viewModel.paymentPlans.collectAsState()       // observes the list of payment plans for this group

    // tracks the balance that the "Paid" dialog is currently showing
    var activePayBalance by remember { mutableStateOf<com.example.moneymapping.network.BalanceResponse?>(null) }

    // tracks the amount the user typed in the "Paid" dialog
    var payAmountInput by remember { mutableStateOf("") }

    // loads the group when the screen first appears
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId) // fetches the group data and expenses
    }

    // shows the "Paid" dialog when a balance is active
    if (activePayBalance != null) {
        val balance = activePayBalance!! // safe to unwrap since we checked for null
        AlertDialog(
            onDismissRequest = {
                activePayBalance = null  // closes dialog on dismiss
                payAmountInput = ""     // clears the input field
            },
            title = {
                Text(text = "${balance.from} pays ${balance.to}") // dialog title showing who is paying whom
            },
            text = {
                Column {
                    Text(
                        text = "Total owed: ${"%.2f".format(balance.amount)}", // shows the total amount owed
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payAmountInput,
                        onValueChange = { payAmountInput = it },              // updates the input field
                        label = { Text("Amount being paid now") },            // label for the input field
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // shows number keyboard
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmountInput.toDoubleOrNull() ?: 0.0   // converts input to double safely
                        if (amount > 0.0) {                                   // only proceeds if amount is valid
                            viewModel.payDebtInstantly(
                                groupId = groupId,
                                fromUserId = balance.from,                    // the person who is paying
                                toUserId = balance.to,                        // the person being paid
                                amount = amount                               // the amount being settled now
                            )
                            activePayBalance = null                           // closes the dialog
                            payAmountInput = ""                               // clears the input field
                        }
                    },
                    enabled = payAmountInput.toDoubleOrNull() != null && (payAmountInput.toDoubleOrNull() ?: 0.0) > 0.0 // only enabled if valid amount
                ) {
                    Text("Confirm") // confirm button label
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        activePayBalance = null  // closes dialog without saving
                        payAmountInput = ""     // clears the input field
                    }
                ) {
                    Text("Cancel") // cancel button label
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state is GroupDetailState.Success)
                            (state as GroupDetailState.Success).group.name // shows group name
                        else "Group" // placeholder while loading
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { // goes back when back button is pressed
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back") // back arrow
                    }
                }
            )
        },
        floatingActionButton = {
            // only shows the + button if the group is loaded and not locked
            if (state is GroupDetailState.Success) {
                val group = (state as GroupDetailState.Success).group // gets the loaded group
                if (!group.isLocked) { // hides the + button for locked ONE_TIME groups
                    FloatingActionButton(
                        onClick = {
                            val memberNames = group.members.map {
                                it.guestName ?: it.username ?: "Unknown" // gets display name for each member
                            }
                            onAddExpense(groupId, memberNames) // navigates to add expense with group context
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Expense") // + icon
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // respects the scaffold's padding
        ) {
            when (state) {

                // shows a loading spinner while fetching the group
                is GroupDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // centered spinner
                }

                // shows an error message if something went wrong
                is GroupDetailState.Error -> {
                    Text(
                        text = (state as GroupDetailState.Error).message, // the error message
                        color = MaterialTheme.colorScheme.error,          // red error color
                        modifier = Modifier.align(Alignment.Center)       // centered on screen
                    )
                }

                // shows the group details when loaded successfully
                is GroupDetailState.Success -> {
                    val group = (state as GroupDetailState.Success).group // gets the loaded group

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),                              // padding around the list
                        verticalArrangement = Arrangement.spacedBy(12.dp) // space between items
                    ) {

                        // group info card
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = group.type,                                     // group type
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (group.isLocked) {                                      // shows locked badge
                                            Text(
                                                text = "Settled & Closed",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${group.members.size} member(s)",                  // shows member count
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // members section title
                        item {
                            Text(
                                text = "Members",                                                  // section title
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // list of members
                        items(group.members) { member ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = member.guestName ?: member.username ?: "Unknown", // shows guest name, or username, or Unknown
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = member.role,                                    // shows role
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // shows "You" badge for the current user
                                    if (member.userId == currentUserId) {
                                        Text(
                                            text = "You",                                          // marks the current user
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // limits section — only shown for FRIEND and FAMILY groups
                        if (group.type != "ONE_TIME") {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Limits",                                               // section title
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            if (limits.isEmpty()) {
                                item {
                                    Text(
                                        text = "No limits set yet.",                               // placeholder if no limits
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(limits) { limit ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (limit.userId == null) "Group Limit"
                                                    else if (limit.userId == currentUserId) "Your Limit"
                                                    else limit.userId,                             // other member's limit (admins only)
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "${limit.period}: ${String.format("%.2f", limit.amount)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            val targetUser = limit.userId ?: currentUserId
                                            val spent = if (limit.userId == null) {
                                                // group-wide limit — sums ALL expenses in the group
                                                expenses.sumOf { it.amount }
                                            } else {
                                                // per-member limit — sums only this member's item assignment shares
                                                expenses.sumOf { expense ->
                                                    expense.items.sumOf { item ->
                                                        item.assignments
                                                            .filter { it.personName == targetUser } // only this member's assignments
                                                            .sumOf { it.shareAmount }               // their share for this item
                                                    }
                                                }
                                            }
                                            val remaining = limit.amount - spent // calculates remaining
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Spent: ${String.format("%.2f", spent)} / Remaining: ${String.format("%.2f", remaining)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (remaining < 0) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // manage limits button
                            item {
                                Button(
                                    onClick = { onManageLimits(groupId) }, // navigates to manage limits screen
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Manage Limits") // button label
                                }
                            }
                        }

                        // who owes whom section — only shown for FRIEND and ONE_TIME groups (not FAMILY)
                        if (group.type != "FAMILY") {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Who Owes Whom",                                        // section title
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            if (balancesLoading) {
                                item {
                                    CircularProgressIndicator(modifier = Modifier.padding(8.dp)) // spinner while loading balances
                                }
                            } else if (balances.isEmpty()) {
                                item {
                                    Text(
                                        text = "All settled up!",                                  // shown when no debts exist
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(balances.filter { balance ->
                                    paymentPlans.none { plan -> plan.fromUserId == balance.from && plan.toUserId == balance.to && plan.totalAmount == balance.amount && plan.status == "ACTIVE" } // hides only the exact balance that has an active payment plan matching from, to, and amount
                                }) { balance ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "${balance.from} owes ${balance.to} ${"%.2f".format(balance.amount)}", // debt summary
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        activePayBalance = balance // opens the pay dialog for this balance
                                                        payAmountInput = "%.2f".format(balance.amount) // pre-fills with the full amount
                                                    },
                                                    modifier = Modifier.weight(1f) // takes half the row width
                                                ) {
                                                    Text("Paid") // button label
                                                }
                                                Button(
                                                    onClick = {
                                                        onPaymentPlan(groupId, balance.from, balance.to, balance.amount) // opens payment plan setup
                                                    },
                                                    modifier = Modifier.weight(1f) // takes half the row width
                                                ) {
                                                    Text("Payment Plan") // button label
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // payment plans section — only shown if there are active payment plans
                        if (paymentPlans.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Payment Plans",                                        // section title
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            items(paymentPlans) { plan ->
                                val paidAmount = plan.installments.filter { it.isPaid }.sumOf { it.amount }  // sum of all paid installments
                                val remainingAmount = plan.totalAmount - paidAmount                           // what is still left to pay
                                val installmentsLeft = plan.installments.count { !it.isPaid }                // how many installments are not yet paid

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onViewPaymentPlan(plan.id, plan.groupId) } // opens tracking screen when tapped
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${plan.fromUserId} owes ${plan.toUserId}",    // who owes whom
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Total: ${"%.2f".format(plan.totalAmount)} — Paid: ${"%.2f".format(paidAmount)} — Left: ${"%.2f".format(remainingAmount)}", // payment summary
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$installmentsLeft installment(s) remaining",  // how many payments are left
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (installmentsLeft == 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // expenses section title
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Expenses",                                                 // section title
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // shows loading spinner while fetching expenses
                        if (expensesLoading) {
                            item {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp)) // spinner while loading expenses
                            }
                        } else if (expenses.isEmpty()) {
                            item {
                                Text(
                                    text = "No expenses yet. Tap + to add one!", // placeholder text
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(expenses) { expense ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onExpenseClick(expense.id) } // navigates to expense detail on tap
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = expense.description,                        // expense description
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${expense.currency} ${String.format("%.2f", expense.amount)}", // amount with currency
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = expense.category,                           // expense category
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = expense.date,                               // expense date
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
        }
    }
}