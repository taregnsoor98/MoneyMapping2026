package com.example.moneymapping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneymapping.ui.auth.AuthState
import com.example.moneymapping.ui.auth.AuthViewModel
import com.example.moneymapping.ui.auth.LoginScreen
import com.example.moneymapping.ui.auth.RegisterScreen
import com.example.moneymapping.ui.theme.MoneyMappingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyMappingTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    var showRegister by remember { mutableStateOf(false) }

    when {
        authState is AuthState.Success -> {
            Text("Logged in successfully!") // temporary success message
        }
        showRegister -> {
            RegisterScreen(
                onRegisterClick = { username, password ->
                    authViewModel.register(username, password)
                },
                onLoginClick = { showRegister = false },
                authState = authState // pass state so screen can show errors
            )
        }
        else -> {
            LoginScreen(
                onLoginClick = { username, password ->
                    authViewModel.login(username, password)
                },
                onRegisterClick = { showRegister = true },
                authState = authState // pass state so screen can show errors
            )
        }
    }
}