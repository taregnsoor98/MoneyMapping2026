package com.moneymapping.backend

import org.springframework.beans.factory.annotation.Value  // reads values from application.properties
import org.springframework.stereotype.Service              // marks this class as a Spring service — auto-injected where needed
import org.springframework.web.client.RestTemplate         // Spring's built-in HTTP client for making external API calls
import java.time.Instant                                   // used to track when the cache was last refreshed

@Service // tells Spring to create and manage one instance of this class
class ExchangeRateService {

    @Value("\${exchangerate.api.key}") // reads the API key from application.properties
    private lateinit var apiKey: String  // injected by Spring before any method is called

    private val restTemplate = RestTemplate()              // HTTP client used to call the ExchangeRate-API

    // ─── Cache ────────────────────────────────────────────────────────────────

    private var cachedRates: Map<String, Double> = emptyMap() // stores the last fetched exchange rates
    private var cacheBase: String = ""                         // tracks which base currency the cache is for
    private var cacheTime: Instant = Instant.EPOCH             // tracks when the cache was last filled — EPOCH means never

    private val cacheDurationSeconds = 12 * 60 * 60L          // 12 hours in seconds — cache is refreshed after this time

    // ─── Public API ───────────────────────────────────────────────────────────

    // returns exchange rates for the given base currency — uses cache if still fresh, fetches otherwise
    fun getRates(base: String): Map<String, Double> {
        val now = Instant.now()                                // current time used to check if cache has expired
        val cacheExpired = now.epochSecond - cacheTime.epochSecond > cacheDurationSeconds // true if 12h have passed
        val baseChanged = base != cacheBase                   // true if the requested base currency is different from cached one

        if (cachedRates.isNotEmpty() && !cacheExpired && !baseChanged) {
            return cachedRates                                 // cache is still fresh and matches the base — return it directly
        }

        return fetchAndCache(base)                            // cache is stale or base changed — fetch fresh rates
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    // calls the ExchangeRate-API and stores the result in the cache
    private fun fetchAndCache(base: String): Map<String, Double> {
        val url = "https://v6.exchangerate-api.com/v6/$apiKey/latest/$base" // builds the API URL with the key and base currency
        val response = restTemplate.getForObject(url, ExchangeRateApiResponse::class.java) // makes the HTTP GET request
            ?: throw RuntimeException("Failed to fetch exchange rates from ExchangeRate-API") // throws if the response is null

        cachedRates = response.conversion_rates              // stores the rates map in the cache
        cacheBase = base                                     // stores the base currency so we know what the cache is for
        cacheTime = Instant.now()                            // records when we fetched so we know when to refresh

        return cachedRates                                   // returns the freshly fetched rates
    }
}

// ─── Response model ───────────────────────────────────────────────────────────

// maps the JSON response from ExchangeRate-API — only the fields we need
data class ExchangeRateApiResponse(
    val result: String,                                      // "success" or "error" — tells us if the request worked
    val conversion_rates: Map<String, Double>                // the map of currency codes to exchange rates e.g. {"USD": 1.0, "EUR": 0.91}
)