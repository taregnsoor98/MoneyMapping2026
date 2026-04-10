package com.example.moneymapping.ui.screens

import android.app.Application                              // needed to access context for TokenManager and RetrofitClient
import androidx.lifecycle.AndroidViewModel                  // base class that gives us application context
import androidx.lifecycle.viewModelScope                    // coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.data.TokenManager          // used to read the stored access token
import com.example.moneymapping.network.AddMemberRequest   // request body for adding a member
import com.example.moneymapping.network.CreateGroupRequest // request body for creating a group
import com.example.moneymapping.network.GroupResponse      // response object for a group
import com.example.moneymapping.network.RetrofitClient     // used to make API calls
import kotlinx.coroutines.flow.MutableStateFlow            // holds state that can change over time
import kotlinx.coroutines.flow.StateFlow                   // read-only version of MutableStateFlow
import kotlinx.coroutines.launch                           // launches a coroutine

// holds the different states the groups screen can be in
sealed class GroupScreenState {
    object Loading : GroupScreenState()                        // currently fetching groups
    data class Success(val groups: List<GroupResponse>) : GroupScreenState() // groups loaded successfully
    data class Error(val message: String) : GroupScreenState() // something went wrong
}

class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using the app context

    // holds the current state of the groups screen
    private val _state = MutableStateFlow<GroupScreenState>(GroupScreenState.Loading)
    val state: StateFlow<GroupScreenState> = _state // exposed as read-only to the UI

    init {
        fetchGroups() // fetches groups as soon as the ViewModel is created
    }

    // fetches all groups the logged-in user belongs to
    fun fetchGroups() {
        viewModelScope.launch { // launches a coroutine in the ViewModel's scope
            _state.value = GroupScreenState.Loading // shows loading state
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _state.value = GroupScreenState.Error("Not logged in") // no token found
                        return@launch
                    }
                val groups = RetrofitClient.create(getApplication()).getGroups("Bearer $token") // calls GET /groups
                _state.value = GroupScreenState.Success(groups) // updates state with loaded groups
            } catch (e: Exception) {
                _state.value = GroupScreenState.Error(e.message ?: "Failed to load groups") // shows error
            }
        }
    }

    // creates a new group with the given name and type
    fun createGroup(name: String, type: String, onSuccess: () -> Unit) {
        viewModelScope.launch { // launches a coroutine in the ViewModel's scope
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // no token — do nothing
                RetrofitClient.create(getApplication()).createGroup( // calls POST /groups
                    "Bearer $token",
                    CreateGroupRequest(name = name, type = type) // sends group name and type
                )
                fetchGroups()  // refreshes the groups list after creating
                onSuccess()    // notifies the UI that creation was successful
            } catch (e: Exception) {
                _state.value = GroupScreenState.Error(e.message ?: "Failed to create group") // shows error
            }
        }
    }

    // adds a member to a group — either a real user by userId or a guest by name
    fun addMember(groupId: Long, userId: String?, guestName: String?, onSuccess: () -> Unit) {
        viewModelScope.launch { // launches a coroutine in the ViewModel's scope
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // no token — do nothing
                RetrofitClient.create(getApplication()).addMember( // calls POST /groups/{id}/members
                    "Bearer $token",
                    groupId,
                    AddMemberRequest(userId = userId, guestName = guestName) // sends member info
                )
                fetchGroups() // refreshes the groups list after adding member
                onSuccess()   // notifies the UI that the member was added
            } catch (e: Exception) {
                _state.value = GroupScreenState.Error(e.message ?: "Failed to add member") // shows error
            }
        }
    }

    // promotes a member to admin — only works in FAMILY groups
    fun promoteMember(groupId: Long, memberId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch { // launches a coroutine in the ViewModel's scope
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // no token — do nothing
                RetrofitClient.create(getApplication()).promoteMember( // calls PUT /groups/{id}/members/{memberId}/promote
                    "Bearer $token",
                    groupId,
                    memberId
                )
                fetchGroups() // refreshes the groups list after promoting
                onSuccess()   // notifies the UI that the member was promoted
            } catch (e: Exception) {
                _state.value = GroupScreenState.Error(e.message ?: "Failed to promote member") // shows error
            }
        }
    }
}