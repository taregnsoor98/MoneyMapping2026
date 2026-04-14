package com.example.moneymapping.ui.screens

import android.app.Application                              // needed to access context for TokenManager and RetrofitClient
import androidx.lifecycle.AndroidViewModel                  // base class that gives us application context
import androidx.lifecycle.viewModelScope                    // coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.data.TokenManager          // used to read the stored access token
import com.example.moneymapping.network.BalanceResponse    // response object for a single debt between two people
import com.example.moneymapping.network.ExpenseResponse    // response object for an expense
import com.example.moneymapping.network.GroupResponse      // response object for a group
import com.example.moneymapping.network.PayDebtRequest     // request body for marking a debt as instantly paid
import com.example.moneymapping.network.RetrofitClient     // used to make API calls
import com.example.moneymapping.network.SpendingLimitResponse // response object for a spending limit
import com.example.moneymapping.network.SetLimitRequest    // request body for setting a limit
import kotlinx.coroutines.flow.MutableStateFlow            // holds state that can change over time
import kotlinx.coroutines.flow.StateFlow                   // read-only version of MutableStateFlow
import kotlinx.coroutines.launch                           // launches a coroutine
import com.example.moneymapping.network.PaymentPlanResponse // response object for a payment plan

// holds the different states the group detail screen can be in
sealed class GroupDetailState {
    object Loading : GroupDetailState()                                    // currently fetching group
    data class Success(val group: GroupResponse) : GroupDetailState()      // group loaded successfully
    data class Error(val message: String) : GroupDetailState()             // something went wrong
}

class GroupDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using the app context

    // holds the current state of the group detail screen
    private val _state = MutableStateFlow<GroupDetailState>(GroupDetailState.Loading)
    val state: StateFlow<GroupDetailState> = _state // exposed as read-only to the UI

    // holds the current user's ID so we can check their role
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId // exposed as read-only to the UI

    // holds the list of expenses for this group
    private val _expenses = MutableStateFlow<List<ExpenseResponse>>(emptyList())
    val expenses: StateFlow<List<ExpenseResponse>> = _expenses // exposed as read-only to the UI

    // true while expenses are being fetched
    private val _expensesLoading = MutableStateFlow(false)
    val expensesLoading: StateFlow<Boolean> = _expensesLoading // exposed as read-only to the UI

    // holds the list of limits for this group
    private val _limits = MutableStateFlow<List<SpendingLimitResponse>>(emptyList())
    val limits: StateFlow<List<SpendingLimitResponse>> = _limits // exposed as read-only to the UI

    // holds the simplified list of who owes whom in this group
    private val _balances = MutableStateFlow<List<BalanceResponse>>(emptyList())
    val balances: StateFlow<List<BalanceResponse>> = _balances // exposed as read-only to the UI

    // true while balances are being fetched
    private val _balancesLoading = MutableStateFlow(false)
    val balancesLoading: StateFlow<Boolean> = _balancesLoading // exposed as read-only to the UI

    // holds the list of payment plans for this group
    private val _paymentPlans = MutableStateFlow<List<PaymentPlanResponse>>(emptyList())
    val paymentPlans: StateFlow<List<PaymentPlanResponse>> = _paymentPlans // exposed as read-only to the UI

    // fetches all payment plans for this group
    fun fetchPaymentPlans(groupId: Long, token: String? = null) {
        viewModelScope.launch {
            try {
                val accessToken = token ?: tokenManager.getAccessToken() // uses provided token or gets stored one
                ?: return@launch // no token — do nothing
                val result = RetrofitClient.create(getApplication()).getGroupPaymentPlans("Bearer $accessToken", groupId) // calls GET /payment-plans/group/{groupId}
                _paymentPlans.value = result // updates the payment plans list
            } catch (e: Exception) {
                _paymentPlans.value = emptyList() // clears plans on error
            }
        }
    }

    // fetches the group details, expenses, limits and balances by group ID
    fun loadGroup(groupId: Long) {
        viewModelScope.launch {
            _state.value = GroupDetailState.Loading // shows loading state
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _state.value = GroupDetailState.Error("Not logged in") // no token found
                        return@launch
                    }
                _currentUserId.value = tokenManager.getUserId() // gets the current user's ID

                // fetches all groups and finds the one with the matching ID
                val groups = RetrofitClient.create(getApplication()).getGroups("Bearer $token") // calls GET /groups
                val group = groups.find { it.id == groupId } // finds the group by ID
                    ?: run {
                        _state.value = GroupDetailState.Error("Group not found") // group not found
                        return@launch
                    }
                _state.value = GroupDetailState.Success(group) // updates state with loaded group

                // fetches expenses, limits and balances for this group
                fetchExpenses(groupId, token)
                fetchLimits(groupId, token)

                // only fetches balances for FRIEND and ONE_TIME groups — FAMILY groups don't track debts
                if (group.type != "FAMILY") {
                    fetchBalances(groupId, token)
                    fetchPaymentPlans(groupId, token) // fetches payment plans for this group
                }

            } catch (e: Exception) {
                _state.value = GroupDetailState.Error(e.message ?: "Failed to load group") // shows error
            }
        }
    }

    // fetches all expenses for this group
    fun fetchExpenses(groupId: Long, token: String? = null) {
        viewModelScope.launch {
            _expensesLoading.value = true // shows loading state for expenses
            try {
                val accessToken = token ?: tokenManager.getAccessToken() // uses provided token or gets stored one
                ?: return@launch // no token — do nothing
                val result = RetrofitClient.create(getApplication()).getGroupExpenses("Bearer $accessToken", groupId) // calls GET /expenses/group/{groupId}
                _expenses.value = result // updates the expenses list
            } catch (e: Exception) {
                _expenses.value = emptyList() // clears expenses on error
            } finally {
                _expensesLoading.value = false // hides loading state
            }
        }
    }

    // fetches all limits for this group
    fun fetchLimits(groupId: Long, token: String? = null) {
        viewModelScope.launch {
            try {
                val accessToken = token ?: tokenManager.getAccessToken() // uses provided token or gets stored one
                ?: return@launch // no token — do nothing
                val result = RetrofitClient.create(getApplication()).getGroupLimits("Bearer $accessToken", groupId) // calls GET /limits/group/{groupId}
                _limits.value = result // updates the limits list
            } catch (e: Exception) {
                _limits.value = emptyList() // clears limits on error
            }
        }
    }

    // fetches the simplified list of who owes whom for this group
    fun fetchBalances(groupId: Long, token: String? = null) {
        viewModelScope.launch {
            _balancesLoading.value = true // shows loading state for balances
            try {
                val accessToken = token ?: tokenManager.getAccessToken() // uses provided token or gets stored one
                ?: return@launch // no token — do nothing
                val result = RetrofitClient.create(getApplication()).getGroupBalances("Bearer $accessToken", groupId) // calls GET /groups/{id}/balances
                _balances.value = result // updates the balances list
            } catch (e: Exception) {
                _balances.value = emptyList() // clears balances on error
                android.util.Log.e("GroupDetail", "fetchBalances failed: ${e.message}") // logs the error so we can see what went wrong
            } finally {
                _balancesLoading.value = false // hides loading state
            }
        }
    }

    // marks a partial or full debt as settled — refreshes balances after
    fun payDebtInstantly(groupId: Long, fromUserId: String, toUserId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // no token — do nothing
                RetrofitClient.create(getApplication()).payDebt( // calls POST /groups/{id}/balances/pay
                    "Bearer $token",
                    groupId,
                    PayDebtRequest(fromUserId = fromUserId, toUserId = toUserId, amount = amount)
                )
                fetchBalances(groupId) // refreshes the balances after marking as paid
            } catch (e: Exception) {
                // silently fails — we can add error handling later
            }
        }
    }

    // sets or updates a limit for this group
    fun setLimit(groupId: Long, amount: Double, period: String, targetUserId: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // no token — do nothing
                RetrofitClient.create(getApplication()).setGroupLimit( // calls POST /limits/group/{groupId}
                    "Bearer $token",
                    groupId,
                    SetLimitRequest(amount = amount, period = period, targetUserId = targetUserId)
                )
                fetchLimits(groupId) // refreshes the limits after setting
                onSuccess()          // notifies the UI that the limit was set
            } catch (e: Exception) {
                // silently fails — we can add error handling later
            }
        }
    }
}