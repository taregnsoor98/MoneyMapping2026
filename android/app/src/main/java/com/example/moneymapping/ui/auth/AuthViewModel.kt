package com.example.moneymapping.ui.auth

import android.app.Application // needed to get the app context for TokenManager
import androidx.lifecycle.AndroidViewModel // base class that provides app context unlike regular ViewModel
import androidx.lifecycle.viewModelScope // the coroutine scope tied to this ViewModel's lifecycle — cancels automatically when ViewModel is cleared
import com.example.moneymapping.data.TokenManager // handles saving and reading tokens from DataStore
import com.example.moneymapping.network.LoginRequest // the login request model sent to the server
import com.example.moneymapping.network.RegisterRequest // the register request model sent to the server
import com.example.moneymapping.network.RetrofitClient // our connection to the server
import kotlinx.coroutines.flow.MutableStateFlow // holds a value that the UI can observe and reacts to changes
import kotlinx.coroutines.flow.StateFlow // read-only version of MutableStateFlow exposed to the UI
import kotlinx.coroutines.launch // launches a background coroutine without blocking the UI

class AuthViewModel(application: Application) : AndroidViewModel(application) { // uses AndroidViewModel instead of ViewModel so we can access the app context for DataStore

    private val tokenManager = TokenManager(application) // creates TokenManager using app context to access DataStore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle) // current state, only editable inside this class
    val authState: StateFlow<AuthState> = _authState // read-only state that the UI observes

    fun register(email: String, username: String, password: String) { // called when user taps register
        viewModelScope.launch { // runs in background so UI doesn't freeze
            _authState.value = AuthState.Loading // show loading indicator while waiting for server
            try {
                val message = RetrofitClient.create(getApplication()).register(RegisterRequest(email, username, password)) // sends register request to the server
                _authState.value = AuthState.RegisterSuccess(message) // registration worked, show success message from server
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Register failed") // something went wrong, show error message
            }
        }
    }

    fun login(emailOrUsername: String, password: String) { // called when user taps login
        viewModelScope.launch { // runs in background so UI doesn't freeze
            _authState.value = AuthState.Loading // show loading indicator while waiting for server
            try {
                val response = RetrofitClient.create(getApplication()).login(LoginRequest(emailOrUsername, password)) // sends login request to the server
                val me = RetrofitClient.create(getApplication()).getMe("Bearer ${response.accessToken}") // fetches the current user's ID
                tokenManager.saveTokens(response.accessToken, response.refreshToken, me.id) // saves tokens and user ID to DataStore
                _authState.value = AuthState.LoginSuccess(response.accessToken, response.refreshToken) // login worked, update state with tokens
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed") // something went wrong, show error message
            }
        }
    }
}

sealed class AuthState { // represents all possible states of the auth process
    object Idle : AuthState()                                                                // nothing happening yet
    object Loading : AuthState()                                                             // waiting for server response
    data class RegisterSuccess(val message: String) : AuthState()                           // registration worked, contains server message
    data class LoginSuccess(val accessToken: String, val refreshToken: String) : AuthState() // login worked, contains both tokens
    data class Error(val message: String) : AuthState()                                      // something went wrong, contains error message
}