package com.example.moneymapping.network

import okhttp3.OkHttpClient // allows us to customize the HTTP client settings
import retrofit2.Retrofit // the main Retrofit library
import retrofit2.converter.gson.GsonConverterFactory // converts JSON to Kotlin objects automatically
import retrofit2.converter.scalars.ScalarsConverterFactory // converts plain text responses to String
import java.util.concurrent.TimeUnit // used to set timeout durations

object RetrofitClient {

    private const val BASE_URL = "http://192.168.31.216:8080/" // address of your laptop on the local WiFi network

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // wait up to 30 seconds to connect to the server
        .readTimeout(30, TimeUnit.SECONDS) // wait up to 30 seconds to read the server response
        .writeTimeout(30, TimeUnit.SECONDS) // wait up to 30 seconds to send data to the server
        .build()

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // set the server address
            .client(okHttpClient) // use our custom HTTP client with longer timeouts
            .addConverterFactory(ScalarsConverterFactory.create()) // handles plain text responses like register success message
            .addConverterFactory(GsonConverterFactory.create()) // handles JSON responses like login tokens
            .build()
            .create(AuthApi::class.java) // create the AuthApi interface so we can call its functions
    }
}