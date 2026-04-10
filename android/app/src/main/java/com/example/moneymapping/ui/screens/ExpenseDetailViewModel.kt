package com.example.moneymapping.ui.screens

import android.app.Application                            // needed to get the app context for TokenManager
import androidx.lifecycle.AndroidViewModel                // base class that provides app context
import androidx.lifecycle.viewModelScope                  // coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.data.TokenManager        // handles reading tokens from DataStore
import com.example.moneymapping.network.ExpenseResponse  // the expense model returned by the backend
import com.example.moneymapping.network.RetrofitClient   // our connection to the backend
import kotlinx.coroutines.flow.MutableStateFlow          // holds a value and emits updates when it changes
import kotlinx.coroutines.flow.StateFlow                 // read-only version exposed to the UI
import kotlinx.coroutines.launch                         // launches a coroutine for background work

class ExpenseDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using app context

    // Tracks the current state of the detail screen
    private val _state = MutableStateFlow<ExpenseDetailState>(ExpenseDetailState.Loading)
    val state: StateFlow<ExpenseDetailState> = _state // read-only state exposed to the UI

    // Loads a single expense by ID from the backend
    fun loadExpense(expenseId: String) {
        viewModelScope.launch {
            _state.value = ExpenseDetailState.Loading // shows loading while fetching
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _state.value = ExpenseDetailState.Error("Session expired. Please log in again.")
                        return@launch
                    }
                val expense = RetrofitClient.create(getApplication()).getExpense("Bearer $accessToken", expenseId) // fetches the single expense by ID
                _state.value = ExpenseDetailState.Loaded(expense) // updates state with the loaded expense
            } catch (e: Exception) {
                _state.value = ExpenseDetailState.Error("Could not load expense: ${e.message}") // shows error
            }
        }
    }
}

// Represents all possible states of the expense detail screen
sealed class ExpenseDetailState {
    object Loading : ExpenseDetailState()                                  // fetching the expense
    data class Loaded(val expense: ExpenseResponse) : ExpenseDetailState() // expense loaded successfully
    data class Error(val message: String) : ExpenseDetailState()           // something went wrong
}