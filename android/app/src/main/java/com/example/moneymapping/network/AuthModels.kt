package com.example.moneymapping.network

// what we send to the server when registering
data class RegisterRequest(
    val email: String, // the email address
    val username: String, // the chosen username
    val password: String // the chosen password
)

// what we send to the server when logging in
data class LoginRequest(
    val emailOrUsername: String, // accepts either email or username
    val password: String // the password
)

// what the server sends back after successful login
data class TokenResponse(
    val accessToken: String, // short-lived token (5 mins), used for API calls
    val refreshToken: String // long-lived token (14 days), used to get a new access token
)