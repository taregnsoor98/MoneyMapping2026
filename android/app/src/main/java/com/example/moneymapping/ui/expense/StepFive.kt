package com.example.moneymapping.ui.expense

// Step 5 of the Add Expense wizard
// Shows each item in the expense and lets the user assign people to each one independently.
// Each item has its own split mode — BY_QUANTITY or BY_PERCENTAGE.
// Any number of people can be assigned to any item regardless of quantity.
// When launched from inside a group, shows only the group members as a dropdown.
// When launched from the home screen, shows a search field and guest name field.
// Also includes a "Who Paid?" section where the user can add one or more payers with amounts.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymapping.network.ExpensePayerRequest

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StepFive(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    val items by viewModel.items.collectAsState()               // observes the current items list
    val searchState by viewModel.searchState.collectAsState()   // observes the current search state
    val isFromGroup by viewModel.isFromGroup.collectAsState()   // true if launched from inside a group
    val groupMembers by viewModel.groupMembers.collectAsState() // the group members if launched from a group
    val payers by viewModel.payers.collectAsState()             // observes the current payers list

    // tracks which item's search field is currently active
    var activeSearchIndex by remember { mutableStateOf(-1) }

    // tracks the search query per item index
    var searchQueryPerItem by remember { mutableStateOf(mapOf<Int, String>()) }

    // tracks guest name input per item index
    var guestNamePerItem by remember { mutableStateOf(mapOf<Int, String>()) }

    // tracks which item's group member dropdown is expanded
    var groupMemberDropdownIndex by remember { mutableStateOf(-1) }

    // tracks the payer name input field for adding a new payer
    var newPayerName by remember { mutableStateOf("") }

    // tracks the payer amount input field for adding a new payer
    var newPayerAmount by remember { mutableStateOf("") }

    // tracks whether the payer name dropdown is expanded — used when launched from a group
    var payerDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // allows scrolling if list is long
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between elements
    ) {

        Text(
            text = "Assign People to Items",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        Text(
            text = if (isFromGroup)
                "Select group members for each item and choose how to split." // group mode description
            else
                "Search for a user or add a guest for each item and choose how to split.", // normal mode description
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter description color
        )

        items.forEachIndexed { index, item ->

            // calculates how many units are already assigned across all people for this item
            val totalAssigned = item.assignedQuantities.values.sum()

            // anyone can be added — no limit on number of people per item
            val canAddMore = true

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // shows item name, quantity and total price
                    Text(
                        text = "${item.name} — qty: ${item.quantity} — ${String.format("%.2f", item.totalPrice)}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // split mode selector — BY_QUANTITY or BY_PERCENTAGE
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = item.splitMode == SplitMode.BY_QUANTITY, // active if quantity mode
                            onClick = {
                                viewModel.updateItem(index, item.copy(splitMode = SplitMode.BY_QUANTITY)) // switches to quantity mode
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("By Quantity") // label for quantity mode
                        }
                        SegmentedButton(
                            selected = item.splitMode == SplitMode.BY_PERCENTAGE, // active if percentage mode
                            onClick = {
                                viewModel.updateItem(index, item.copy(splitMode = SplitMode.BY_PERCENTAGE)) // switches to percentage mode
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("By %") // label for percentage mode
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // add people section
                    if (isFromGroup) {
                        // shows group member dropdown when launched from a group
                        val availableMembers = groupMembers.filter { it !in item.assignedTo } // filters out already assigned members

                        if (availableMembers.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = groupMemberDropdownIndex == index,
                                onExpandedChange = {
                                    groupMemberDropdownIndex = if (groupMemberDropdownIndex == index) -1 else index // toggles dropdown
                                }
                            ) {
                                OutlinedTextField(
                                    value = "Select a member",                  // placeholder text
                                    onValueChange = {},
                                    readOnly = true,                            // user can only select, not type
                                    label = { Text("Add member") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMemberDropdownIndex == index) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()                           // anchors the dropdown to this field
                                )
                                ExposedDropdownMenu(
                                    expanded = groupMemberDropdownIndex == index,
                                    onDismissRequest = { groupMemberDropdownIndex = -1 } // closes dropdown on dismiss
                                ) {
                                    availableMembers.forEach { memberName ->
                                        DropdownMenuItem(
                                            text = { Text(memberName) },        // shows the member name
                                            onClick = {
                                                val updatedAssigned = item.assignedTo + memberName // adds member to assigned list
                                                val updatedQuantities = item.assignedQuantities + (memberName to 1) // assigns 1 unit by default
                                                viewModel.updateItem(
                                                    index,
                                                    item.copy(
                                                        assignedTo = updatedAssigned,
                                                        assignedQuantities = updatedQuantities
                                                    )
                                                )
                                                groupMemberDropdownIndex = -1   // closes dropdown after selection
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // shows search field when launched from the home screen
                        ExposedDropdownMenuBox(
                            expanded = activeSearchIndex == index && searchState is SearchState.Results,
                            onExpandedChange = {}                               // controlled manually
                        ) {
                            OutlinedTextField(
                                value = searchQueryPerItem[index] ?: "",        // uses per-item query
                                onValueChange = { query ->
                                    searchQueryPerItem = searchQueryPerItem + (index to query) // stores query per item
                                    activeSearchIndex = index                   // marks this item as active
                                    if (query.length >= 2) {
                                        viewModel.searchUsers(query)            // searches if query is long enough
                                    } else {
                                        viewModel.resetSearch()                 // clears results if too short
                                    }
                                },
                                label = { Text("Search user by username or email") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),                              // anchors the dropdown to this field
                                trailingIcon = {
                                    if (activeSearchIndex == index && searchState is SearchState.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.padding(8.dp)) // spinner while searching
                                    }
                                }
                            )

                            // shows search results as dropdown for this item only
                            if (activeSearchIndex == index && searchState is SearchState.Results) {
                                ExposedDropdownMenu(
                                    expanded = true,
                                    onDismissRequest = {
                                        activeSearchIndex = -1                  // clears active search
                                        viewModel.resetSearch()                 // clears search results
                                    }
                                ) {
                                    (searchState as SearchState.Results).users.forEach { user ->
                                        DropdownMenuItem(
                                            text = { Text("${user.username} (${user.email})") }, // shows username and email
                                            onClick = {
                                                val updatedAssigned = item.assignedTo + user.username // adds user to assigned list
                                                val updatedQuantities = item.assignedQuantities + (user.username to 1) // assigns 1 unit by default
                                                viewModel.updateItem(
                                                    index,
                                                    item.copy(
                                                        assignedTo = updatedAssigned,
                                                        assignedQuantities = updatedQuantities
                                                    )
                                                )
                                                searchQueryPerItem = searchQueryPerItem + (index to "") // clears search field
                                                activeSearchIndex = -1          // clears active search
                                                viewModel.resetSearch()         // clears search results
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // guest name field — only shown in normal mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = guestNamePerItem[index] ?: "",          // uses per-item guest name
                                onValueChange = { name ->
                                    guestNamePerItem = guestNamePerItem + (index to name) // stores guest name per item
                                },
                                label = { Text("Or add guest by name") },       // label for guest field
                                modifier = Modifier.weight(1f)                  // takes remaining width
                            )
                            Button(
                                onClick = {
                                    val guestName = guestNamePerItem[index] ?: "" // gets the guest name
                                    if (guestName.isNotBlank()) {
                                        val updatedAssigned = item.assignedTo + guestName // adds guest to assigned list
                                        val updatedQuantities = item.assignedQuantities + (guestName to 1) // assigns 1 unit by default
                                        viewModel.updateItem(
                                            index,
                                            item.copy(
                                                assignedTo = updatedAssigned,
                                                assignedQuantities = updatedQuantities
                                            )
                                        )
                                        guestNamePerItem = guestNamePerItem + (index to "") // clears guest name field
                                    }
                                }
                            ) {
                                Text("+") // plus button to add guest
                            }
                        }
                    }

                    // shows assigned people section if anyone is assigned
                    if (item.assignedTo.isNotEmpty()) {

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Assigned to:",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        item.assignedTo.forEach { personName ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // person name chip — tap to remove
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        val updatedAssigned = item.assignedTo - personName               // removes from assigned list
                                        val updatedQuantities = item.assignedQuantities - personName      // removes their quantity
                                        val updatedPercentages = item.assignedPercentages - personName    // removes their percentage
                                        viewModel.updateItem(
                                            index,
                                            item.copy(
                                                assignedTo = updatedAssigned,
                                                assignedQuantities = updatedQuantities,
                                                assignedPercentages = updatedPercentages
                                            )
                                        )
                                    },
                                    label = { Text("$personName ✕") }
                                )

                                when (item.splitMode) {
                                    SplitMode.BY_PERCENTAGE -> {
                                        // shows percentage input field
                                        val currentPct = item.assignedPercentages[personName]?.toString() ?: "" // current percentage
                                        val totalPct = item.assignedPercentages.values.sum() // total percentage assigned
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = currentPct,
                                                onValueChange = { input ->
                                                    val typed = input.toDoubleOrNull()     // converts to double safely
                                                    if (typed != null) {
                                                        val updatedPercentages = item.assignedPercentages + (personName to typed) // updates percentage
                                                        viewModel.updateItem(index, item.copy(assignedPercentages = updatedPercentages))
                                                    }
                                                },
                                                label = { Text("%") },                     // label for percentage field
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // shows number keyboard
                                                modifier = Modifier.width(80.dp),          // fixed width for percentage field
                                                singleLine = true                          // keeps it on one line
                                            )
                                            // shows warning if percentages don't add up to 100
                                            if (totalPct != 100.0 && item.assignedPercentages.size == item.assignedTo.size) {
                                                Text(
                                                    text = "${String.format("%.1f", totalPct)}%", // shows current total
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (totalPct == 100.0) MaterialTheme.colorScheme.primary // green if 100%
                                                    else MaterialTheme.colorScheme.error              // red if not 100%
                                                )
                                            }
                                        }
                                    }
                                    SplitMode.BY_QUANTITY -> {
                                        val personQty = item.assignedQuantities[personName] ?: 1 // current quantity for this person
                                        val othersTotal = totalAssigned - (item.assignedQuantities[personName] ?: 0) // total assigned to everyone else
                                        val maxForThisPerson = item.quantity - othersTotal // max this person can take

                                        // quantity picker — only shown if item has more than 1 unit
                                        if (item.quantity > 1) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                // minus button — decreases quantity down to 1
                                                OutlinedButton(
                                                    onClick = {
                                                        val newQty = (personQty - 1).coerceAtLeast(1) // never goes below 1
                                                        val updatedQuantities = item.assignedQuantities + (personName to newQty)
                                                        viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                                    }
                                                ) {
                                                    Text("-") // minus label
                                                }

                                                // editable quantity field
                                                OutlinedTextField(
                                                    value = personQty.toString(),
                                                    onValueChange = { input ->
                                                        val typed = input.toIntOrNull() ?: 1             // converts to int safely
                                                        val clamped = typed.coerceIn(1, maxForThisPerson) // clamps between 1 and max
                                                        val updatedQuantities = item.assignedQuantities + (personName to clamped)
                                                        viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.width(64.dp),      // fixed width for the number field
                                                    singleLine = true                      // keeps it on one line
                                                )

                                                // plus button
                                                OutlinedButton(
                                                    onClick = {
                                                        val newQty = (personQty + 1).coerceAtMost(maxForThisPerson) // never exceeds max
                                                        val updatedQuantities = item.assignedQuantities + (personName to newQty)
                                                        viewModel.updateItem(index, item.copy(assignedQuantities = updatedQuantities))
                                                    }
                                                ) {
                                                    Text("+") // plus label
                                                }

                                                // shows this person's calculated share
                                                val totalAssignedQty = item.assignedQuantities.values.sum().takeIf { it > 0 } ?: item.assignedTo.size
                                                val personShare = (personQty.toDouble() / totalAssignedQty) * item.totalPrice
                                                Text(
                                                    text = String.format("%.2f", personShare),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            // shows equal split summary if quantity is 1
                                            Text(
                                                text = "Pays: ${String.format("%.2f", item.totalPrice / item.assignedTo.size)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
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

        // ── Who Paid? section ──────────────────────────────────────────────────────
        // lets the user add one or more payers with a name and amount each

        Divider()

        Text(
            text = "Who Paid?",
            style = MaterialTheme.typography.titleMedium // medium title for this section
        )

        Text(
            text = "Add everyone who contributed to paying this expense.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // lighter description color
        )

        // shows the list of already added payers
        payers.forEachIndexed { index, payer ->
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
                    // shows payer name and amount
                    Column {
                        Text(
                            text = payer.payerName,                                    // payer's name
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Paid: ${String.format("%.2f", payer.amountPaid)}", // formatted amount
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // button to remove this payer
                    OutlinedButton(onClick = { viewModel.removePayer(index) }) {
                        Text("✕") // remove button
                    }
                }
            }
        }

        // form to add a new payer
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                Text(
                    text = "Add Payer",
                    style = MaterialTheme.typography.titleSmall // small title for add payer form
                )

                Spacer(modifier = Modifier.height(8.dp))

                // if launched from a group, show a dropdown of group members as payer name
                if (isFromGroup) {
                    ExposedDropdownMenuBox(
                        expanded = payerDropdownExpanded,
                        onExpandedChange = { payerDropdownExpanded = !payerDropdownExpanded } // toggles dropdown
                    ) {
                        OutlinedTextField(
                            value = newPayerName.ifEmpty { "Select a member" }, // shows selected name or placeholder
                            onValueChange = {},
                            readOnly = true,                                    // user can only select, not type
                            label = { Text("Payer Name") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()                                   // anchors the dropdown to this field
                        )
                        ExposedDropdownMenu(
                            expanded = payerDropdownExpanded,
                            onDismissRequest = { payerDropdownExpanded = false } // closes dropdown on dismiss
                        ) {
                            groupMembers.forEach { memberName ->
                                DropdownMenuItem(
                                    text = { Text(memberName) },                // shows the member name
                                    onClick = {
                                        newPayerName = memberName               // sets the selected member as payer name
                                        payerDropdownExpanded = false           // closes dropdown
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // if not from a group, show a free text field for payer name
                    OutlinedTextField(
                        value = newPayerName,
                        onValueChange = { newPayerName = it },                  // updates payer name
                        label = { Text("Payer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // amount field — how much this payer paid
                OutlinedTextField(
                    value = newPayerAmount,
                    onValueChange = { newPayerAmount = it },                    // updates payer amount
                    label = { Text("Amount Paid") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // shows number keyboard
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // button to add this payer to the list
                Button(
                    onClick = {
                        val amount = newPayerAmount.toDoubleOrNull() ?: 0.0    // converts amount safely
                        if (newPayerName.isNotBlank() && amount > 0.0) {       // only adds if name and amount are valid
                            viewModel.addPayer(
                                ExpensePayerRequest(
                                    payerName = newPayerName,                   // the payer's name
                                    amountPaid = amount                         // how much they paid
                                )
                            )
                            newPayerName = ""                                   // clears the name field
                            newPayerAmount = ""                                 // clears the amount field
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newPayerName.isNotBlank() && newPayerAmount.isNotBlank() // only enabled if both fields are filled
                ) {
                    Text("Add Payer") // button label
                }
            }
        }

        // next button to move to Step 6
        Button(
            onClick = { viewModel.nextStep() },
            modifier = Modifier.fillMaxWidth(),
            enabled = payers.isNotEmpty() // only enabled if at least one payer has been added
        ) {
            Text("Next")
        }
    }
}