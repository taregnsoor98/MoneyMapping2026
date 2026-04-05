package com.example.moneymapping.data

import android.content.Context // needed to access the app's storage
import androidx.datastore.core.DataStore // the DataStore instance
import androidx.datastore.preferences.core.Preferences // the preferences type
import androidx.datastore.preferences.core.edit // allows editing stored values
import androidx.datastore.preferences.core.stringPreferencesKey // creates a string key for storage
import androidx.datastore.preferences.preferencesDataStore // creates the DataStore instance
import kotlinx.coroutines.flow.Flow // allows observing stored values as a stream
import kotlinx.coroutines.flow.first // gets the first value from a flow
import kotlinx.coroutines.flow.map // transforms flow values

// Creates a single DataStore instance tied to the app context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

class TokenManager(private val context: Context) { // takes app context to access DataStore

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")   // key for storing access token
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token") // key for storing refresh token
    }

    // Saves both tokens to DataStore after successful login
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken   // stores the access token
            preferences[REFRESH_TOKEN_KEY] = refreshToken // stores the refresh token
        }
    }

    // Returns the access token as a Flow so it can be observed
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY] // reads the access token
    }

    // Returns the refresh token as a Flow so it can be observed
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY] // reads the refresh token
    }

    // Gets the current access token directly (not as a flow)
    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY] // reads access token once
    }

    // Gets the current refresh token directly (not as a flow)
    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[REFRESH_TOKEN_KEY] // reads refresh token once
    }

    // Clears both tokens from storage — used when logging out
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)   // removes access token
            preferences.remove(REFRESH_TOKEN_KEY)  // removes refresh token
        }
    }
}