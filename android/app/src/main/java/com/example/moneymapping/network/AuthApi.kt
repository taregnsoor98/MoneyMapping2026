package com.example.moneymapping.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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