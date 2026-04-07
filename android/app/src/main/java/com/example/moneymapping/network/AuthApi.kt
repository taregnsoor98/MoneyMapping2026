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

    @GET("account/search") // tells Retrofit this is a GET request to /account/search?query=xxx
    suspend fun searchUsers(@Query("query") query: String): List<UserSearchResult> // searches for users by username or email

    @GET("groups") // tells Retrofit this is a GET request to /groups
    suspend fun getGroups(@Header("Authorization") token: String): List<GroupResult> // fetches the user's groups

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

    @POST("receipt/scan") // tells Retrofit this is a POST request to /receipt/scan
    suspend fun scanReceipt(@Header("Authorization") token: String, @Body request: ScanReceiptRequest): String // sends base64 image to backend and gets back a JSON list of items
}

// the result returned for each matching user from the search endpoint
data class UserSearchResult(
    val id: String,       // the user's unique id
    val username: String, // the user's username
    val email: String     // the user's email
)

// the result returned for each group the user belongs to
data class GroupResult(
    val id: Int,          // the group's unique id
    val name: String,     // the group's display name
    val type: String      // the group type — "family", "friends", or "one_time"
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
    val groupId: Int? = null,                          // optional group ID — null for solo expenses
    val amount: Double,                                // total amount of the expense
    val currency: String,                              // e.g. "USD"
    val description: String,                           // what the expense was for
    val category: String,                              // e.g. "Food"
    val date: String,                                  // date in "yyyy-MM-dd" format
    val isOneTimeSplit: Boolean = false,                // true if this is a one-time split
    val receiptImages: List<String> = emptyList(),     // optional list of receipt image URIs
    val items: List<ExpenseItemRequest> = emptyList()  // list of items in this expense
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
    val paidBy: String,                                    // the user ID of whoever paid
    val groupId: Int?,                                     // optional group ID
    val amount: Double,                                    // total amount
    val currency: String,                                  // e.g. "USD"
    val description: String,                               // what the expense was for
    val category: String,                                  // e.g. "Food"
    val date: String,                                      // date as "yyyy-MM-dd" string
    val isOneTimeSplit: Boolean,                           // true if one-time split
    val receiptImages: List<String>,                       // list of receipt image URIs
    val items: List<ExpenseItemResponse> = emptyList()     // list of items in this expense
)

// the request body sent when scanning a receipt — contains the base64 encoded image
data class ScanReceiptRequest(
    val base64Image: String, // the receipt image encoded as base64
    val mediaType: String    // the image type e.g. "image/jpeg" or "image/png"
)