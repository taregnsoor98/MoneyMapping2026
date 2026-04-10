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

// creates a single DataStore instance tied to the app context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

class TokenManager(private val context: Context) { // takes app context to access DataStore

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")   // key for storing access token
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token") // key for storing refresh token
        private val USER_ID_KEY = stringPreferencesKey("user_id")             // key for storing the logged-in user's ID
    }

    // saves both tokens and the user ID to DataStore after successful login
    suspend fun saveTokens(accessToken: String, refreshToken: String, userId: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken   // stores the access token
            preferences[REFRESH_TOKEN_KEY] = refreshToken // stores the refresh token
            if (userId != null) {
                preferences[USER_ID_KEY] = userId         // stores the user ID if provided
            }
        }
    }

    // returns the access token as a Flow so it can be observed
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY] // reads the access token
    }

    // returns the refresh token as a Flow so it can be observed
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY] // reads the refresh token
    }

    // gets the current access token directly (not as a flow)
    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY] // reads access token once
    }

    // gets the current refresh token directly (not as a flow)
    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[REFRESH_TOKEN_KEY] // reads refresh token once
    }

    // gets the current user ID directly (not as a flow)
    suspend fun getUserId(): String? {
        return context.dataStore.data.first()[USER_ID_KEY] // reads user ID once
    }

    // clears all stored data — used when logging out
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)   // removes access token
            preferences.remove(REFRESH_TOKEN_KEY)  // removes refresh token
            preferences.remove(USER_ID_KEY)        // removes user ID
        }
    }
}