package com.example.moneymapping.network // this file belongs to the network package

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("Account/login") // tells Retrofit this is a POST request to /Account/login
    suspend fun login(@Body credentials: Credentials): TokenResponse
    // suspend = runs in background, @Body = sends credentials as JSON, returns TokenResponse

    @POST("Account/register") // tells Retrofit this is a POST request to /Account/register
    suspend fun register(@Body credentials: Credentials): TokenResponse
    // same as login but creates a new account first
}