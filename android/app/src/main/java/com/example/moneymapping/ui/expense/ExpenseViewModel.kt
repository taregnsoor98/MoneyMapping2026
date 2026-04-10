package com.example.moneymapping.ui.expense

import android.app.Application                              // needed to get the app context for TokenManager
import androidx.lifecycle.AndroidViewModel                  // base class that provides app context unlike regular ViewModel
import androidx.lifecycle.viewModelScope                    // coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.data.TokenManager          // handles saving and reading tokens from DataStore
import com.example.moneymapping.network.CreateExpenseRequest // the request model for creating an expense
import com.example.moneymapping.network.ExpenseItemRequest  // the request model for a single item
import com.example.moneymapping.network.GroupResponse       // the response model for a group
import com.example.moneymapping.network.ItemAssignmentRequest // the request model for a single assignment
import com.example.moneymapping.network.RetrofitClient      // our connection to the server
import com.example.moneymapping.network.UserSearchResult    // the result model for a user search
import kotlinx.coroutines.flow.MutableStateFlow             // holds a value and emits updates when it changes
import kotlinx.coroutines.flow.StateFlow                    // read-only version exposed to the UI
import kotlinx.coroutines.launch                            // launches a coroutine for background work

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using app context to access DataStore

    // tracks the current step in the wizard (1 through 6)
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    // tracks whether the user chose to scan or enter manually
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    // basic expense info fields
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _date = MutableStateFlow("")
    val date: StateFlow<String> = _date

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category

    // tracks whether expense is solo or group
    private val _expenseType = MutableStateFlow<ExpenseType>(ExpenseType.Solo)
    val expenseType: StateFlow<ExpenseType> = _expenseType

    // list of items in the expense
    private val _items = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val items: StateFlow<List<ExpenseItem>> = _items

    // list of participants for group expenses
    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = _participants

    // receipt images picked from gallery
    private val _receiptImages = MutableStateFlow<List<String>>(emptyList())
    val receiptImages: StateFlow<List<String>> = _receiptImages

    // tracks the current submission state
    private val _expenseState = MutableStateFlow<ExpenseState>(ExpenseState.Idle)
    val expenseState: StateFlow<ExpenseState> = _expenseState

    // tracks the current user search state
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    // tracks the current groups fetch state
    private val _groupsState = MutableStateFlow<GroupsState>(GroupsState.Idle)
    val groupsState: StateFlow<GroupsState> = _groupsState

    // holds the group ID if the expense was launched from inside a group — null if launched from home
    private val _launchGroupId = MutableStateFlow<Long?>(null)
    val launchGroupId: StateFlow<Long?> = _launchGroupId

    // true if the wizard was launched from inside a group — skips Step 3
    private val _isFromGroup = MutableStateFlow(false)
    val isFromGroup: StateFlow<Boolean> = _isFromGroup

    // holds the group members if launched from inside a group — used in Step 5
    private val _groupMembers = MutableStateFlow<List<String>>(emptyList())
    val groupMembers: StateFlow<List<String>> = _groupMembers

    // sets up the wizard to be launched from inside a group
    fun launchFromGroup(groupId: Long, memberNames: List<String>) {
        _launchGroupId.value = groupId               // stores the group ID
        _isFromGroup.value = true                    // marks that we launched from a group
        _groupMembers.value = memberNames            // stores the group member names
        _expenseType.value = ExpenseType.ExistingGroup(groupId) // sets the expense type to existing group
    }

    // moves to the next step in the wizard
    fun nextStep() {
        if (_currentStep.value < 6) _currentStep.value++ // increments step if not at the last
    }

    // moves to the previous step in the wizard
    fun previousStep() {
        if (_currentStep.value > 1) _currentStep.value-- // decrements step if not at the first
    }

    // sets whether the user is scanning or entering manually
    fun setScanning(scanning: Boolean) {
        _isScanning.value = scanning
    }

    // updates the description field
    fun setDescription(value: String) { _description.value = value }

    // updates the date field
    fun setDate(value: String) { _date.value = value }

    // updates the currency field
    fun setCurrency(value: String) { _currency.value = value }

    // updates the category field
    fun setCategory(value: String) { _category.value = value }

    // updates the expense type — solo or group
    fun setExpenseType(type: ExpenseType) { _expenseType.value = type }

    // adds a new item to the expense items list
    fun addItem(item: ExpenseItem) {
        _items.value = _items.value + item // appends the new item to the list
    }

    // updates an existing item in the list by its index
    fun updateItem(index: Int, item: ExpenseItem) {
        val updated = _items.value.toMutableList()
        updated[index] = item // replaces the item at the given index
        _items.value = updated
    }

    // removes an item from the list by its index
    fun removeItem(index: Int) {
        val updated = _items.value.toMutableList()
        updated.removeAt(index) // removes the item at the given index
        _items.value = updated
    }

    // adds a participant to the list
    fun addParticipant(participant: Participant) {
        _participants.value = _participants.value + participant // appends participant to list
    }

    // removes a participant by index
    fun removeParticipant(index: Int) {
        val updated = _participants.value.toMutableList()
        updated.removeAt(index) // removes participant at given index
        _participants.value = updated
    }

    // adds receipt image URIs to the list
    fun addReceiptImages(uris: List<String>) {
        _receiptImages.value = _receiptImages.value + uris // appends new images to existing list
    }

    // resets the search state
    fun resetSearch() {
        _searchState.value = SearchState.Idle // resets search state to idle
    }

    // fetches the user's groups from the backend using the stored token
    fun fetchGroups() {
        viewModelScope.launch {
            _groupsState.value = GroupsState.Loading // shows loading while fetching
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // stops if no token is found
                val groups = RetrofitClient.create(getApplication()).getGroups("Bearer $accessToken") // calls the groups endpoint with token
                _groupsState.value = GroupsState.Success(groups) // updates state with fetched groups
            } catch (e: Exception) {
                _groupsState.value = GroupsState.Error("Could not load groups: ${e.message}") // shows error if fetch fails
            }
        }
    }

    // searches for existing users by username or email
    fun searchUsers(query: String) {
        if (query.length < 2) { // require at least 2 characters before searching
            _searchState.value = SearchState.Idle // resets search state if query is too short
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading // shows loading while searching
            try {
                val results = RetrofitClient.create(getApplication()).searchUsers(query) // calls the search endpoint
                _searchState.value = SearchState.Results(results) // updates state with search results
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("Search failed: ${e.message}") // shows error if search fails
            }
        }
    }

    // calculates how much each person owes based on item assignments and split mode
    fun calculateShares(): Map<String, Double> {
        val shares = mutableMapOf<String, Double>() // maps participant name to amount owed
        _items.value.forEach { item ->
            if (item.assignedTo.isNotEmpty()) {
                when (item.splitMode) {
                    SplitMode.BY_PERCENTAGE -> {
                        // calculates share based on percentage each person owns
                        if (item.assignedPercentages.isNotEmpty()) {
                            item.assignedPercentages.forEach { (person, percentage) ->
                                val share = (percentage / 100.0) * item.totalPrice // percentage share
                                shares[person] = (shares[person] ?: 0.0) + share // adds to their total
                            }
                        } else {
                            // falls back to equal split if no percentages are specified
                            val sharePerPerson = item.totalPrice / item.assignedTo.size // equal split
                            item.assignedTo.forEach { person ->
                                shares[person] = (shares[person] ?: 0.0) + sharePerPerson // adds to their total
                            }
                        }
                    }
                    SplitMode.BY_QUANTITY -> {
                        if (item.assignedQuantities.isNotEmpty()) {
                            // calculates share based on quantity each person is taking
                            val totalAssignedQty = item.assignedQuantities.values.sum() // total assigned quantity
                            item.assignedQuantities.forEach { (person, qty) ->
                                val share = (qty.toDouble() / totalAssignedQty) * item.totalPrice // proportional share
                                shares[person] = (shares[person] ?: 0.0) + share // adds to their total
                            }
                        } else {
                            // falls back to equal split if no quantities are specified
                            val sharePerPerson = item.totalPrice / item.assignedTo.size // equal split
                            item.assignedTo.forEach { person ->
                                shares[person] = (shares[person] ?: 0.0) + sharePerPerson // adds to their total
                            }
                        }
                    }
                }
            }
        }
        return shares
    }

    // submits the expense — sends expense data and items to the backend
    fun submitExpense() {
        viewModelScope.launch {
            _expenseState.value = ExpenseState.Loading // shows loading while submitting
            try {
                val accessToken = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _expenseState.value = ExpenseState.Error("Session expired. Please log in again.")
                        return@launch
                    }

                // converts the ViewModel items to request items to send to the backend
                val itemRequests = _items.value.map { item ->
                    ExpenseItemRequest(
                        name = item.name,                    // item name
                        unitPrice = item.unitPrice,          // unit price
                        quantity = item.quantity.toDouble(), // converts Int quantity to Double for the backend
                        totalPrice = item.totalPrice,        // total price
                        assignments = item.assignedTo.map { personName ->
                            val shareAmount = when (item.splitMode) {
                                SplitMode.BY_PERCENTAGE -> {
                                    val percentage = item.assignedPercentages[personName] ?: (100.0 / item.assignedTo.size) // uses percentage or equal split
                                    (percentage / 100.0) * item.totalPrice // calculates share from percentage
                                }
                                SplitMode.BY_QUANTITY -> {
                                    val totalAssignedQty = item.assignedQuantities.values.sum().takeIf { it > 0 } ?: item.assignedTo.size // total quantity assigned
                                    val personQty = item.assignedQuantities[personName] ?: 1 // quantity this person is taking
                                    (personQty.toDouble() / totalAssignedQty) * item.totalPrice // proportional share
                                }
                            }
                            val personQty = item.assignedQuantities[personName] ?: 1 // quantity this person is taking
                            ItemAssignmentRequest(
                                personName = personName,         // the person's name
                                quantity = personQty.toDouble(), // their quantity as Double
                                shareAmount = shareAmount        // how much they owe
                            )
                        }
                    )
                }

                // builds the request object from the current ViewModel state
                val request = CreateExpenseRequest(
                    groupId = (_expenseType.value as? ExpenseType.ExistingGroup)?.groupId, // gets group ID if applicable
                    amount = _items.value.sumOf { it.totalPrice },                         // calculates total from all items
                    currency = _currency.value,                                            // the selected currency
                    description = _description.value,                                      // the expense description
                    category = _category.value,                                            // the selected category
                    date = _date.value,                                                    // the selected date
                    isOneTimeSplit = false,                                                 // always false — one-time splits handled through ONE_TIME groups
                    receiptImages = _receiptImages.value,                                  // the list of receipt image URIs
                    items = itemRequests                                                    // the list of items
                )

                RetrofitClient.create(getApplication()).createExpense("Bearer $accessToken", request) // sends the expense to the backend
                _expenseState.value = ExpenseState.Success // marks submission as successful
            } catch (e: Exception) {
                _expenseState.value = ExpenseState.Error("Failed to submit: ${e.message}") // shows error if something goes wrong
            }
        }
    }

    // resets everything back to initial state
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
        _launchGroupId.value = null              // clears the launch group ID
        _isFromGroup.value = false               // clears the from group flag
        _groupMembers.value = emptyList()        // clears the group members
    }
}

// represents the type of expense — solo or existing group
sealed class ExpenseType {
    object Solo : ExpenseType()                                 // only affects the user's own budget
    data class ExistingGroup(val groupId: Long) : ExpenseType() // belongs to an existing group
}

// represents how much of an item one person is taking
data class PersonQuantity(
    val name: String,  // the person's name
    val quantity: Int  // how many units of the item they are taking
)

// represents the split mode for an item
enum class SplitMode {
    BY_QUANTITY,   // split based on how many units each person takes
    BY_PERCENTAGE  // split based on what percentage each person owns
}

// represents a single item inside the expense e.g. "Pizza"
data class ExpenseItem(
    val id: Int = 0,                                           // unique id for the item
    val name: String = "",                                     // item name e.g. "Pizza"
    val unitPrice: Double = 0.0,                               // price per unit
    val quantity: Int = 1,                                     // total quantity of this item
    val totalPrice: Double = 0.0,                              // unit price x total quantity
    val assignedTo: List<String> = emptyList(),                // list of participant names assigned to this item
    val assignedQuantities: Map<String, Int> = emptyMap(),     // maps each person to how many units they take
    val splitMode: SplitMode = SplitMode.BY_QUANTITY,          // the split mode for this item
    val assignedPercentages: Map<String, Double> = emptyMap()  // maps each person to their percentage share
)

// represents a participant — either an existing app user or a guest
data class Participant(
    val name: String = "",       // participant name
    val email: String = "",      // optional email
    val isGuest: Boolean = true, // true if they don't have an account
    val userId: String? = null   // user id if they have an account, null if guest
)

// represents all possible states of the expense submission
sealed class ExpenseState {
    object Idle : ExpenseState()                            // nothing happening yet
    object Loading : ExpenseState()                         // request is in progress
    object Success : ExpenseState()                         // expense was added successfully
    data class Error(val message: String) : ExpenseState()  // something went wrong
}

// represents all possible states of the user search
sealed class SearchState {
    object Idle : SearchState()                                            // no search happening
    object Loading : SearchState()                                         // search is in progress
    data class Results(val users: List<UserSearchResult>) : SearchState()  // search returned results
    data class Error(val message: String) : SearchState()                  // search failed
}

// represents all possible states of the groups fetch
sealed class GroupsState {
    object Idle : GroupsState()                                          // no fetch happening yet
    object Loading : GroupsState()                                       // fetch is in progress
    data class Success(val groups: List<GroupResponse>) : GroupsState()  // groups loaded successfully
    data class Error(val message: String) : GroupsState()                // fetch failed
}