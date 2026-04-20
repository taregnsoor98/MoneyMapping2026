package com.example.moneymapping.ui.screens

import androidx.compose.foundation.background                          // used to draw the avatar circle background
import androidx.compose.foundation.layout.Arrangement                 // controls spacing between items in a Column/Row
import androidx.compose.foundation.layout.Box                         // stacks children on top of each other — used for the avatar circle
import androidx.compose.foundation.layout.Column                      // arranges children vertically
import androidx.compose.foundation.layout.Row                         // arranges children horizontally
import androidx.compose.foundation.layout.Spacer                      // adds empty space between composables
import androidx.compose.foundation.layout.fillMaxSize                 // makes a composable fill all available space
import androidx.compose.foundation.layout.fillMaxWidth                // makes a composable fill the full width
import androidx.compose.foundation.layout.height                      // sets a fixed height
import androidx.compose.foundation.layout.padding                     // adds padding around a composable
import androidx.compose.foundation.layout.size                        // sets both width and height at once
import androidx.compose.foundation.rememberScrollState                // remembers the scroll position of a scrollable column
import androidx.compose.foundation.shape.CircleShape                  // used to make the avatar a circle
import androidx.compose.foundation.verticalScroll                     // makes a column scrollable vertically
import androidx.compose.material3.Button                              // filled button — used for Log Out
import androidx.compose.material3.ButtonDefaults                      // provides button color customization
import androidx.compose.material3.Card                                // card container with elevation and rounded corners
import androidx.compose.material3.CardDefaults                        // provides default card styling
import androidx.compose.material3.CircularProgressIndicator           // spinning loading indicator
import androidx.compose.material3.HorizontalDivider                   // draws a horizontal line between list items
import androidx.compose.material3.MaterialTheme                       // provides theme colors and typography
import androidx.compose.material3.Text                                // displays text
import androidx.compose.runtime.Composable                            // marks a function as a Compose UI component
import androidx.compose.runtime.collectAsState                        // observes a StateFlow as Compose state
import androidx.compose.runtime.getValue                              // allows using 'by' delegation for state
import androidx.compose.ui.Alignment                                  // controls alignment inside layouts
import androidx.compose.ui.Modifier                                   // used to style and layout composables
import androidx.compose.ui.draw.clip                                  // clips a composable to a specific shape
import androidx.compose.ui.graphics.Color                             // used to set custom colors
import androidx.compose.ui.text.font.FontWeight                       // makes text bold
import androidx.compose.ui.unit.dp                                    // density-independent pixels for sizing
import androidx.compose.ui.unit.sp                                    // scale-independent pixels for font sizes
import androidx.lifecycle.viewmodel.compose.viewModel                 // creates a ViewModel scoped to this composable
import androidx.compose.material3.DropdownMenuItem                    // a single item inside a dropdown menu
import androidx.compose.material3.ExperimentalMaterial3Api            // needed for ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBox              // a dropdown menu anchored to a text field
import androidx.compose.material3.ExposedDropdownMenuDefaults         // provides default trailing icon for the dropdown
import androidx.compose.material3.OutlinedTextField                   // the text field that anchors the dropdown
import androidx.compose.runtime.mutableStateOf                        // creates a mutable state value
import androidx.compose.runtime.remember                              // remembers a value across recompositions
import androidx.compose.runtime.setValue                              // allows setting state with 'by' delegation
import com.example.moneymapping.data.CurrencyPreferenceManager        // provides the list of supported currencies
import androidx.compose.material3.TextButton

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    onLogout: () -> Unit                                               // called when the user confirms logout — navigates to login
) {
    val viewModel: ProfileViewModel = viewModel()                      // creates or retrieves the ProfileViewModel
    val profileState by viewModel.profileState.collectAsState()        // observes the profile loading state

    // makes the whole screen scrollable in case content overflows on small screens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)             // consistent spacing between all sections
    ) {

        // ── Page title ────────────────────────────────────────────────────────
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold                               // bold page title, consistent with HomeScreen
        )

        // ── Header (avatar + username) ────────────────────────────────────────
        when (val state = profileState) {
            is ProfileState.Loading -> {
                // shows a centered spinner while the username is being fetched
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()                        // loading spinner
                }
            }
            is ProfileState.Error -> {
                // shows the error message if the profile fetch failed
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error            // red error text
                )
            }
            is ProfileState.Loaded -> {
                // shows the avatar and username once loaded
                ProfileHeader(username = state.username)
            }
        }

        // ── Section: Account ──────────────────────────────────────────────────
        ProfileSection(title = "Account") {
            val username = (profileState as? ProfileState.Loaded)?.username ?: "—" // shows username or dash if not loaded
            ProfileInfoRow(label = "Username", value = username)
        }

        // ── Section: Security (placeholder) ───────────────────────────────────
        ProfileSection(title = "Security") {
            ProfilePlaceholderRow(label = "Change Password")           // placeholder — not yet implemented
            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp)) // separator line between rows
            ProfilePlaceholderRow(label = "Change Email")              // placeholder — not yet implemented
        }

        // ── Section: Preferences (currencies) ────────────────────────────────
        ProfileSection(title = "Preferences") {
            val homeCurrency by viewModel.homeCurrency.collectAsState()  // observes the saved home currency
            val localCurrency by viewModel.localCurrency.collectAsState() // observes the saved local currency

            CurrencyDropdownRow(
                label = "Home Currency",
                selected = homeCurrency,                                 // currently saved home currency
                onSelected = { viewModel.saveHomeCurrency(it) }         // saves when user picks a new one
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
            CurrencyDropdownRow(
                label = "Local Currency",
                selected = localCurrency,                                // currently saved local currency
                onSelected = { viewModel.saveLocalCurrency(it) }        // saves when user picks a new one
            )
        }

        // ── Section: App ──────────────────────────────────────────────────────
        ProfileSection(title = "App") {
            Spacer(modifier = Modifier.height(4.dp))
            // log out button — clears tokens then navigates to login
            Button(
                onClick = {
                    viewModel.logout(onLoggedOut = onLogout)           // clears tokens, then calls back to navigate
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error   // red button to signal a destructive action
                )
            ) {
                Text(
                    text = "Log Out",
                    color = Color.White                                 // white text on red background
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

// shows the avatar circle with the first letter of the username, plus the username below it
@Composable
private fun ProfileHeader(username: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,           // centers the avatar and username horizontally
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // circular avatar with the first letter of the username
        Box(
            modifier = Modifier
                .size(80.dp)                                          // 80dp circle
                .clip(CircleShape)                                    // clips to a circle shape
                .background(MaterialTheme.colorScheme.primary),       // fills with the theme's primary color
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?", // first letter uppercase, or ? if empty
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White                                   // white letter on colored background
            )
        }

        // username displayed below the avatar
        Text(
            text = username,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Section wrapper ──────────────────────────────────────────────────────────

// wraps content in a labeled card section — reused for Account, Security, Preferences, App
@Composable
private fun ProfileSection(
    title: String,                                                    // the section header label
    content: @Composable () -> Unit                                   // the rows inside the section
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // section title label above the card
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,       // subdued color for section headers
            fontWeight = FontWeight.Bold
        )

        // card that contains all the rows for this section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()                                             // renders whatever rows were passed in
            }
        }
    }
}

// ─── Row types ────────────────────────────────────────────────────────────────

// a read-only info row showing a label on the left and a value on the right
@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,             // pushes label to left, value to right
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant        // slightly muted value text
        )
    }
}

// a placeholder row that shows a label on the left and "Coming soon" on the right
@Composable
private fun ProfilePlaceholderRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = {}) {
            Text("Manage")
        }
    }
}
// a dropdown row that lets the user pick a currency from the supported list
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdownRow(
    label: String,                                                    // the label shown on the left e.g. "Home Currency"
    selected: String,                                                 // the currently selected currency code
    onSelected: (String) -> Unit                                      // called when the user picks a new currency
) {
    var expanded by remember { mutableStateOf(false) }               // tracks whether the dropdown is open or closed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium              // label on the left
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }              // toggles the dropdown open/closed on tap
        ) {
            OutlinedTextField(
                value = selected,                                    // shows the currently selected currency
                onValueChange = {},                                  // read-only — user can only pick from the list
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) // arrow icon that rotates when open
                },
                modifier = Modifier.menuAnchor()                    // anchors the dropdown to this text field
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }             // closes the dropdown when user taps outside
            ) {
                CurrencyPreferenceManager.SUPPORTED_CURRENCIES.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },                  // shows the currency code e.g. "USD"
                        onClick = {
                            onSelected(currency)                    // saves the selected currency
                            expanded = false                        // closes the dropdown after selection
                        }
                    )
                }
            }
        }
    }
}