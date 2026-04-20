package com.example.moneymapping.data

import android.content.Context                                        // needed to access DataStore
import androidx.datastore.preferences.core.edit                       // allows editing stored values
import androidx.datastore.preferences.core.stringPreferencesKey       // creates a string key for storage
import kotlinx.coroutines.flow.Flow                                   // allows observing stored values as a stream
import kotlinx.coroutines.flow.first                                  // gets the first value from a flow
import kotlinx.coroutines.flow.map                                    // transforms flow values

// manages the user's currency preferences — stored in the same DataStore as auth tokens
class CurrencyPreferenceManager(private val context: Context) {

    companion object {
        private val HOME_CURRENCY_KEY = stringPreferencesKey("home_currency")   // key for the user's home currency e.g. USD
        private val LOCAL_CURRENCY_KEY = stringPreferencesKey("local_currency") // key for the user's local currency e.g. AMD

        const val DEFAULT_HOME_CURRENCY = "USD"   // default home currency if none is set
        const val DEFAULT_LOCAL_CURRENCY = "EUR"  // default local currency if none is set

        // the full list of currencies the user can choose from in the dropdowns
        val SUPPORTED_CURRENCIES = listOf(
            "USD", "EUR", "GBP", "AMD", "RUB", "AED",
            "JOD", "JPY", "CNY", "CAD", "AUD", "CHF",
            "TRY", "SAR", "KWD", "QAR", "BHD", "EGP",
            "INR", "PKR", "NOK", "SEK", "DKK", "PLN",
            "CZK", "HUF", "RON", "BGN", "HRK", "UAH",
            "GEL", "AZN", "KZT", "UZS", "BYN", "MXN",
            "BRL", "ARS", "COP", "KRW", "SGD", "HKD",
            "NZD", "ZAR", "NGN", "GHS", "MAD", "TND"
        )
    }

    // returns the home currency as a Flow so the UI can observe changes
    val homeCurrencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOME_CURRENCY_KEY] ?: DEFAULT_HOME_CURRENCY // returns stored value or default
    }

    // returns the local currency as a Flow so the UI can observe changes
    val localCurrencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LOCAL_CURRENCY_KEY] ?: DEFAULT_LOCAL_CURRENCY // returns stored value or default
    }

    // saves the selected home currency to DataStore
    suspend fun saveHomeCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[HOME_CURRENCY_KEY] = currency // stores the selected home currency
        }
    }

    // saves the selected local currency to DataStore
    suspend fun saveLocalCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_CURRENCY_KEY] = currency // stores the selected local currency
        }
    }

    // gets the current home currency directly — used when making API calls
    suspend fun getHomeCurrency(): String {
        return context.dataStore.data.first()[HOME_CURRENCY_KEY] ?: DEFAULT_HOME_CURRENCY
    }

    // gets the current local currency directly — used when making API calls
    suspend fun getLocalCurrency(): String {
        return context.dataStore.data.first()[LOCAL_CURRENCY_KEY] ?: DEFAULT_LOCAL_CURRENCY
    }
}