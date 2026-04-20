package com.example.moneymapping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // watches the authState for any changes and recomposes the UI
import androidx.compose.runtime.getValue // allows using "by" keyword to access state values cleanly
import androidx.compose.runtime.mutableStateOf // creates a simple state value that triggers UI updates when changed
import androidx.compose.runtime.remember // remembers the value across recompositions so it doesn't reset
import androidx.compose.runtime.setValue // allows changing the value of a remembered state
import androidx.lifecycle.viewmodel.compose.viewModel // creates and provides the ViewModel to the composable
import com.example.moneymapping.ui.auth.AuthState // the possible states of the auth process
import com.example.moneymapping.ui.auth.AuthViewModel // handles all auth logic
import com.example.moneymapping.ui.auth.LoginScreen // the login UI screen
import com.example.moneymapping.ui.auth.RegisterScreen // the register UI screen
import com.example.moneymapping.ui.navigation.MainScreen // imports the main screen with bottom nav
import com.example.moneymapping.ui.theme.MoneyMappingTheme // the app theme
import com.example.moneymapping.worker.scheduleInstallmentReminders  // schedules the daily installment reminder worker

class MainActivity : ComponentActivity() {

    // launcher that handles the result of the POST_NOTIFICATIONS permission request
    private val requestNotificationPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission() // standard contract for requesting a single permission
    ) { _ -> } // we don't need to handle the result — if denied, notifications simply won't show

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleInstallmentReminders(this)                                   // schedules the daily check for upcoming installments

        // asks the user for notification permission on Android 13+ when the app first opens
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) // shows the system permission dialog
        }

        setContent {
            MoneyMappingTheme {
                AppNavigation() // starts the app navigation
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel() // creates the AuthViewModel
    val authState by authViewModel.authState.collectAsState() // watches for auth state changes
    var showRegister by remember { mutableStateOf(false) } // tracks whether to show register or login screen

    when {
        authState is AuthState.LoginSuccess -> {
            MainScreen(authViewModel = authViewModel) // loads the main screen with bottom nav after successful login  and passes authViewModel so mainscreen can call logout
        }
        showRegister -> {
            RegisterScreen(
                onRegisterClick = { email, username, password ->
                    authViewModel.register(email, username, password) // calls register in ViewModel
                },
                onLoginClick = { showRegister = false }, // switches back to login screen
                authState = authState // passes current state so screen can show feedback
            )
        }
        else -> {
            LoginScreen(
                onLoginClick = { emailOrUsername, password ->
                    authViewModel.login(emailOrUsername, password) // calls login in ViewModel
                },
                onRegisterClick = { showRegister = true }, // switches to register screen
                authState = authState // passes current state so screen can show feedback
            )
        }
    }
}