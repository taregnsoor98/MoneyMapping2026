package com.example.moneymapping.ui.screens

import android.app.Application                            // needed to get the app context for TokenManager
import androidx.lifecycle.AndroidViewModel                // base class that provides app context
import androidx.lifecycle.viewModelScope                  // coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.data.TokenManager        // handles reading tokens from DataStore
import com.example.moneymapping.network.CreateExpenseRequest    // the request model for updating an expense
import com.example.moneymapping.network.ExpenseItemRequest      // the request model for a single item
import com.example.moneymapping.network.ExpenseItemResponse     // the response model for a single item
import com.example.moneymapping.network.ExpenseResponse         // the expense model returned by the backend
import com.example.moneymapping.network.ItemAssignmentRequest   // the request model for a single assignment
import com.example.moneymapping.network.RetrofitClient          // our connection to the backend
import com.example.moneymapping.network.UserSearchResult        // the result model for user search
import kotlinx.coroutines.flow.MutableStateFlow          // holds a value and emits updates when it changes
import kotlinx.coroutines.flow.StateFlow                 // read-only version exposed to the UI
import kotlinx.coroutines.launch                         // launches a coroutine for background work

class EditExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using app context

    // Tracks the current state of the edit screen
    private val _state = MutableStateFlow<EditExpenseState>(EditExpenseState.Loading)
    val state: StateFlow<EditExpenseState> = _state // read-only state exposed to the UI

    // Tracks the current list of items being edited — separate from the main state for easier updates
    private val _editableItems = MutableStateFlow<List<EditableItem>>(emptyList())
    val editableItems: StateFlow<List<EditableItem>> = _editableItems // read-only list exposed to the UI

    // Tracks the current user search state
    private val _searchState = MutableStateFlow<EditSearchState>(EditSearchState.Idle)
    val searchState: StateFlow<EditSearchState> = _searchState // read-only search state exposed to the UI

    // Loads a single expense by ID from the backend
    fun loadExpense(expenseId: String) {
        viewModelScope.launch {
            _state.value = EditExpenseState.Loading // shows loading while fetching
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _state.value = EditExpenseState.Error("Session expired. Please log in again.")
                        return@launch
                    }
                val expense = RetrofitClient.create(getApplication()).getExpense("Bearer $accessToken", expenseId) // fetches the single expense by ID
                _editableItems.value = expense.items.map { it.toEditableItem() } // converts items to editable items
                _state.value = EditExpenseState.Loaded(expense) // updates state with the loaded expense
            } catch (e: Exception) {
                _state.value = EditExpenseState.Error("Could not load expense: ${e.message}") // shows error
            }
        }
    }

    // Adds a new empty item to the editable items list
    fun addItem() {
        _editableItems.value = _editableItems.value + EditableItem() // appends a new empty item
    }

    // Updates an existing item in the editable items list by its index
    fun updateItem(index: Int, item: EditableItem) {
        val updated = _editableItems.value.toMutableList()
        updated[index] = item // replaces the item at the given index
        _editableItems.value = updated
    }

    // Removes an item from the editable items list by its index
    fun removeItem(index: Int) {
        val updated = _editableItems.value.toMutableList()
        updated.removeAt(index) // removes the item at the given index
        _editableItems.value = updated
    }

    // Searches for existing users by username or email
    fun searchUsers(query: String) {
        if (query.length < 2) { // require at least 2 characters before searching
            _searchState.value = EditSearchState.Idle // resets search state if query is too short
            return
        }
        viewModelScope.launch {
            _searchState.value = EditSearchState.Loading // shows loading while searching
            try {
                val results = RetrofitClient.create(getApplication()).searchUsers(query) // calls the search endpoint
                _searchState.value = EditSearchState.Results(results)   // updates state with results
            } catch (e: Exception) {
                _searchState.value = EditSearchState.Error("Search failed: ${e.message}") // shows error
            }
        }
    }

    // Resets the search state back to idle
    fun resetSearch() {
        _searchState.value = EditSearchState.Idle // clears search results
    }

    // Saves the updated expense and its items to the backend
    fun saveExpense(expenseId: String, request: CreateExpenseRequest) {
        viewModelScope.launch {
            _state.value = EditExpenseState.Saving // shows saving indicator
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _state.value = EditExpenseState.Error("Session expired. Please log in again.")
                        return@launch
                    }

                // Converts editable items to request items to send to the backend
                val itemRequests = _editableItems.value.map { item ->
                    val totalAssignedQty = item.assignments.sumOf { it.quantity }.takeIf { it > 0.0 } ?: item.assignments.size.toDouble() // total quantity assigned across all people
                    ExpenseItemRequest(
                        name = item.name,            // item name
                        unitPrice = item.unitPrice,  // unit price
                        quantity = item.quantity,    // quantity
                        totalPrice = item.totalPrice, // total price
                        assignments = item.assignments.map { assignment ->
                            val shareAmount = if (totalAssignedQty > 0.0)
                                (assignment.quantity / totalAssignedQty) * item.totalPrice // proportional share
                            else
                                item.totalPrice / item.assignments.size                    // equal split fallback
                            ItemAssignmentRequest(
                                personName = assignment.personName,  // the person's name
                                quantity = assignment.quantity,      // their quantity
                                shareAmount = shareAmount            // how much they owe
                            )
                        }
                    )
                }

                // Builds the final request with the updated items and recalculated total
                val finalRequest = request.copy(
                    amount = _editableItems.value.sumOf { it.totalPrice }, // recalculates total from items
                    items = itemRequests                                    // updated items list
                )

                RetrofitClient.create(getApplication()).updateExpense("Bearer $accessToken", expenseId, finalRequest) // calls PUT /expenses/{id}
                _state.value = EditExpenseState.Saved // marks as saved successfully
            } catch (e: Exception) {
                _state.value = EditExpenseState.Error("Could not save expense: ${e.message}") // shows error
            }
        }
    }
}

// Represents a single assignment being edited
data class EditableAssignment(
    val personName: String = "", // the person's name — either a username or guest name
    val quantity: Double = 1.0   // how many units of the item this person is taking
)

// Represents a single item being edited in the edit screen
data class EditableItem(
    val name: String = "",                                  // item name
    val unitPrice: Double = 0.0,                           // price per unit
    val quantity: Double = 1.0,                            // quantity — Double to support weight-based items
    val totalPrice: Double = 0.0,                          // total price for this item
    val assignments: List<EditableAssignment> = emptyList() // list of people assigned to this item
)

// Converts a backend item response to an editable item
fun ExpenseItemResponse.toEditableItem() = EditableItem(
    name = name,             // item name from backend
    unitPrice = unitPrice,   // unit price from backend
    quantity = quantity,     // quantity from backend
    totalPrice = totalPrice, // total price from backend
    assignments = assignments.map { assignment ->
        EditableAssignment(
            personName = assignment.personName, // person name from backend
            quantity = assignment.quantity      // quantity from backend
        )
    }
)

// Represents all possible states of the user search in the edit screen
sealed class EditSearchState {
    object Idle : EditSearchState()                                                // no search happening
    object Loading : EditSearchState()                                             // search is in progress
    data class Results(val users: List<UserSearchResult>) : EditSearchState()      // search returned results
    data class Error(val message: String) : EditSearchState()                      // search failed
}

// Represents all possible states of the edit expense screen
sealed class EditExpenseState {
    object Loading : EditExpenseState()                                  // fetching the expense
    data class Loaded(val expense: ExpenseResponse) : EditExpenseState() // expense loaded and ready to edit
    object Saving : EditExpenseState()                                   // saving the updated expense
    object Saved : EditExpenseState()                                    // expense saved successfully
    data class Error(val message: String) : EditExpenseState()           // something went wrong
}