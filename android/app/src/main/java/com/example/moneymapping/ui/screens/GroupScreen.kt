package com.example.moneymapping.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn                 // a scrollable list that only renders visible items
import androidx.compose.foundation.lazy.items                      // renders a list of items in LazyColumn
import androidx.compose.material.icons.Icons                       // provides built-in icons
import androidx.compose.material.icons.filled.Add                  // the + icon
import androidx.compose.material3.Card                             // a card container with elevation
import androidx.compose.material3.CircularProgressIndicator        // a loading spinner
import androidx.compose.material3.FloatingActionButton             // the floating + button
import androidx.compose.material3.Icon                             // displays an icon
import androidx.compose.material3.MaterialTheme                    // provides theme colors and typography
import androidx.compose.material3.Scaffold                         // provides basic screen structure with FAB support
import androidx.compose.material3.Text                             // displays text
import androidx.compose.runtime.Composable                         // marks a function as a composable
import androidx.compose.runtime.collectAsState                     // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                           // allows using 'by' delegation for state
import androidx.compose.ui.Alignment                               // controls alignment of composables
import androidx.compose.ui.Modifier                                // used to style and layout composables
import androidx.compose.ui.unit.dp                                 // density-independent pixels
import androidx.lifecycle.viewmodel.compose.viewModel              // creates a ViewModel in a composable

@Composable
fun GroupsScreen(
    onNavigateToCreateGroup: () -> Unit,          // called when the + button is tapped
    onNavigateToGroupDetail: (Long) -> Unit        // called when a group card is tapped
) {
    val viewModel: GroupViewModel = viewModel() // creates the GroupViewModel
    val state by viewModel.state.collectAsState() // observes the current screen state

    // re-fetches groups every time this screen becomes visible
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchGroups() // refreshes the groups list when screen appears
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateGroup) { // navigates to create group screen on click
                Icon(Icons.Filled.Add, contentDescription = "Create Group") // + icon
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // respects the scaffold's padding
        ) {
            when (state) {

                // shows a loading spinner while fetching groups
                is GroupScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // centered spinner
                }

                // shows an error message if something went wrong
                is GroupScreenState.Error -> {
                    Text(
                        text = (state as GroupScreenState.Error).message, // the error message
                        color = MaterialTheme.colorScheme.error,          // red error color
                        modifier = Modifier.align(Alignment.Center)       // centered on screen
                    )
                }

                // shows the list of groups when loaded successfully
                is GroupScreenState.Success -> {
                    val groups = (state as GroupScreenState.Success).groups // gets the loaded groups

                    if (groups.isEmpty()) {
                        // shows a message if the user has no groups yet
                        Text(
                            text = "You have no groups yet. Tap + to create one!",
                            modifier = Modifier.align(Alignment.Center) // centered on screen
                        )
                    } else {
                        // shows the list of groups
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp), // padding around the list
                            verticalArrangement = Arrangement.spacedBy(12.dp) // space between cards
                        ) {
                            items(groups) { group ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToGroupDetail(group.id) } // navigates to group detail on tap
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) { // padding inside card
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween // name and type on opposite sides
                                        ) {
                                            Text(
                                                text = group.name,                          // group name
                                                style = MaterialTheme.typography.titleMedium // medium title style
                                            )
                                            // shows a colored label based on group type
                                            Text(
                                                text = when (group.type) {
                                                    "FRIEND" -> "👥 Friends"       // friends group label
                                                    "FAMILY" -> "👨‍👩‍👧 Family"       // family group label
                                                    "ONE_TIME" -> "⚡ One-Time"    // one-time group label
                                                    else -> group.type              // fallback to raw type
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when (group.type) {
                                                    "FRIEND" -> MaterialTheme.colorScheme.primary  // blue for friends
                                                    "FAMILY" -> MaterialTheme.colorScheme.secondary // purple for family
                                                    else -> MaterialTheme.colorScheme.error         // red for one-time
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp)) // space between name and member count
                                        Text(
                                            text = "${group.members.size} member(s)", // shows number of members
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (group.isLocked) { // shows locked badge for closed ONE_TIME groups
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Settled & Closed",                      // locked group label
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error         // red to indicate closed
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