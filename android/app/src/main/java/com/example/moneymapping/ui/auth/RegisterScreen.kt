package com.example.moneymapping.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation // hides password characters
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String) -> Unit, // passes email, username, password
    onLoginClick: () -> Unit, // goes to login screen
    authState: AuthState // receives the current state to show feedback
) {
    var email by remember { mutableStateOf("") } // stores the email typed by user
    var username by remember { mutableStateOf("") } // stores the username typed by user
    var password by remember { mutableStateOf("") } // stores the password typed by user

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }) // email field
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }) // username field
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation()) // password field
        Spacer(modifier = Modifier.height(16.dp))

        // show feedback based on current state
        when (authState) {
            is AuthState.Loading -> CircularProgressIndicator() // spinning loader
            is AuthState.Error -> Text(text = authState.message, color = MaterialTheme.colorScheme.error) // red error
            is AuthState.RegisterSuccess -> Text(text = authState.message, color = MaterialTheme.colorScheme.primary) // success message in green
            else -> {}
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onRegisterClick(email, username, password) }) { Text("Register") } // register button
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onLoginClick) { Text("Already have an account? Login") } // go to login
    }
}