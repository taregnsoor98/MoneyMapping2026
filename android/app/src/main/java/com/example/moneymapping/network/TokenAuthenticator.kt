package com.example.moneymapping.network

import com.example.moneymapping.data.TokenManager // access saved tokens from DataStore
import kotlinx.coroutines.runBlocking              // lets us call suspend functions from non-suspend context
import okhttp3.Authenticator                       // OkHttp interface for handling 401 responses
import okhttp3.Request                             // represents an HTTP request
import okhttp3.Response                            // represents an HTTP response
import okhttp3.Route                               // represents the route of the request (used by OkHttp interface)

// TokenAuthenticator is called automatically by OkHttp whenever a 401 Unauthorized is received.
// It tries to refresh the access token using the stored refresh token.
// If successful, it saves the new tokens and retries the original request with the new access token.
// If refresh fails (e.g. refresh token is also expired), it returns null — which cancels the request.
class TokenAuthenticator(
    private val tokenManager: TokenManager, // used to read and save tokens from DataStore
    private val authApi: AuthApi            // used to call the /account/refresh endpoint
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        // If we already tried refreshing on this request and still got 401, stop retrying.
        // This prevents an infinite loop if the refresh token is also invalid.
        if (response.request.header("X-Retry-After-Refresh") != null) {
            return null // already retried once — give up
        }

        // Get the stored refresh token — done inside runBlocking because authenticate() is not a suspend function
        val refreshToken = runBlocking { tokenManager.getRefreshToken() }
            ?: return null // no refresh token stored — can't refresh, give up

        return try {
            // Call the refresh endpoint with the refresh token
            val newTokens = runBlocking {
                authApi.refresh("Bearer $refreshToken") // sends refresh token to backend
            }

            // Save the new access and refresh tokens to DataStore
            runBlocking {
                tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
            }

            // Retry the original request with the new access token attached
            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}") // replaces old token
                .header("X-Retry-After-Refresh", "true")                    // marks this as a retry so we don't loop
                .build()

        } catch (e: Exception) {
            // Refresh failed (network error, expired refresh token, etc.) — cancel the request
            null
        }
    }
}
// the refresh token will also expire, then the user need to put the credits again (ideally once every 3 weeks)
// this way he gets a whole new set of tokens (like the refresh tokens, which will be valid for again 3 weeks)
// he has to re-enter his info (email/username and password to get his new tokens)