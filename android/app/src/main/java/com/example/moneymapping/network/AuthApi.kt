package com.example.moneymapping.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("account/register") // tells Retrofit this is a POST request to /account/register
    suspend fun register(@Body request: RegisterRequest): String // sends register request, returns success message

    @POST("account/login") // tells Retrofit this is a POST request to /account/login
    suspend fun login(@Body request: LoginRequest): TokenResponse // sends login request, returns tokens

    @POST("account/refresh") // tells Retrofit this is a POST request to /account/refresh
    suspend fun refresh(@Header("Authorization") token: String): TokenResponse // refreshes the access token

    @GET("account/me") // tells Retrofit this is a GET request to /account/me
    suspend fun getMe(@Header("Authorization") token: String): UserSearchResult // returns the current logged-in user's ID and username

    @GET("account/search") // tells Retrofit this is a GET request to /account/search?query=xxx
    suspend fun searchUsers(@Query("query") query: String): List<UserSearchResult> // searches for users by username or email

    @GET("groups") // tells Retrofit this is a GET request to /groups
    suspend fun getGroups(@Header("Authorization") token: String): List<GroupResponse> // fetches the user's groups

    @POST("groups") // tells Retrofit this is a POST request to /groups
    suspend fun createGroup(@Header("Authorization") token: String, @Body request: CreateGroupRequest): GroupResponse // creates a new group

    @POST("groups/{id}/members") // tells Retrofit this is a POST request to /groups/{id}/members
    suspend fun addMember(@Header("Authorization") token: String, @Path("id") id: Long, @Body request: AddMemberRequest): GroupMemberResponse // adds a member to a group

    @PUT("groups/{id}/members/{memberId}/promote") // tells Retrofit this is a PUT request to promote a member
    suspend fun promoteMember(@Header("Authorization") token: String, @Path("id") id: Long, @Path("memberId") memberId: Long): GroupMemberResponse // promotes a member to admin

    @GET("groups/{id}/balances") // tells Retrofit this is a GET request to /groups/{id}/balances
    suspend fun getGroupBalances(@Header("Authorization") token: String, @Path("id") id: Long): List<BalanceResponse> // fetches the simplified list of who owes whom in a group

    @POST("groups/{id}/balances/pay") // tells Retrofit this is a POST request to /groups/{id}/balances/pay
    suspend fun payDebt(@Header("Authorization") token: String, @Path("id") id: Long, @Body request: PayDebtRequest): String // marks a full debt as instantly settled

    @POST("expenses") // tells Retrofit this is a POST request to /expenses
    suspend fun createExpense(@Header("Authorization") token: String, @Body request: CreateExpenseRequest): ExpenseResponse // saves a new expense to the backend

    @GET("expenses/{id}") // tells Retrofit this is a GET request to /expenses/{id}
    suspend fun getExpense(@Header("Authorization") token: String, @Path("id") id: String): ExpenseResponse // fetches a single expense by its ID

    @GET("expenses") // tells Retrofit this is a GET request to /expenses
    suspend fun getExpenses(@Header("Authorization") token: String): List<ExpenseResponse> // fetches all expenses for the logged-in user

    @PUT("expenses/{id}") // tells Retrofit this is a PUT request to /expenses/{id}
    suspend fun updateExpense(@Header("Authorization") token: String, @Path("id") id: String, @Body request: CreateExpenseRequest): ExpenseResponse // updates an existing expense

    @DELETE("expenses/{id}") // tells Retrofit this is a DELETE request to /expenses/{id}
    suspend fun deleteExpense(@Header("Authorization") token: String, @Path("id") id: String): String // deletes an expense by its ID

    @GET("expenses/group/{groupId}") // tells Retrofit this is a GET request to /expenses/group/{groupId}
    suspend fun getGroupExpenses(@Header("Authorization") token: String, @Path("groupId") groupId: Long): List<ExpenseResponse> // fetches all expenses for a specific group

    @POST("receipt/scan") // tells Retrofit this is a POST request to /receipt/scan
    suspend fun scanReceipt(@Header("Authorization") token: String, @Body request: ScanReceiptRequest): String // sends base64 image to backend and gets back a JSON list of items

    @POST("limits/personal") // tells Retrofit this is a POST request to /limits/personal
    suspend fun setPersonalLimit(@Header("Authorization") token: String, @Body request: SetLimitRequest): SpendingLimitResponse // sets or updates the personal limit

    @GET("limits/personal") // tells Retrofit this is a GET request to /limits/personal
    suspend fun getPersonalLimit(@Header("Authorization") token: String): SpendingLimitResponse // returns the personal limit

    @POST("limits/group/{groupId}") // tells Retrofit this is a POST request to /limits/group/{groupId}
    suspend fun setGroupLimit(@Header("Authorization") token: String, @Path("groupId") groupId: Long, @Body request: SetLimitRequest): SpendingLimitResponse // sets or updates a group limit

    @GET("limits/group/{groupId}") // tells Retrofit this is a GET request to /limits/group/{groupId}
    suspend fun getGroupLimits(@Header("Authorization") token: String, @Path("groupId") groupId: Long): List<SpendingLimitResponse> // returns all limits for a group

    @POST("payment-plans") // tells Retrofit this is a POST request to /payment-plans
    suspend fun createPaymentPlan(@Header("Authorization") token: String, @Body request: CreatePaymentPlanRequest): PaymentPlanResponse // creates a new payment plan and auto-generates installments

    @GET("payment-plans/group/{groupId}") // tells Retrofit this is a GET request to /payment-plans/group/{groupId}
    suspend fun getGroupPaymentPlans(@Header("Authorization") token: String, @Path("groupId") groupId: Long): List<PaymentPlanResponse> // fetches all payment plans for a group

    @POST("payment-plans/{id}/installments/{installmentId}/pay") // tells Retrofit this is a POST request to mark an installment as paid
    suspend fun payInstallment(@Header("Authorization") token: String, @Path("id") id: Long, @Path("installmentId") installmentId: Long): PaymentPlanInstallmentResponse // marks a single installment as paid
}

// the result returned for each matching user from the search endpoint
data class UserSearchResult(
    val id: String,       // the user's unique id
    val username: String, // the user's username
    val email: String     // the user's email
)

// the request body for a single payer sent when creating or updating an expense
data class ExpensePayerRequest(
    val payerName: String,  // the name of the person who paid — username or guest name
    val amountPaid: Double  // how much this person paid toward the expense
)

// the request body for a single assignment sent when creating or updating an expense
data class ItemAssignmentRequest(
    val personName: String,  // the person's name — either a username or guest name
    val quantity: Double,    // how many units of the item this person is taking
    val shareAmount: Double  // how much this person owes for this item
)

// the request body for a single item sent when creating or updating an expense
data class ExpenseItemRequest(
    val name: String,                                          // item name
    val unitPrice: Double,                                     // price per unit
    val quantity: Double,                                      // quantity — Double to support weight-based items
    val totalPrice: Double,                                    // total price for this item
    val assignments: List<ItemAssignmentRequest> = emptyList() // list of people assigned to this item
)

// the request body sent when creating or updating an expense
data class CreateExpenseRequest(
    val groupId: Long? = null,                                  // optional group ID — null for solo expenses
    val amount: Double,                                        // total amount of the expense
    val currency: String,                                      // e.g. "USD"
    val description: String,                                   // what the expense was for
    val category: String,                                      // e.g. "Food"
    val date: String,                                          // date in "yyyy-MM-dd" format
    val isOneTimeSplit: Boolean = false,                        // true if this is a one-time split
    val receiptImages: List<String> = emptyList(),             // optional list of receipt image URIs
    val items: List<ExpenseItemRequest> = emptyList(),         // list of items in this expense
    val payers: List<ExpensePayerRequest> = emptyList()        // list of payers for this expense
)

// the response object for a single payer returned by the backend
data class ExpensePayerResponse(
    val id: String,         // the payer record's unique ID
    val payerName: String,  // the name of the person who paid
    val amountPaid: Double  // how much they paid
)

// the response object for a single assignment returned by the backend
data class ItemAssignmentResponse(
    val id: Long,            // the assignment's unique ID
    val personName: String,  // the person's name
    val quantity: Double,    // how many units they are taking
    val shareAmount: Double  // how much they owe for this item
)

// the response object for a single item returned by the backend
data class ExpenseItemResponse(
    val id: Long,                                              // the item's unique ID
    val name: String,                                          // item name
    val unitPrice: Double,                                     // price per unit
    val quantity: Double,                                      // quantity
    val totalPrice: Double,                                    // total price for this item
    val assignments: List<ItemAssignmentResponse> = emptyList() // list of assignments for this item
)

// the response returned by the backend after creating, updating or fetching an expense
data class ExpenseResponse(
    val id: String,                                        // the expense's unique ID
    val paidBy: String?,                                   // kept for backwards compatibility — may be null
    val groupId: Int?,                                     // optional group ID
    val amount: Double,                                    // total amount
    val currency: String,                                  // e.g. "USD"
    val description: String,                               // what the expense was for
    val category: String,                                  // e.g. "Food"
    val date: String,                                      // date as "yyyy-MM-dd" string
    val isOneTimeSplit: Boolean,                           // true if one-time split
    val receiptImages: List<String>,                       // list of receipt image URIs
    val payers: List<ExpensePayerResponse> = emptyList(),  // list of payers for this expense
    val items: List<ExpenseItemResponse> = emptyList()     // list of items in this expense
)

// the request body sent when scanning a receipt — contains the base64 encoded image
data class ScanReceiptRequest(
    val base64Image: String, // the receipt image encoded as base64
    val mediaType: String    // the image type e.g. "image/jpeg" or "image/png"
)

// the request body sent when creating a group
data class CreateGroupRequest(
    val name: String, // the group name
    val type: String  // FRIEND, FAMILY, or ONE_TIME
)

// the request body sent when adding a member to a group
data class AddMemberRequest(
    val userId: String?,    // the user ID — null if adding a guest
    val guestName: String?  // the guest name — null if adding a real user
)

// the response object for a group member returned by the backend
data class GroupMemberResponse(
    val id: Long,           // the membership ID
    val userId: String?,    // the user ID — null if guest
    val username: String?,  // the username — null if guest
    val guestName: String?, // the guest name — null if real user
    val role: String        // ADMIN or MEMBER
)

// the request body sent when setting a limit
data class SetLimitRequest(
    val amount: Double,              // the limit amount
    val period: String,              // the period — "DAILY", "WEEKLY", or "MONTHLY"
    val targetUserId: String? = null // the user to set the limit for — null for group-wide limit
)

// the response object for a spending limit returned by the backend
data class SpendingLimitResponse(
    val id: Long,                    // the limit's unique ID
    val groupId: Long?,              // the group this limit belongs to — null if personal
    val userId: String?,             // the user this limit belongs to — null if group-wide
    val amount: Double,              // the limit amount
    val period: String               // the period — "DAILY", "WEEKLY", or "MONTHLY"
)

// the response object for a group returned by the backend
data class GroupResponse(
    val id: Long,                          // the group ID
    val name: String,                      // the group name
    val type: String,                      // FRIEND, FAMILY, or ONE_TIME
    val createdBy: String,                 // the user ID of the creator
    val isLocked: Boolean,                 // true if the group is closed and read-only
    val members: List<GroupMemberResponse> // list of members in this group
)

// represents a single simplified debt — one person owes another a specific amount
data class BalanceResponse(
    val from: String,   // the person who owes money
    val to: String,     // the person who receives money
    val amount: Double  // how much is owed
)

// the request body sent when marking a full debt as instantly paid
data class PayDebtRequest(
    val fromUserId: String, // the person who was owing money
    val toUserId: String,    // the person who was owed money
    val amount: Double      // the exact amount being settled right now
)

// the request body sent when creating a payment plan
data class CreatePaymentPlanRequest(
    val groupId: Long,          // the group this plan belongs to
    val fromUserId: String,     // the person who owes money
    val toUserId: String,       // the person who is owed money
    val totalAmount: Double,    // the total debt amount
    val startDate: String,      // the start date as "yyyy-MM-dd"
    val endDate: String,        // the end date as "yyyy-MM-dd"
    val frequency: String       // "DAILY", "WEEKLY", or "MONTHLY"
)

// the response object for a single installment returned by the backend
data class PaymentPlanInstallmentResponse(
    val id: Long,           // the installment's unique ID
    val paymentPlanId: Long, // the ID of the payment plan this installment belongs to
    val dueDate: String,    // the date this installment is due as "yyyy-MM-dd"
    val amount: Double,     // the amount due for this installment
    val isPaid: Boolean,    // true if this installment has been marked as paid
    val paidDate: String?   // the date it was paid — null if not yet paid
)

// the response object for a payment plan returned by the backend — includes all installments
data class PaymentPlanResponse(
    val id: Long,                                           // the payment plan's unique ID
    val groupId: Long,                                      // the group this plan belongs to
    val fromUserId: String,                                 // the person who owes money
    val toUserId: String,                                   // the person who is owed money
    val totalAmount: Double,                                // the total debt amount
    val startDate: String,                                  // the start date
    val endDate: String,                                    // the end date
    val frequency: String,                                  // "DAILY", "WEEKLY", or "MONTHLY"
    val installmentAmount: Double,                          // the amount per installment
    val status: String,                                     // "ACTIVE" or "COMPLETED"
    val installments: List<PaymentPlanInstallmentResponse>  // all installments for this plan
)