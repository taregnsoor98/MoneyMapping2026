package com.example.moneymapping.ui.screens

import android.app.Application                                      // needed to access context
import android.app.DatePickerDialog                                 // Android's built-in date picker dialog
import androidx.compose.foundation.layout.Arrangement              // controls spacing between items
import androidx.compose.foundation.layout.Column                   // arranges children vertically
import androidx.compose.foundation.layout.Spacer                   // adds empty space
import androidx.compose.foundation.layout.fillMaxSize              // makes a composable fill all available space
import androidx.compose.foundation.layout.fillMaxWidth             // makes a composable fill the full width
import androidx.compose.foundation.layout.height                   // sets the height of a composable
import androidx.compose.foundation.layout.padding                  // adds padding around a composable
import androidx.compose.foundation.rememberScrollState             // remembers the scroll state
import androidx.compose.foundation.verticalScroll                  // makes a column scrollable
import androidx.compose.material.icons.Icons                       // provides built-in icons
import androidx.compose.material.icons.filled.ArrowBack            // back arrow icon
import androidx.compose.material3.Button                           // a filled button
import androidx.compose.material3.CircularProgressIndicator        // a loading spinner
import androidx.compose.material3.DropdownMenuItem                 // a single item in a dropdown menu
import androidx.compose.material3.ExperimentalMaterial3Api         // needed for some Material3 components
import androidx.compose.material3.ExposedDropdownMenuBox           // the dropdown menu container
import androidx.compose.material3.ExposedDropdownMenuDefaults      // provides default styles for the dropdown
import androidx.compose.material3.Icon                             // displays an icon
import androidx.compose.material3.IconButton                       // a button that only contains an icon
import androidx.compose.material3.MaterialTheme                    // provides theme colors and typography
import androidx.compose.material3.OutlinedButton                   // an outlined button — used for date pickers
import androidx.compose.material3.OutlinedTextField                // a text input field with an outline
import androidx.compose.material3.Scaffold                         // provides basic screen structure
import androidx.compose.material3.Text                             // displays text
import androidx.compose.material3.TopAppBar                        // the top bar with title and back button
import androidx.compose.runtime.Composable                         // marks a function as a composable
import androidx.compose.runtime.collectAsState                     // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                           // allows using 'by' delegation for state
import androidx.compose.runtime.mutableStateOf                     // creates mutable state
import androidx.compose.runtime.remember                           // remembers state across recompositions
import androidx.compose.runtime.setValue                           // allows setting state with 'by' delegation
import androidx.compose.ui.Modifier                                // used to style and layout composables
import androidx.compose.ui.platform.LocalContext                   // gets the current context
import androidx.compose.ui.unit.dp                                 // density-independent pixels
import androidx.lifecycle.ViewModelProvider                        // creates ViewModels with parameters
import androidx.lifecycle.viewmodel.compose.viewModel              // creates a ViewModel in a composable
import java.util.Calendar                                          // used to get the current date for the date picker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPlanSetupScreen(
    groupId: Long,                      // the group this payment plan belongs to
    fromUserId: String,                 // the person who owes money
    toUserId: String,                   // the person who is owed money
    totalAmount: Double,                // the total debt amount
    onBack: () -> Unit,                 // called when back button is pressed
    onPlanCreated: (Long, Long) -> Unit  // called with the new plan ID and groupId when the plan is created successfully
) {
    val context = LocalContext.current // gets the current context
    val viewModel: PaymentPlanViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    ) // creates the ViewModel with application context

    val isLoading by viewModel.isLoading.collectAsState() // observes the loading state
    val error by viewModel.error.collectAsState()         // observes the error state

    // local state for the form inputs
    var startDate by remember { mutableStateOf("") }            // holds the selected start date as "yyyy-MM-dd"
    var endDate by remember { mutableStateOf("") }              // holds the selected end date as "yyyy-MM-dd"
    var frequency by remember { mutableStateOf("MONTHLY") }     // holds the selected frequency — defaults to MONTHLY
    var frequencyExpanded by remember { mutableStateOf(false) } // controls whether the dropdown is open

    // the available frequency options
    val frequencyOptions = listOf("DAILY", "WEEKLY", "MONTHLY") // the three frequency choices

    // gets today's date to use as the default starting point for the date pickers
    val calendar = Calendar.getInstance() // gets the current date and time
    val currentYear = calendar.get(Calendar.YEAR)   // current year
    val currentMonth = calendar.get(Calendar.MONTH) // current month — 0-indexed
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH) // current day

    // creates the start date picker dialog — shown when the user taps the start date button
    val startDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            // formats the picked date as "yyyy-MM-dd" — month is 0-indexed so we add 1
            startDate = "%04d-%02d-%02d".format(year, month + 1, day)
        },
        currentYear, currentMonth, currentDay // opens showing today's date
    )

    // creates the end date picker dialog — shown when the user taps the end date button
    val endDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            // formats the picked date as "yyyy-MM-dd" — month is 0-indexed so we add 1
            endDate = "%04d-%02d-%02d".format(year, month + 1, day)
        },
        currentYear, currentMonth, currentDay // opens showing today's date
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Up Payment Plan") }, // screen title
                navigationIcon = {
                    IconButton(onClick = onBack) { // goes back when back button is pressed
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back") // back arrow
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)                          // adds padding around the content
                .verticalScroll(rememberScrollState()),  // makes the screen scrollable
            verticalArrangement = Arrangement.spacedBy(16.dp) // space between items
        ) {

            // shows the debt summary at the top so the user knows what they are setting up
            Text(
                text = "$fromUserId owes $toUserId ${"%.2f".format(totalAmount)}", // debt summary
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // start date picker button — shows the selected date or a placeholder
            OutlinedButton(
                onClick = { startDatePicker.show() }, // opens the date picker dialog
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (startDate.isBlank()) "Pick Start Date" else "Start Date: $startDate" // shows selected date or placeholder
                )
            }

            // end date picker button — shows the selected date or a placeholder
            OutlinedButton(
                onClick = { endDatePicker.show() }, // opens the date picker dialog
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (endDate.isBlank()) "Pick End Date" else "End Date: $endDate" // shows selected date or placeholder
                )
            }

            // frequency dropdown — lets the user pick DAILY, WEEKLY, or MONTHLY
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,                               // whether the dropdown is open
                onExpandedChange = { frequencyExpanded = !frequencyExpanded } // toggles the dropdown
            ) {
                OutlinedTextField(
                    value = frequency,                                      // shows the selected frequency
                    onValueChange = {},                                     // read-only — user picks from dropdown
                    readOnly = true,                                        // prevents manual typing
                    label = { Text("Frequency") },                         // field label
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) }, // dropdown arrow icon
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()                                       // anchors the dropdown to this field
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,                           // whether the dropdown is open
                    onDismissRequest = { frequencyExpanded = false }        // closes the dropdown when dismissed
                ) {
                    frequencyOptions.forEach { option ->                    // renders each frequency option
                        DropdownMenuItem(
                            text = { Text(option) },                        // the option label
                            onClick = {
                                frequency = option                          // updates the selected frequency
                                frequencyExpanded = false                   // closes the dropdown
                            }
                        )
                    }
                }
            }

            // calculates and shows the estimated installment amount based on current inputs
            if (startDate.isNotBlank() && endDate.isNotBlank()) {
                val estimatedCount = calculateInstallmentCount(startDate, endDate, frequency) // calculates count
                if (estimatedCount > 0) {
                    val estimatedAmount = totalAmount / estimatedCount // calculates amount per installment
                    Text(
                        text = "Estimated: $estimatedCount payments of ${"%.2f".format(estimatedAmount)}", // shows estimate
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // shows error message if something went wrong
            if (error != null) {
                Text(
                    text = error!!,                            // the error message
                    color = MaterialTheme.colorScheme.error,  // red error color
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // create plan button — disabled while loading or if dates are not picked
            Button(
                onClick = {
                    viewModel.createPaymentPlan(  // calls the ViewModel to create the plan
                        groupId = groupId,
                        fromUserId = fromUserId,
                        toUserId = toUserId,
                        totalAmount = totalAmount,
                        startDate = startDate,
                        endDate = endDate,
                        frequency = frequency,
                        onSuccess = { planId -> onPlanCreated(planId, groupId) } // navigates to tracking screen on success — passes both planId and groupId
                    )
                },
                enabled = !isLoading && startDate.isNotBlank() && endDate.isNotBlank(), // only enabled when dates are picked
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator() // shows spinner while creating
                } else {
                    Text("Create Payment Plan") // button label
                }
            }
        }
    }
}

// calculates how many installments there will be based on start date, end date and frequency
// returns 0 if the dates are invalid or end is before start
private fun calculateInstallmentCount(startDate: String, endDate: String, frequency: String): Int {
    return try {
        val parts1 = startDate.split("-")  // splits "yyyy-MM-dd" into parts
        val parts2 = endDate.split("-")    // splits "yyyy-MM-dd" into parts

        val start = Calendar.getInstance().apply {
            set(parts1[0].toInt(), parts1[1].toInt() - 1, parts1[2].toInt()) // sets year, month (0-indexed), day
        }
        val end = Calendar.getInstance().apply {
            set(parts2[0].toInt(), parts2[1].toInt() - 1, parts2[2].toInt()) // sets year, month (0-indexed), day
        }

        if (end.before(start)) return 0 // returns 0 if end is before start

        when (frequency) {
            "DAILY" -> ((end.timeInMillis - start.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1   // counts days
            "WEEKLY" -> ((end.timeInMillis - start.timeInMillis) / (1000 * 60 * 60 * 24 * 7)).toInt() + 1 // counts weeks
            "MONTHLY" -> {
                val months = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 +
                        (end.get(Calendar.MONTH) - start.get(Calendar.MONTH)) // counts months
                months + 1
            }
            else -> 0 // unknown frequency
        }
    } catch (e: Exception) {
        0 // returns 0 if dates cannot be parsed
    }
}