package com.example.moneymapping.ui.screens

import android.app.Application                              // needed to access context for TokenManager and RetrofitClient
import androidx.lifecycle.AndroidViewModel                  // base class that gives us application context
import androidx.lifecycle.viewModelScope                    // coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.data.TokenManager          // used to read the stored access token
import com.example.moneymapping.network.AddMemberRequest   // request body for adding a member
import com.example.moneymapping.network.CreateGroupRequest // request body for creating a group
import com.example.moneymapping.network.RetrofitClient     // used to make API calls
import com.example.moneymapping.network.UserSearchResult   // result object for user search
import kotlinx.coroutines.flow.MutableStateFlow            // holds state that can change over time
import kotlinx.coroutines.flow.StateFlow                   // read-only version of MutableStateFlow
import kotlinx.coroutines.launch                           // launches a coroutine

// represents a member being added to the group during creation
data class PendingMember(
    val userId: String?,          // the user ID — null if guest
    val username: String,         // the display name — either username or guest name
    val isGuest: Boolean,         // true if this member has no account
    val isAdmin: Boolean = false, // true if this member is an admin — only relevant for FAMILY groups
    val limitAmount: String = ""  // the personal limit amount assigned to this member
)

class CreateGroupViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using the app context

    // holds the current step number — starts at step 1
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    // holds the typed group name
    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

    // holds the selected group type — defaults to FRIEND
    private val _groupType = MutableStateFlow("FRIEND")
    val groupType: StateFlow<String> = _groupType

    // holds the selected group currency — defaults to USD
    private val _groupCurrency = MutableStateFlow("USD")
    val groupCurrency: StateFlow<String> = _groupCurrency

    // holds the list of members being added
    private val _members = MutableStateFlow<List<PendingMember>>(emptyList())
    val members: StateFlow<List<PendingMember>> = _members

    // holds the search results from the user search
    private val _searchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val searchResults: StateFlow<List<UserSearchResult>> = _searchResults

    // true while searching for users
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // holds the group limit amount as a string for the text field
    private val _groupLimitAmount = MutableStateFlow("")
    val groupLimitAmount: StateFlow<String> = _groupLimitAmount

    // holds the selected limit period — defaults to MONTHLY
    private val _groupLimitPeriod = MutableStateFlow("MONTHLY")
    val groupLimitPeriod: StateFlow<String> = _groupLimitPeriod

    // true while creating the group
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // holds any error message to show the user
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // moves to the next step
    fun nextStep() {
        _currentStep.value += 1 // increments the step counter
    }

    // moves to the previous step
    fun previousStep() {
        _currentStep.value -= 1 // decrements the step counter
    }

    // updates the group name
    fun setGroupName(name: String) {
        _groupName.value = name // stores the typed name
    }

    // updates the group type
    fun setGroupType(type: String) {
        _groupType.value = type // stores the selected type
    }

    // updates the group currency
    fun setGroupCurrency(currency: String) {
        _groupCurrency.value = currency // stores the selected currency
    }

    // updates the group limit amount
    fun setGroupLimitAmount(amount: String) {
        _groupLimitAmount.value = amount // stores the typed amount
    }

    // updates the group limit period
    fun setGroupLimitPeriod(period: String) {
        _groupLimitPeriod.value = period // stores the selected period
    }

    // searches for users by username or email — filters out the current logged-in user
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList() // clears results if query is empty
            return
        }
        viewModelScope.launch {
            _isSearching.value = true // shows loading spinner
            try {
                val currentUserId = tokenManager.getUserId() // gets the current user's ID from DataStore
                val results = RetrofitClient.create(getApplication()).searchUsers(query) // calls the search endpoint
                _searchResults.value = results.filter { it.id != currentUserId } // filters out the current user
            } catch (e: Exception) {
                _searchResults.value = emptyList() // clears results on error
            } finally {
                _isSearching.value = false // hides loading spinner
            }
        }
    }

    // adds a member to the pending list
    fun addMember(userId: String?, username: String, isGuest: Boolean) {
        val already = _members.value.any { it.userId == userId && it.username == username } // checks for duplicates
        if (!already) {
            _members.value = _members.value + PendingMember( // adds new member to the list
                userId = userId,
                username = username,
                isGuest = isGuest
            )
        }
        _searchResults.value = emptyList() // clears search results after adding
    }

    // removes a member from the pending list
    fun removeMember(member: PendingMember) {
        _members.value = _members.value.filter { it != member } // removes the member from the list
    }

    // toggles the admin role for a member — only used in FAMILY groups
    fun toggleAdmin(member: PendingMember) {
        _members.value = _members.value.map {
            if (it == member) it.copy(isAdmin = !it.isAdmin) // flips the admin flag for this member
            else it // leaves other members unchanged
        }
    }

    // updates the personal limit for a specific member
    fun setMemberLimit(member: PendingMember, amount: String) {
        _members.value = _members.value.map {
            if (it == member) it.copy(limitAmount = amount) // updates the limit for this member
            else it // leaves other members unchanged
        }
    }

    // creates the group and adds all members
    fun createGroup(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true  // shows loading spinner
            _error.value = null      // clears any previous error
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _error.value = "Not logged in" // no token found
                        _isLoading.value = false
                        return@launch
                    }

                // creates the group on the backend
                val group = RetrofitClient.create(getApplication()).createGroup(
                    "Bearer $token",
                    CreateGroupRequest(
                        name = _groupName.value,         // the group name
                        type = _groupType.value          // the group type
                    )
                )

                // adds each pending member to the group
                _members.value.forEach { member ->
                    RetrofitClient.create(getApplication()).addMember(
                        "Bearer $token",
                        group.id,
                        AddMemberRequest(
                            userId = member.userId,                                            // null if guest
                            guestName = if (member.isGuest) member.username else null          // guest name or null
                        )
                    )
                }

                onSuccess?.invoke() // notifies the UI that creation was successful
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create group" // shows error message
            } finally {
                _isLoading.value = false // hides loading spinner
            }
        }
    }
}