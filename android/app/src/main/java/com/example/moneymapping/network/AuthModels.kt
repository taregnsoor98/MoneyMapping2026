package com.example.moneymapping.network // this file belongs to the network package

// this is what we SEND to the server when logging in or registering
data class Credentials(
    val username: String, // the username typed by the user
    val password: String  // the password typed by the user
)

// this is what the server SENDS BACK after successful login or register
data class TokenResponse(
    val accessToken: String,  // short-lived token (5 mins), used for API calls
    val refreshToken: String  // long-lived token (14 days), used to get a new access token
)