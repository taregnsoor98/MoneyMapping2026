package com.example.moneymapping.ui.auth // this file belongs to the auth UI package

import androidx.lifecycle.ViewModel // base class for ViewModels
import androidx.lifecycle.viewModelScope // the coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.network.Credentials // the data we send to the server
import com.example.moneymapping.network.RetrofitClient // our connection to the server
import kotlinx.coroutines.flow.MutableStateFlow // holds a value that the UI can observe
import kotlinx.coroutines.flow.StateFlow // read-only version of MutableStateFlow
import kotlinx.coroutines.launch // launches a background coroutine

class AuthViewModel : ViewModel() { // ViewModel survives screen rotations

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle) // current state, only editable inside this class
    val authState: StateFlow<AuthState> = _authState // read-only state that the UI observes

    fun login(username: String, password: String) { // called when user taps login
        viewModelScope.launch { // runs in background so UI doesn't freeze
            _authState.value = AuthState.Loading // show loading indicator
            try {
                val response = RetrofitClient.authApi.login(Credentials(username, password)) // call the server
                _authState.value = AuthState.Success(response.accessToken) // login worked, save token
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed") // something went wrong
            }
        }
    }

    fun register(username: String, password: String) { // called when user taps register
        viewModelScope.launch { // runs in background so UI doesn't freeze
            _authState.value = AuthState.Loading // show loading indicator
            try {
                val response = RetrofitClient.authApi.register(Credentials(username, password)) // call the server
                _authState.value = AuthState.Success(response.accessToken) // register worked, save token
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Register failed") // something went wrong
            }
        }
    }
}

sealed class AuthState { // represents all possible states of the auth process
    object Idle : AuthState() // nothing happening yet
    object Loading : AuthState() // waiting for server response
    data class Success(val token: String) : AuthState() // login/register worked, contains the token
    data class Error(val message: String) : AuthState() // something went wrong, contains error message
}