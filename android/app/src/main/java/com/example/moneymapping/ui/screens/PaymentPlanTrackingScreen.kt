package com.example.moneymapping.ui.screens

import android.app.Application                                      // needed to access context
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
import androidx.compose.material.icons.Icons                       // provides built-in icons
import androidx.compose.material.icons.filled.ArrowBack            // back arrow icon
import androidx.compose.material3.Button                           // a filled button
import androidx.compose.material3.Card                             // a card container
import androidx.compose.material3.CircularProgressIndicator        // a loading spinner
import androidx.compose.material3.ExperimentalMaterial3Api         // needed for some Material3 components
import androidx.compose.material3.Icon                             // displays an icon
import androidx.compose.material3.IconButton                       // a button that only contains an icon
import androidx.compose.material3.MaterialTheme                    // provides theme colors and typography
import androidx.compose.material3.Scaffold                         // provides basic screen structure
import androidx.compose.material3.Text                             // displays text
import androidx.compose.material3.TopAppBar                        // the top bar with title and back button
import androidx.compose.runtime.Composable                         // marks a function as a composable
import androidx.compose.runtime.LaunchedEffect                     // runs a side effect when the composable is first shown
import androidx.compose.runtime.collectAsState                     // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                           // allows using 'by' delegation for state
import androidx.compose.ui.Alignment                               // controls alignment of composables
import androidx.compose.ui.Modifier                                // used to style and layout composables
import androidx.compose.ui.platform.LocalContext                   // gets the current context
import androidx.compose.ui.unit.dp                                 // density-independent pixels
import androidx.lifecycle.ViewModelProvider                        // creates ViewModels with parameters
import androidx.lifecycle.viewmodel.compose.viewModel              // creates a ViewModel in a composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPlanTrackingScreen(
    planId: Long,           // the ID of the payment plan to show
    groupId: Long,          // the group this payment plan belongs to — needed to load the plan correctly
    onBack: () -> Unit      // called when back button is pressed
) {
    val context = LocalContext.current // gets the current context
    val viewModel: PaymentPlanViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    ) // creates the ViewModel with application context

    val plan by viewModel.plan.collectAsState()           // observes the current payment plan
    val isLoading by viewModel.isLoading.collectAsState() // observes the loading state
    val error by viewModel.error.collectAsState()         // observes the error state

    // loads the plan when the screen first appears
    // note: we need the groupId to fetch plans — we get it from the plan itself once loaded
    // for the initial load we pass planId and rely on the ViewModel to find it across all plans
    LaunchedEffect(planId) {
        // we load by planId — the ViewModel will search across the group's plans to find it
        // groupId is passed as 0 initially and the plan is found by planId match
        viewModel.loadPlan(planId, groupId) // loads the plan using the correct groupId passed from the setup screen
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Plan") }, // screen title
                navigationIcon = {
                    IconButton(onClick = onBack) { // goes back when back button is pressed
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back") // back arrow
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // shows a loading spinner while fetching the plan
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // centered spinner
                }

                // shows an error message if something went wrong
                error != null -> {
                    Text(
                        text = error!!,                            // the error message
                        color = MaterialTheme.colorScheme.error,  // red error color
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // shows the plan details when loaded successfully
                plan != null -> {
                    val currentPlan = plan!! // safe to unwrap since we checked for null

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),                              // padding around the list
                        verticalArrangement = Arrangement.spacedBy(12.dp) // space between items
                    ) {

                        // plan summary card at the top
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${currentPlan.fromUserId} owes ${currentPlan.toUserId}", // who owes whom
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total: ${"%.2f".format(currentPlan.totalAmount)}", // total debt amount
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${currentPlan.frequency} — ${"%.2f".format(currentPlan.installmentAmount)} per installment", // frequency and amount per installment
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${currentPlan.startDate} → ${currentPlan.endDate}", // date range
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${currentPlan.status}", // ACTIVE or COMPLETED
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (currentPlan.status == "COMPLETED")
                                            MaterialTheme.colorScheme.primary  // green-ish for completed
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant // grey for active
                                    )
                                }
                            }
                        }

                        // back to group button — takes the user back to the group screen
                        item {
                            Button(
                                onClick = onBack,              // navigates back to the group screen
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Group") // button label
                            }
                        }

                        // installments section title
                        item {
                            Text(
                                text = "Installments",                         // section title
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // renders each installment as a card
                        items(currentPlan.installments) { installment ->
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
                                            text = "Due: ${installment.dueDate}", // the due date
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${"%.2f".format(installment.amount)}", // the amount due
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // shows the paid date if this installment has been paid
                                        if (installment.isPaid && installment.paidDate != null) {
                                            Text(
                                                text = "Paid on ${installment.paidDate}", // when it was paid
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // shows "Paid" badge or a "Mark as Paid" button
                                    if (installment.isPaid) {
                                        Text(
                                            text = "✓ Paid",                              // paid badge
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.payInstallment( // marks this installment as paid
                                                    planId = currentPlan.id,
                                                    installmentId = installment.id,
                                                    groupId = currentPlan.groupId
                                                )
                                            }
                                        ) {
                                            Text("Mark as Paid") // button label
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // shows a message if no plan was found
                else -> {
                    Text(
                        text = "Payment plan not found.",          // fallback message
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}