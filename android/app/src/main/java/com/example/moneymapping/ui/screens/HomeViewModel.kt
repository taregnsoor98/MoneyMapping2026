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

class HomeViewModel(application: Application) : AndroidViewModel(application) { // uses AndroidViewModel to access app context

    private val tokenManager = TokenManager(application) // creates TokenManager using app context to access DataStore

    // Tracks the current state of the expenses fetch
    private val _expensesState = MutableStateFlow<ExpensesState>(ExpensesState.Loading)
    val expensesState: StateFlow<ExpensesState> = _expensesState // read-only state exposed to the UI

    init {
        fetchExpenses() // automatically fetches expenses when the ViewModel is first created
    }

    // Fetches all expenses for the logged-in user from the backend
    fun fetchExpenses() {
        viewModelScope.launch {
            _expensesState.value = ExpensesState.Loading // shows loading while fetching
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _expensesState.value = ExpensesState.Error("Session expired. Please log in again.")
                        return@launch // stops if no token is found
                    }
                val expenses = RetrofitClient.create(getApplication()).getExpenses("Bearer $accessToken") // calls GET /expenses
                _expensesState.value = ExpensesState.Success(expenses)                   // updates state with fetched expenses
            } catch (e: Exception) {
                _expensesState.value = ExpensesState.Error("Could not load expenses: ${e.message}") // shows error if fetch fails
            }
        }
    }

    // Deletes an expense by its ID and refreshes the list afterwards
    fun deleteExpense(id: String) {
        viewModelScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // stops if no token is found
                RetrofitClient.create(getApplication()).deleteExpense("Bearer $accessToken", id) // calls DELETE /expenses/{id}
                fetchExpenses() // refreshes the list after deletion
            } catch (e: Exception) {
                _expensesState.value = ExpensesState.Error("Could not delete expense: ${e.message}") // shows error if delete fails
            }
        }
    }
}

// Represents all possible states of the expenses fetch
sealed class ExpensesState {
    object Loading : ExpensesState()                                        // fetch is in progress
    data class Success(val expenses: List<ExpenseResponse>) : ExpensesState() // expenses loaded successfully
    data class Error(val message: String) : ExpensesState()                 // fetch failed
}