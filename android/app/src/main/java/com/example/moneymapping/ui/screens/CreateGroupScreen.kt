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
import androidx.compose.material.icons.filled.Close                // close/remove icon
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
import androidx.compose.material3.Switch                           // a toggle switch
import androidx.compose.material3.Text                             // displays text
import androidx.compose.material3.TopAppBar                        // the top bar with title and back button
import androidx.compose.runtime.Composable                         // marks a function as a composable
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
fun CreateGroupScreen(
    onGroupCreated: () -> Unit, // called when group is successfully created
    onBack: () -> Unit          // called when back button is pressed
) {
    val context = LocalContext.current // gets the current context
    val viewModel: CreateGroupViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    ) // creates the ViewModel with application context

    val currentStep by viewModel.currentStep.collectAsState()  // observes the current step
    val isLoading by viewModel.isLoading.collectAsState()      // observes the loading state
    val error by viewModel.error.collectAsState()              // observes any error message

    // step titles shown in the top bar
    val stepTitles = mapOf(
        1 to "Group Info",      // step 1 — name and type
        2 to "Add Members",     // step 2 — add members
        3 to "Set Limits"       // step 3 — set limits
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stepTitles[currentStep] ?: "") }, // shows current step title
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 1) onBack() // goes back out of wizard on first step
                        else viewModel.previousStep()  // goes to previous step otherwise
                    }) {
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
                .padding(16.dp) // inner padding for content
        ) {

            // shows error message if something went wrong
            if (error != null) {
                Text(
                    text = error!!,                          // the error message
                    color = MaterialTheme.colorScheme.error, // red error color
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // shows the correct step based on current step number
            when (currentStep) {
                1 -> CreateGroupStepOne(viewModel = viewModel)                                    // name and type
                2 -> CreateGroupStepTwo(viewModel = viewModel, onGroupCreated = onGroupCreated)   // add members
                3 -> CreateGroupStepThree(                                                        // set limits
                    viewModel = viewModel,
                    onGroupCreated = onGroupCreated
                )
            }
        }
    }
}

// Step 1 — group name and type
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupStepOne(viewModel: CreateGroupViewModel) {
    val groupName by viewModel.groupName.collectAsState()         // observes the typed group name
    val groupType by viewModel.groupType.collectAsState()         // observes the selected group type
    val groupCurrency by viewModel.groupCurrency.collectAsState() // observes the selected currency
    var typeExpanded by remember { mutableStateOf(false) }        // controls type dropdown visibility
    var currencyExpanded by remember { mutableStateOf(false) }    // controls currency dropdown visibility

    val groupTypes = listOf("FRIEND", "FAMILY", "ONE_TIME")       // available group types
    val currencies = listOf("USD", "EUR", "GBP", "AED", "SAR", "TRY", "AMD", "JPY", "CAD", "AUD") // available currencies

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)         // space between items
    ) {
        // group name input
        OutlinedTextField(
            value = groupName,                                     // current typed name
            onValueChange = { viewModel.setGroupName(it) },        // updates name in ViewModel
            label = { Text("Group Name") },                        // label for the field
            modifier = Modifier.fillMaxWidth()                     // takes full width
        )

        // group type dropdown
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }    // toggles dropdown
        ) {
            OutlinedTextField(
                value = groupType,                                 // shows selected type
                onValueChange = {},
                readOnly = true,                                   // user can only select, not type
                label = { Text("Group Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()                                  // anchors the dropdown to this field
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }        // closes dropdown on dismiss
            ) {
                groupTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },                     // shows the type name
                        onClick = {
                            viewModel.setGroupType(type)           // updates selected type in ViewModel
                            typeExpanded = false                   // closes dropdown
                        }
                    )
                }
            }
        }

        // currency dropdown
        ExposedDropdownMenuBox(
            expanded = currencyExpanded,
            onExpandedChange = { currencyExpanded = !currencyExpanded } // toggles dropdown
        ) {
            OutlinedTextField(
                value = groupCurrency,                             // shows selected currency
                onValueChange = {},
                readOnly = true,                                   // user can only select, not type
                label = { Text("Currency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()                                  // anchors the dropdown to this field
            )
            ExposedDropdownMenu(
                expanded = currencyExpanded,
                onDismissRequest = { currencyExpanded = false }    // closes dropdown on dismiss
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },                 // shows the currency code
                        onClick = {
                            viewModel.setGroupCurrency(currency)   // updates selected currency in ViewModel
                            currencyExpanded = false               // closes dropdown
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // pushes button to the bottom

        // next button — only enabled if name is not empty
        Button(
            onClick = { viewModel.nextStep() },                    // moves to step 2
            modifier = Modifier.fillMaxWidth(),
            enabled = groupName.isNotBlank()                       // only enabled if name is filled
        ) {
            Text("Next")
        }
    }
}
// Step 2 — add members
@Composable
fun CreateGroupStepTwo(
    viewModel: CreateGroupViewModel,
    onGroupCreated: () -> Unit          // called when ONE_TIME group is created and we navigate back
) {
    val members by viewModel.members.collectAsState()             // observes the list of added members
    val groupType by viewModel.groupType.collectAsState()         // observes the group type
    val searchResults by viewModel.searchResults.collectAsState() // observes search results
    val isSearching by viewModel.isSearching.collectAsState()     // observes search loading state

    var searchQuery by remember { mutableStateOf("") }            // holds the typed search query
    var guestName by remember { mutableStateOf("") }              // holds the typed guest name

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)         // space between items
    ) {

        // search for real users by username or email
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it                                   // updates query
                viewModel.searchUsers(it)                          // searches as user types
            },
            label = { Text("Search by username or email") },      // label for search field
            modifier = Modifier.fillMaxWidth()
        )

        // shows loading spinner while searching
        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        // shows search results
        searchResults.forEach { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(result.username) // shows the username
                    Button(onClick = {
                        viewModel.addMember(                       // adds the user as a member
                            userId = result.id,
                            username = result.username,
                            isGuest = false
                        )
                        searchQuery = ""                           // clears search field
                    }) {
                        Text("Add")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // add a guest by name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = guestName,
                onValueChange = { guestName = it },                // updates guest name
                label = { Text("Add guest by name") },             // label for guest field
                modifier = Modifier.weight(1f)                     // takes remaining width
            )
            Button(
                onClick = {
                    if (guestName.isNotBlank()) {
                        viewModel.addMember(                       // adds the guest as a member
                            userId = null,
                            username = guestName,
                            isGuest = true
                        )
                        guestName = ""                             // clears guest name field
                    }
                },
                enabled = guestName.isNotBlank()                   // only enabled if name is filled
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // shows the list of added members
        Text(
            text = "Members (${members.size}):",                   // shows count of added members
            style = MaterialTheme.typography.titleSmall
        )

        LazyColumn(
            modifier = Modifier.weight(1f),                        // takes remaining space
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(member.username)                  // shows member name
                            if (member.isGuest) {
                                Text(
                                    text = "Guest",                // shows guest badge
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // shows admin toggle only for Family groups
                            if (groupType == "FAMILY" && !member.isGuest) {
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    checked = member.isAdmin,           // current admin status
                                    onCheckedChange = {
                                        viewModel.toggleAdmin(member)   // toggles admin role
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // remove member button
                            IconButton(onClick = { viewModel.removeMember(member) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove") // X icon
                            }
                        }
                    }
                }
            }
        }

        // next/create button
        Button(
            onClick = {
                if (groupType == "ONE_TIME") {
                    viewModel.createGroup { onGroupCreated() }     // creates ONE_TIME group and navigates back
                } else {
                    viewModel.nextStep()                           // goes to limits step for FRIEND and FAMILY
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (groupType == "ONE_TIME") "Create Group" else "Next") // different label for ONE_TIME
        }
    }
}

// Step 3 — set limits (Friend and Family only)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupStepThree(
    viewModel: CreateGroupViewModel,
    onGroupCreated: () -> Unit // called when group is successfully created
) {
    val members by viewModel.members.collectAsState()                     // observes the list of members
    val groupType by viewModel.groupType.collectAsState()                 // observes the group type
    val groupLimitAmount by viewModel.groupLimitAmount.collectAsState()   // observes the group limit amount
    val groupLimitPeriod by viewModel.groupLimitPeriod.collectAsState()   // observes the group limit period
    val isLoading by viewModel.isLoading.collectAsState()                 // observes loading state

    var periodExpanded by remember { mutableStateOf(false) }              // controls period dropdown visibility
    val periods = listOf("DAILY", "WEEKLY", "MONTHLY")                    // available limit periods

    // calculates the total of all member limits combined
    val totalMemberLimits = members.sumOf { it.limitAmount.toDoubleOrNull() ?: 0.0 }

    // parses the group limit as a number — 0.0 if empty or invalid
    val groupLimit = groupLimitAmount.toDoubleOrNull() ?: 0.0

    // true if the total member limits exceed the group limit
    val limitsExceeded = totalMemberLimits > groupLimit && groupLimit > 0.0

    // true if the total member limits are exactly equal to the group limit
    val limitsBalanced = totalMemberLimits == groupLimit && groupLimit > 0.0

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Set Group Limit",                                     // section title
            style = MaterialTheme.typography.titleMedium
        )

        // group limit amount input
        OutlinedTextField(
            value = groupLimitAmount,
            onValueChange = { viewModel.setGroupLimitAmount(it) },        // updates amount in ViewModel
            label = { Text("Group Limit Amount") },                       // label for amount field
            modifier = Modifier.fillMaxWidth()
        )

        // period dropdown
        ExposedDropdownMenuBox(
            expanded = periodExpanded,
            onExpandedChange = { periodExpanded = !periodExpanded }
        ) {
            OutlinedTextField(
                value = groupLimitPeriod,                                 // shows selected period
                onValueChange = {},
                readOnly = true,                                          // user can only select, not type
                label = { Text("Period") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = periodExpanded,
                onDismissRequest = { periodExpanded = false }
            ) {
                periods.forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period) },                          // shows the period name
                        onClick = {
                            viewModel.setGroupLimitPeriod(period)         // updates period in ViewModel
                            periodExpanded = false                        // closes dropdown
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // shows per-member limit inputs
        Text(
            text = "Assign limits per member:",                           // section title
            style = MaterialTheme.typography.titleSmall
        )

        // shows how much of the group limit has been assigned so far
        Text(
            text = "Assigned: $totalMemberLimits / $groupLimit",         // shows assigned vs total
            style = MaterialTheme.typography.bodySmall,
            color = when {
                limitsExceeded -> MaterialTheme.colorScheme.error        // red if exceeded
                limitsBalanced -> MaterialTheme.colorScheme.primary      // green if balanced
                else -> MaterialTheme.colorScheme.onSurfaceVariant       // grey if still assigning
            }
        )

        // shows a warning if the total member limits exceed the group limit
        if (limitsExceeded) {
            Text(
                text = "Total member limits exceed the group limit!",    // warning message
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error                  // red warning text
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),                               // takes remaining space
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.username,                       // shows member name
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = member.limitAmount,                   // current limit for this member
                            onValueChange = {
                                viewModel.setMemberLimit(member, it)      // updates member limit in ViewModel
                            },
                            label = { Text("Limit") },                    // label for limit field
                            modifier = Modifier.width(120.dp)             // fixed width for limit input
                        )
                    }
                }
            }
        }

        // create group button
        Button(
            onClick = {
                viewModel.createGroup { onGroupCreated() }                // creates the group and navigates back
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && groupLimitAmount.isNotBlank() && !limitsExceeded // disabled if limits exceeded
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp)) // shows spinner while creating
            } else {
                Text("Create Group")                                      // normal button label
            }
        }
    }
}