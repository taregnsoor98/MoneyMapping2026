package com.example.moneymapping.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation // hides password characters
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit, // passes emailOrUsername and password
    onRegisterClick: () -> Unit, // goes to register screen
    authState: AuthState // receives the current state to show feedback
) {
    var emailOrUsername by remember { mutableStateOf("") } // stores email or username typed by user
    var password by remember { mutableStateOf("") } // stores password typed by user

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = emailOrUsername, onValueChange = { emailOrUsername = it }, label = { Text("Email or Username") }) // email or username field
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation()) // password field
        Spacer(modifier = Modifier.height(16.dp))

        // show feedback based on current state
        when (authState) {
            is AuthState.Loading -> CircularProgressIndicator() // spinning loader
            is AuthState.Error -> Text(text = authState.message, color = MaterialTheme.colorScheme.error) // red error
            else -> {}
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onLoginClick(emailOrUsername, password) }) { Text("Login") } // login button
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRegisterClick) { Text("Don't have an account? Register") } // go to register
    }
}