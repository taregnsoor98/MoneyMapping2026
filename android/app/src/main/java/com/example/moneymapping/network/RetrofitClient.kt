package com.example.moneymapping.network // this file belongs to the network package

import retrofit2.Retrofit // the main Retrofit library
import retrofit2.converter.gson.GsonConverterFactory // converts JSON to Kotlin objects automatically

object RetrofitClient { // object means there is only one instance of this, shared across the app

    private const val BASE_URL = "http://192.168.31.216:8080/" // address of your laptop on the local wifi network

    val authApi: AuthApi by lazy { // lazy means it's only created when first needed
        Retrofit.Builder() // start building the Retrofit client
            .baseUrl(BASE_URL) // set the server address
            .addConverterFactory(GsonConverterFactory.create()) // use Gson to handle JSON automatically
            .build() // build the Retrofit instance
            .create(AuthApi::class.java) // create the AuthApi interface so we can call its functions
    }
}