package com.example.moneymapping.ui.expense

// The main container for the Add Expense wizard
// Controls which step is currently shown based on the current step number in the ViewModel
// Also handles the top bar with the back button and the current step title

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onExpenseAdded: () -> Unit,              // callback to navigate back after successful submission
    onBack: () -> Unit,                      // callback to navigate back when back button is pressed
    groupId: Long? = null,                   // optional group ID if launched from inside a group
    memberNames: List<String> = emptyList()  // optional list of group member names if launched from inside a group
) {
    val viewModel: ExpenseViewModel = viewModel() // creates the ExpenseViewModel
    val currentStep by viewModel.currentStep.collectAsState()   // watches which step we are on
    val isFromGroup by viewModel.isFromGroup.collectAsState()    // true if launched from inside a group

    // if launched from a group, set up the ViewModel with the group context
    LaunchedEffect(groupId) {
        if (groupId != null) {
            viewModel.launchFromGroup(groupId, memberNames) // sets up the group context in the ViewModel
        }
    }

    // step titles shown in the top bar — step 3 is skipped when launched from a group
    val stepTitles = mapOf(
        1 to "How to add?",       // step 1 — scan or manual
        2 to "Expense Details",   // step 2 — basic info
        3 to "Solo or Shared?",   // step 3 — expense type (skipped if from group)
        4 to "Items",             // step 4 — items list
        5 to "Assign People",     // step 5 — assign people to items
        6 to "Review & Confirm"   // step 6 — summary and confirm
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
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back" // back arrow in top bar
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
                .padding(16.dp) // inner padding for content
        ) {
            // shows the correct step based on current step number
            // skips step 3 when launched from a group — type is already set
            when (currentStep) {
                1 -> StepOne(viewModel = viewModel)   // scan or manual choice
                2 -> StepTwo(viewModel = viewModel)   // basic expense info
                3 -> {
                    if (isFromGroup) {
                        viewModel.nextStep()           // skips step 3 automatically if launched from a group
                    } else {
                        StepThree(viewModel = viewModel) // shows step 3 only if not from a group
                    }
                }
                4 -> StepFour(viewModel = viewModel)  // items list
                5 -> StepFive(viewModel = viewModel)  // assign people to items
                6 -> StepSix(                         // review and confirm
                    viewModel = viewModel,
                    onExpenseAdded = onExpenseAdded
                )
            }
        }
    }
}