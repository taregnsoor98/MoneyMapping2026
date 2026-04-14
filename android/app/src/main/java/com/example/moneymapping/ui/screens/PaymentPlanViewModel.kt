package com.example.moneymapping.ui.screens

import android.app.Application                                      // needed to access context for TokenManager and RetrofitClient
import androidx.lifecycle.AndroidViewModel                          // base class that gives us application context
import androidx.lifecycle.viewModelScope                            // coroutine scope tied to this ViewModel's lifecycle
import com.example.moneymapping.data.TokenManager                  // used to read the stored access token
import com.example.moneymapping.network.CreatePaymentPlanRequest   // request body for creating a payment plan
import com.example.moneymapping.network.PaymentPlanInstallmentResponse // response object for a single installment
import com.example.moneymapping.network.PaymentPlanResponse        // response object for a payment plan
import com.example.moneymapping.network.RetrofitClient             // used to make API calls
import kotlinx.coroutines.flow.MutableStateFlow                    // holds state that can change over time
import kotlinx.coroutines.flow.StateFlow                           // read-only version of MutableStateFlow
import kotlinx.coroutines.launch                                   // launches a coroutine

class PaymentPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application) // creates TokenManager using the app context

    // holds the current payment plan — null until one is created or loaded
    private val _plan = MutableStateFlow<PaymentPlanResponse?>(null)
    val plan: StateFlow<PaymentPlanResponse?> = _plan // exposed as read-only to the UI

    // true while a plan is being created or loaded
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading // exposed as read-only to the UI

    // holds an error message if something goes wrong — null if no error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error // exposed as read-only to the UI

    // creates a new payment plan and auto-generates its installments
    fun createPaymentPlan(
        groupId: Long,       // the group this plan belongs to
        fromUserId: String,  // the person who owes money
        toUserId: String,    // the person who is owed money
        totalAmount: Double, // the total debt amount
        startDate: String,   // the start date as "yyyy-MM-dd"
        endDate: String,     // the end date as "yyyy-MM-dd"
        frequency: String,   // "DAILY", "WEEKLY", or "MONTHLY"
        onSuccess: (Long) -> Unit // called with the new plan's ID when creation succeeds
    ) {
        viewModelScope.launch {
            _isLoading.value = true  // shows loading state
            _error.value = null      // clears any previous error
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _error.value = "Not logged in" // no token found
                        return@launch
                    }

                val result = RetrofitClient.create(getApplication()).createPaymentPlan( // calls POST /payment-plans
                    "Bearer $token",
                    CreatePaymentPlanRequest(
                        groupId = groupId,           // the group this plan belongs to
                        fromUserId = fromUserId,     // the debtor
                        toUserId = toUserId,         // the creditor
                        totalAmount = totalAmount,   // total debt
                        startDate = startDate,       // start date
                        endDate = endDate,           // end date
                        frequency = frequency        // DAILY, WEEKLY, or MONTHLY
                    )
                )

                _plan.value = result                          // stores the created plan
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(result.id)                      // calls navigation on the main thread so Compose can handle it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create payment plan" // shows error
            } finally {
                _isLoading.value = false // hides loading state
            }
        }
    }

    // loads a payment plan by fetching all plans for a group and finding the matching one
    fun loadPlan(planId: Long, groupId: Long) {
        viewModelScope.launch {
            _isLoading.value = true  // shows loading state
            _error.value = null      // clears any previous error
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: run {
                        _error.value = "Not logged in" // no token found
                        return@launch
                    }

                val plans = RetrofitClient.create(getApplication()).getGroupPaymentPlans( // calls GET /payment-plans/group/{groupId}
                    "Bearer $token",
                    groupId
                )

                val match = plans.find { it.id == planId } // finds the plan with the matching ID
                if (match != null) {
                    _plan.value = match // stores the found plan
                } else {
                    _error.value = "Payment plan not found" // plan not found
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load payment plan" // shows error
            } finally {
                _isLoading.value = false // hides loading state
            }
        }
    }

    // marks a single installment as paid and refreshes the plan
    fun payInstallment(planId: Long, installmentId: Long, groupId: Long) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken() // gets the stored access token
                    ?: return@launch // no token — do nothing

                RetrofitClient.create(getApplication()).payInstallment( // calls POST /payment-plans/{id}/installments/{installmentId}/pay
                    "Bearer $token",
                    planId,
                    installmentId
                )

                loadPlan(planId, groupId) // refreshes the plan after marking installment as paid
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to mark installment as paid" // shows error
            }
        }
    }
}