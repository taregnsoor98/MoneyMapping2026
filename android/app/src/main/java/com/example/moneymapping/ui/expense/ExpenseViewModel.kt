package com.example.moneymapping.ui.expense

import android.app.Application // needed to get the app context for TokenManager
import androidx.lifecycle.AndroidViewModel // base class that provides app context unlike regular ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymapping.data.TokenManager // handles saving and reading tokens from DataStore
import com.example.moneymapping.network.CreateExpenseRequest
import com.example.moneymapping.network.ExpenseItemRequest
import com.example.moneymapping.network.GroupResult
import com.example.moneymapping.network.RetrofitClient
import com.example.moneymapping.network.UserSearchResult
import kotlinx.coroutines.flow.MutableStateFlow // holds a value and emits updates when it changes
import kotlinx.coroutines.flow.StateFlow // read-only version exposed to the UI
import kotlinx.coroutines.launch // launches a coroutine for background work
import com.example.moneymapping.network.ItemAssignmentRequest // the request model for a single assignment

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using app context to access DataStore

    // Tracks the current step in the wizard (1 through 6)
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    // Tracks whether the user chose to scan or enter manually
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    // Basic expense info fields
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _date = MutableStateFlow("")
    val date: StateFlow<String> = _date

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category

    // Tracks whether expense is solo, group, or one-time split
    private val _expenseType = MutableStateFlow<ExpenseType>(ExpenseType.Solo)
    val expenseType: StateFlow<ExpenseType> = _expenseType

    // List of items in the expense
    private val _items = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val items: StateFlow<List<ExpenseItem>> = _items

    // List of participants for one-time splits and group expenses
    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = _participants

    // Receipt images picked from gallery
    private val _receiptImages = MutableStateFlow<List<String>>(emptyList())
    val receiptImages: StateFlow<List<String>> = _receiptImages

    // Tracks the current submission state
    private val _expenseState = MutableStateFlow<ExpenseState>(ExpenseState.Idle)
    val expenseState: StateFlow<ExpenseState> = _expenseState

    // Tracks the current user search state
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    // Tracks the current groups fetch state
    private val _groupsState = MutableStateFlow<GroupsState>(GroupsState.Idle)
    val groupsState: StateFlow<GroupsState> = _groupsState

    // Moves to the next step in the wizard
    fun nextStep() {
        if (_currentStep.value < 6) _currentStep.value++ // increments step if not at the last
    }

    // Moves to the previous step in the wizard
    fun previousStep() {
        if (_currentStep.value > 1) _currentStep.value-- // decrements step if not at the first
    }

    // Sets whether the user is scanning or entering manually
    fun setScanning(scanning: Boolean) {
        _isScanning.value = scanning
    }

    // Updates the description field
    fun setDescription(value: String) { _description.value = value }

    // Updates the date field
    fun setDate(value: String) { _date.value = value }

    // Updates the currency field
    fun setCurrency(value: String) { _currency.value = value }

    // Updates the category field
    fun setCategory(value: String) { _category.value = value }

    // Updates the expense type — solo, group, or one-time split
    fun setExpenseType(type: ExpenseType) { _expenseType.value = type }

    // Adds a new item to the expense items list
    fun addItem(item: ExpenseItem) {
        _items.value = _items.value + item // appends the new item to the list
    }

    // Updates an existing item in the list by its index
    fun updateItem(index: Int, item: ExpenseItem) {
        val updated = _items.value.toMutableList()
        updated[index] = item // replaces the item at the given index
        _items.value = updated
    }

    // Removes an item from the list by its index
    fun removeItem(index: Int) {
        val updated = _items.value.toMutableList()
        updated.removeAt(index) // removes the item at the given index
        _items.value = updated
    }

    // Adds a participant to the list
    fun addParticipant(participant: Participant) {
        _participants.value = _participants.value + participant // appends participant to list
    }

    // Removes a participant by index
    fun removeParticipant(index: Int) {
        val updated = _participants.value.toMutableList()
        updated.removeAt(index) // removes participant at given index
        _participants.value = updated
    }

    // Adds receipt image URIs to the list
    fun addReceiptImages(uris: List<String>) {
        _receiptImages.value = _receiptImages.value + uris // appends new images to existing list
    }

    // Fetches the user's groups from the backend using the stored token
    fun fetchGroups() {
        viewModelScope.launch {
            _groupsState.value = GroupsState.Loading // shows loading while fetching
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // stops if no token is found
                val groups = RetrofitClient.authApi.getGroups("Bearer $accessToken") // calls the groups endpoint with token
                _groupsState.value = GroupsState.Success(groups) // updates state with fetched groups
            } catch (e: Exception) {
                _groupsState.value = GroupsState.Error("Could not load groups: ${e.message}") // shows error if fetch fails
            }
        }
    }

    // Searches for existing users by username or email
    fun searchUsers(query: String) {
        if (query.length < 2) { // require at least 2 characters before searching
            _searchState.value = SearchState.Idle // resets search state if query is too short
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading // shows loading while searching
            try {
                val results = RetrofitClient.authApi.searchUsers(query) // calls the search endpoint
                _searchState.value = SearchState.Results(results) // updates state with results
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("Search failed: ${e.message}") // shows error if search fails
            }
        }
    }

    // Resets the search state back to idle
    fun resetSearch() {
        _searchState.value = SearchState.Idle // clears search results
    }

    // Calculates how much each participant owes based on item assignments and quantities
    fun calculateShares(): Map<String, Double> {
        val shares = mutableMapOf<String, Double>() // maps participant name to amount owed
        _items.value.forEach { item ->
            if (item.assignedTo.isNotEmpty()) {
                if (item.assignedQuantities.isNotEmpty()) {
                    // Calculates share based on quantity each person is taking
                    val totalAssignedQty = item.assignedQuantities.values.sum() // total assigned quantity
                    item.assignedQuantities.forEach { (person, qty) ->
                        val share = (qty.toDouble() / totalAssignedQty) * item.totalPrice // proportional share
                        shares[person] = (shares[person] ?: 0.0) + share // adds to their total
                    }
                } else {
                    // Falls back to equal split if no quantities are specified
                    val sharePerPerson = item.totalPrice / item.assignedTo.size // equal split
                    item.assignedTo.forEach { person ->
                        shares[person] = (shares[person] ?: 0.0) + sharePerPerson // adds to their total
                    }
                }
            }
        }
        return shares
    }

    // Submits the expense — refreshes token first, then sends expense data and items to the backend
    fun submitExpense() {
        viewModelScope.launch {
            _expenseState.value = ExpenseState.Loading // shows loading while submitting
            try {
                var accessToken = tokenManager.getAccessToken()   // gets the stored access token
                val refreshToken = tokenManager.getRefreshToken() // gets the stored refresh token

                // If no access token exists, the user needs to log in again
                if (accessToken == null) {
                    _expenseState.value = ExpenseState.Error("Session expired. Please log in again.")
                    return@launch
                }

                // Tries to refresh the token in case it has expired
                try {
                    val refreshResponse = RetrofitClient.authApi.refresh("Bearer $refreshToken") // calls refresh endpoint
                    accessToken = refreshResponse.accessToken                                     // uses the new access token
                    tokenManager.saveTokens(refreshResponse.accessToken, refreshResponse.refreshToken) // saves the new tokens
                } catch (e: Exception) {
                    _expenseState.value = ExpenseState.Error("Session expired. Please log in again.")
                    return@launch
                }

// Converts the ViewModel items to request items to send to the backend
                val itemRequests = _items.value.map { item ->
                    val totalAssignedQty = item.assignedQuantities.values.sum().takeIf { it > 0 } ?: item.assignedTo.size // total quantity assigned across all people
                    ExpenseItemRequest(
                        name = item.name,                    // item name
                        unitPrice = item.unitPrice,          // unit price
                        quantity = item.quantity.toDouble(), // converts Int quantity to Double for the backend
                        totalPrice = item.totalPrice,        // total price
                        assignments = item.assignedTo.map { personName ->
                            val personQty = item.assignedQuantities[personName] ?: 1 // quantity this person is taking
                            val shareAmount = if (totalAssignedQty > 0)
                                (personQty.toDouble() / totalAssignedQty) * item.totalPrice // proportional share
                            else
                                item.totalPrice / item.assignedTo.size // equal split fallback
                            ItemAssignmentRequest(
                                personName = personName,         // the person's name
                                quantity = personQty.toDouble(), // their quantity as Double
                                shareAmount = shareAmount        // how much they owe
                            )
                        }
                    )
                }
                // Builds the request object from the current ViewModel state
                val request = CreateExpenseRequest(
                    groupId = (_expenseType.value as? ExpenseType.ExistingGroup)?.groupId, // gets group ID if applicable
                    amount = _items.value.sumOf { it.totalPrice },                          // calculates total from all items
                    currency = _currency.value,                                             // the selected currency
                    description = _description.value,                                       // the expense description
                    category = _category.value,                                             // the selected category
                    date = _date.value,                                                     // the selected date
                    isOneTimeSplit = _expenseType.value is ExpenseType.OneTimeSplit,        // true if one-time split
                    receiptImages = _receiptImages.value,                                   // the list of receipt image URIs
                    items = itemRequests                                                     // the list of items
                )

                RetrofitClient.authApi.createExpense("Bearer $accessToken", request) // sends the expense to the backend
                _expenseState.value = ExpenseState.Success                           // marks submission as successful
            } catch (e: Exception) {
                _expenseState.value = ExpenseState.Error("Failed to submit: ${e.message}") // shows error if something goes wrong
            }
        }
    }

    // Resets everything back to initial state
    fun resetAll() {
        _currentStep.value = 1
        _isScanning.value = false
        _description.value = ""
        _date.value = ""
        _currency.value = "USD"
        _category.value = ""
        _expenseType.value = ExpenseType.Solo
        _items.value = emptyList()
        _participants.value = emptyList()
        _receiptImages.value = emptyList()
        _expenseState.value = ExpenseState.Idle
        _searchState.value = SearchState.Idle
        _groupsState.value = GroupsState.Idle
    }
}

// Represents the type of expense — solo, existing group, or one-time split
sealed class ExpenseType {
    object Solo : ExpenseType()                                // only affects the user's own budget
    data class ExistingGroup(val groupId: Int) : ExpenseType() // belongs to an existing group
    object OneTimeSplit : ExpenseType()                        // temporary split with manual participants
}

// Represents how much of an item one person is taking
data class PersonQuantity(
    val name: String,  // the person's name
    val quantity: Int  // how many units of the item they are taking
)

// Represents a single item inside the expense e.g. "Pizza"
data class ExpenseItem(
    val id: Int = 0,                                      // unique id for the item
    val name: String = "",                                // item name e.g. "Pizza"
    val unitPrice: Double = 0.0,                          // price per unit
    val quantity: Int = 1,                                // total quantity of this item
    val totalPrice: Double = 0.0,                         // unit price x total quantity
    val assignedTo: List<String> = emptyList(),           // list of participant names assigned to this item
    val assignedQuantities: Map<String, Int> = emptyMap() // maps each person to how many units they take
)

// Represents a participant — either an existing app user or a guest
data class Participant(
    val name: String = "",       // participant name
    val email: String = "",      // optional email
    val isGuest: Boolean = true, // true if they don't have an account
    val userId: String? = null   // user id if they have an account, null if guest
)

// Represents all possible states of the expense submission
sealed class ExpenseState {
    object Idle : ExpenseState()                           // nothing happening yet
    object Loading : ExpenseState()                        // request is in progress
    object Success : ExpenseState()                        // expense was added successfully
    data class Error(val message: String) : ExpenseState() // something went wrong
}

// Represents all possible states of the user search
sealed class SearchState {
    object Idle : SearchState()                                           // no search happening
    object Loading : SearchState()                                        // search is in progress
    data class Results(val users: List<UserSearchResult>) : SearchState() // search returned results
    data class Error(val message: String) : SearchState()                 // search failed
}

// Represents all possible states of the groups fetch
sealed class GroupsState {
    object Idle : GroupsState()                                        // no fetch happening yet
    object Loading : GroupsState()                                     // fetch is in progress
    data class Success(val groups: List<GroupResult>) : GroupsState()  // groups loaded successfully
    data class Error(val message: String) : GroupsState()              // fetch failed
}