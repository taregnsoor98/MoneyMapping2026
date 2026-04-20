package com.example.moneymapping.ui.screens

import android.app.Application                              // needed to access app context for TokenManager
import androidx.lifecycle.AndroidViewModel                  // gives us app context without leaking an Activity
import androidx.lifecycle.viewModelScope                    // coroutine scope that cancels when the ViewModel is destroyed
import com.example.moneymapping.data.CurrencyPreferenceManager // manages home and local currency preferences
import com.example.moneymapping.data.TokenManager          // handles reading and clearing stored tokens
import com.example.moneymapping.network.RetrofitClient     // our configured Retrofit instance for API calls
import kotlinx.coroutines.flow.MutableStateFlow            // holds a value and emits updates when it changes
import kotlinx.coroutines.flow.SharingStarted              // controls when the flow starts collecting
import kotlinx.coroutines.flow.StateFlow                   // read-only version of MutableStateFlow exposed to the UI
import kotlinx.coroutines.flow.stateIn                     // converts a Flow into a StateFlow so the UI can observe it
import kotlinx.coroutines.launch                           // launches a background coroutine without blocking the UI

// ─── State ────────────────────────────────────────────────────────────────────

// represents all possible states of the profile data fetch
sealed class ProfileState {
    object Loading : ProfileState()                                      // waiting for the API response
    data class Loaded(val username: String) : ProfileState()             // profile loaded successfully
    data class Error(val message: String) : ProfileState()               // something went wrong
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)                 // reads and clears tokens from DataStore
    private val currencyManager = CurrencyPreferenceManager(application) // reads and saves currency preferences

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading) // starts in Loading state
    val profileState: StateFlow<ProfileState> = _profileState            // read-only version exposed to the UI

    // observes the home currency from DataStore — updates the UI automatically when changed
    val homeCurrency: StateFlow<String> = currencyManager.homeCurrencyFlow
        .stateIn(
            scope = viewModelScope,                                      // tied to this ViewModel's lifecycle
            started = SharingStarted.WhileSubscribed(5000),             // keeps collecting while UI is active
            initialValue = CurrencyPreferenceManager.DEFAULT_HOME_CURRENCY // shown immediately before DataStore responds
        )

    // observes the local currency from DataStore — updates the UI automatically when changed
    val localCurrency: StateFlow<String> = currencyManager.localCurrencyFlow
        .stateIn(
            scope = viewModelScope,                                      // tied to this ViewModel's lifecycle
            started = SharingStarted.WhileSubscribed(5000),             // keeps collecting while UI is active
            initialValue = CurrencyPreferenceManager.DEFAULT_LOCAL_CURRENCY // shown immediately before DataStore responds
        )

    init {
        loadProfile()                                                    // load the user's profile as soon as the ViewModel is created
    }

    // fetches the current user's username from GET /account/me
    fun loadProfile() {
        viewModelScope.launch {                                          // runs in background so UI doesn't freeze
            _profileState.value = ProfileState.Loading                  // show spinner while loading
            try {
                val token = tokenManager.getAccessToken()               // read the stored access token
                    ?: run {
                        _profileState.value = ProfileState.Error("Session expired. Please log in again.")
                        return@launch                                    // exit early if no token found
                    }
                val me = RetrofitClient.create(getApplication())
                    .getMe("Bearer $token")                             // call GET /account/me to get username
                _profileState.value = ProfileState.Loaded(me.username) // update state with loaded username
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error("Could not load profile: ${e.message}")
            }
        }
    }

    // saves the selected home currency to DataStore
    fun saveHomeCurrency(currency: String) {
        viewModelScope.launch {                                          // runs in background since DataStore is async
            currencyManager.saveHomeCurrency(currency)                  // persists the selected home currency
        }
    }

    // saves the selected local currency to DataStore
    fun saveLocalCurrency(currency: String) {
        viewModelScope.launch {                                          // runs in background since DataStore is async
            currencyManager.saveLocalCurrency(currency)                 // persists the selected local currency
        }
    }

    // clears all stored tokens and resets auth state — called when user taps Log Out
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {                                          // runs in background since DataStore is async
            tokenManager.clearTokens()                                  // wipes access token, refresh token, and user ID
            onLoggedOut()                                               // notifies the UI to navigate to login screen
        }
    }
}