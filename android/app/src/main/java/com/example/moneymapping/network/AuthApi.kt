package com.example.moneymapping.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("account/register") // tells Retrofit this is a POST request to /account/register
    suspend fun register(@Body request: RegisterRequest): String // sends register request, returns success message

    @POST("account/login") // tells Retrofit this is a POST request to /account/login
    suspend fun login(@Body request: LoginRequest): TokenResponse // sends login request, returns tokens

    @POST("account/refresh") // tells Retrofit this is a POST request to /account/refresh
    suspend fun refresh(@Header("Authorization") token: String): TokenResponse // refreshes the access token
}