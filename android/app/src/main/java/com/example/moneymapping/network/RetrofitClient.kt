package com.example.moneymapping.network

import android.content.Context                                  // needed to access TokenManager which requires app context
import com.example.moneymapping.data.TokenManager              // used to read and save tokens
import okhttp3.OkHttpClient                                    // allows us to customize the HTTP client settings
import retrofit2.Retrofit                                      // the main Retrofit library
import retrofit2.converter.gson.GsonConverterFactory           // converts JSON to Kotlin objects automatically
import retrofit2.converter.scalars.ScalarsConverterFactory     // converts plain text responses to String
import java.util.concurrent.TimeUnit                           // used to set timeout durations

object RetrofitClient {

    private const val BASE_URL = "http://192.168.31.216:8080/" // address of your laptop on the local WiFi network

    // Creates the Retrofit client with the TokenAuthenticator plugged in
    // Context is needed so TokenAuthenticator can access DataStore through TokenManager
    fun create(context: Context): AuthApi {

        val tokenManager = TokenManager(context) // creates TokenManager using the app context

        // Creates a basic Retrofit instance used ONLY for the refresh call inside TokenAuthenticator
        // This one has NO authenticator attached — to avoid an infinite refresh loop
        val refreshApi = Retrofit.Builder()
            .baseUrl(BASE_URL)                                      // sets the server address
            .addConverterFactory(ScalarsConverterFactory.create())  // handles plain text responses
            .addConverterFactory(GsonConverterFactory.create())     // handles JSON responses
            .build()
            .create(AuthApi::class.java)                            // creates the AuthApi for refresh calls only

        val authenticator = TokenAuthenticator(tokenManager, refreshApi) // creates the authenticator with token manager and refresh api

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)   // wait up to 30 seconds to connect to the server
            .readTimeout(30, TimeUnit.SECONDS)      // wait up to 30 seconds to read the server response
            .writeTimeout(30, TimeUnit.SECONDS)     // wait up to 30 seconds to send data to the server
            .authenticator(authenticator)           // plugs in the token authenticator for automatic refresh on 401
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)                                      // sets the server address
            .client(okHttpClient)                                   // uses our custom client with the authenticator
            .addConverterFactory(ScalarsConverterFactory.create())  // handles plain text responses
            .addConverterFactory(GsonConverterFactory.create())     // handles JSON responses
            .build()
            .create(AuthApi::class.java)                            // creates the main AuthApi used throughout the app
    }
}