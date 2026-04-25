// ─── Package declaration ───────────────────────────────────────────────────────
// This must match the package of the class we are testing so the test can access it
package com.moneymapping.backend

// ─── JUnit 5 imports ──────────────────────────────────────────────────────────
// JUnit 5 is the test runner — it finds our @Test functions and executes them
import org.junit.jupiter.api.Assertions.assertEquals   // checks that two values are equal — fails the test if they are not
import org.junit.jupiter.api.Assertions.assertNotEquals // checks that two values are NOT equal — used to confirm a fetch happened
import org.junit.jupiter.api.BeforeEach                // marks a function that runs automatically before every single test
import org.junit.jupiter.api.Test                      // marks a function as a test case that JUnit should run

// ─── Mockito imports ──────────────────────────────────────────────────────────
// Mockito lets us create fake (mock) objects so we don't make real HTTP calls during tests
import org.mockito.Mockito.mock                        // creates a fake instance of any class or interface
import org.mockito.Mockito.`when`                      // tells the mock what to return when a specific method is called
import org.mockito.Mockito.verify                      // checks that a method on the mock was actually called
import org.mockito.Mockito.times                       // used with verify — checks how many times a method was called

// ─── Spring RestTemplate import ───────────────────────────────────────────────
// RestTemplate is the HTTP client used inside ExchangeRateService to call the real API
// We will replace it with a mock so no real network call is ever made during tests
import org.springframework.web.client.RestTemplate

// ─── Java reflection import ───────────────────────────────────────────────────
// We use reflection to inject private fields (like restTemplate and apiKey) into the service
// This is necessary because those fields are private — we cannot set them directly from outside the class
import java.lang.reflect.Field
import java.time.Instant // used to manually set the cache timestamp to simulate time passing


// ─────────────────────────────────────────────────────────────────────────────
// TEST CLASS
// This class holds all unit tests for ExchangeRateService.
// It is a plain Kotlin class — no Spring, no database, no network.
// Every test runs in milliseconds because everything external is faked.
// ─────────────────────────────────────────────────────────────────────────────
class ExchangeRateServiceTest {

    // ── Fields we will use in every test ──────────────────────────────────────

    private lateinit var service: ExchangeRateService
    // The real ExchangeRateService we are testing — created fresh before each test

    private lateinit var mockRestTemplate: RestTemplate
    // A fake RestTemplate created by Mockito — we control what it returns
    // This means no real HTTP call is ever made during any test

    // ── @BeforeEach ───────────────────────────────────────────────────────────
    // This function runs automatically before EVERY test below.
    // It creates a fresh service and a fresh mock so tests never affect each other.
    @BeforeEach
    fun setUp() {
        service = ExchangeRateService()
        // Creates a real ExchangeRateService instance — but we will swap its internals below

        mockRestTemplate = mock(RestTemplate::class.java)
        // Creates a fake RestTemplate — it does nothing by default until we tell it what to return

        injectField(service, "restTemplate", mockRestTemplate)
        // Injects our fake RestTemplate into the private restTemplate field of the service
        // Without this, the service would use the real RestTemplate and make real network calls

        injectField(service, "apiKey", "test-api-key")
        // Injects a fake API key into the private apiKey field
        // The real key comes from application.properties via @Value — we bypass that here
        // The value "test-api-key" is just a placeholder — no real API call is made anyway
    }


    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 — Fresh cache is returned without making a new API call
    //
    // WHY THIS TEST EXISTS:
    // The whole point of the cache is to avoid hitting the external API on every request.
    // If the cache is fresh (less than 12 hours old) and the base currency is the same,
    // the service should return the cached rates immediately — no API call should happen.
    // This test confirms that behavior works correctly.
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `returns cached rates when cache is still fresh`() {

        // ARRANGE — set up the fake API response that will be returned on the first call
        val fakeRates = mapOf("USD" to 1.0, "EUR" to 0.91, "GBP" to 0.79)
        // This is the fake exchange rate data we pretend the API returned

        val fakeResponse = ExchangeRateApiResponse(
            result = "success",           // simulates a successful API response
            conversion_rates = fakeRates  // the fake rates we want the service to store in its cache
        )

        val expectedUrl = "https://v6.exchangerate-api.com/v6/test-api-key/latest/USD"
        // This is the exact URL the service will build when fetching rates for USD

        `when`(mockRestTemplate.getForObject(expectedUrl, ExchangeRateApiResponse::class.java))
            .thenReturn(fakeResponse)
        // Tells our fake RestTemplate: when this exact URL is called, return fakeResponse
        // This is how Mockito intercepts the network call and replaces it with our fake data

        // ACT — call getRates twice with the same base currency
        val firstCall = service.getRates("USD")
        // First call — cache is empty, so the service should fetch from the (fake) API and store the result

        val secondCall = service.getRates("USD")
        // Second call — cache should now be fresh, so the service should return the cached result directly

        // ASSERT — both calls should return the same rates
        assertEquals(fakeRates, firstCall)
        // Confirms the first call returned the correct rates from the fake API

        assertEquals(fakeRates, secondCall)
        // Confirms the second call also returned the correct rates (from cache this time)

        verify(mockRestTemplate, times(1))
            .getForObject(expectedUrl, ExchangeRateApiResponse::class.java)
        // THE KEY CHECK: confirms the fake RestTemplate was called exactly ONCE — not twice.
        // If the cache is working correctly, the second call should NOT trigger a new API call.
        // If this fails, it means the cache is broken and the service is fetching every time.
    }


    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 — Cache is bypassed when the base currency changes
    //
    // WHY THIS TEST EXISTS:
    // The cache is only valid for one base currency at a time.
    // If the user changes their home currency (e.g. from USD to EUR),
    // the cached rates are no longer correct and a new fetch must happen.
    // This test confirms the service correctly detects a base currency change
    // and fetches fresh rates instead of returning stale data.
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `fetches fresh rates when base currency changes`() {

        // ARRANGE — prepare two different fake responses for two different base currencies
        val usdRates = mapOf("USD" to 1.0, "EUR" to 0.91)
        val eurRates = mapOf("USD" to 1.10, "EUR" to 1.0)
        // Different base currencies produce different rate maps

        val usdResponse = ExchangeRateApiResponse("success", usdRates)
        val eurResponse = ExchangeRateApiResponse("success", eurRates)

        val usdUrl = "https://v6.exchangerate-api.com/v6/test-api-key/latest/USD"
        val eurUrl = "https://v6.exchangerate-api.com/v6/test-api-key/latest/EUR"

        `when`(mockRestTemplate.getForObject(usdUrl, ExchangeRateApiResponse::class.java))
            .thenReturn(usdResponse)
        // When USD url is called → return usdResponse

        `when`(mockRestTemplate.getForObject(eurUrl, ExchangeRateApiResponse::class.java))
            .thenReturn(eurResponse)
        // When EUR url is called → return eurResponse

        // ACT
        val usdResult = service.getRates("USD")
        // First call with USD — fetches and caches USD rates

        val eurResult = service.getRates("EUR")
        // Second call with EUR — base changed, so the service must fetch again

        // ASSERT
        assertEquals(usdRates, usdResult)
        // Confirms the USD call returned the correct USD rates

        assertEquals(eurRates, eurResult)
        // Confirms the EUR call returned the correct EUR rates (freshly fetched, not the stale USD cache)

        assertNotEquals(usdResult, eurResult)
        // Confirms the two results are actually different — proving the cache was invalidated

        verify(mockRestTemplate, times(1))
            .getForObject(usdUrl, ExchangeRateApiResponse::class.java)
        verify(mockRestTemplate, times(1))
            .getForObject(eurUrl, ExchangeRateApiResponse::class.java)
        // Confirms the API was called exactly once for USD and once for EUR — two fetches total
    }


    // ─────────────────────────────────────────────────────────────────────────
    // TEST 3 — Cache is bypassed when 12 hours have passed
    //
    // WHY THIS TEST EXISTS:
    // Exchange rates change over time, so the cache must expire after 12 hours.
    // This test simulates time passing by manually setting the cache timestamp
    // to a moment 13 hours ago, then confirming the service fetches fresh data.
    // We cannot wait a real 12 hours in a test — so we manipulate the timestamp directly.
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `fetches fresh rates when cache has expired`() {

        // ARRANGE — set up fake response
        val fakeRates = mapOf("USD" to 1.0, "EUR" to 0.91)
        val fakeResponse = ExchangeRateApiResponse("success", fakeRates)
        val url = "https://v6.exchangerate-api.com/v6/test-api-key/latest/USD"

        `when`(mockRestTemplate.getForObject(url, ExchangeRateApiResponse::class.java))
            .thenReturn(fakeResponse)

        // Perform a first call to populate the cache with USD rates
        service.getRates("USD")
        // After this call, the cache holds USD rates and cacheTime is set to now

        // Now we simulate 13 hours passing by manually overwriting the cacheTime field
        val thirteenHoursAgo = Instant.now().minusSeconds(13 * 60 * 60)
        // 13 hours * 60 minutes * 60 seconds = 46800 seconds in the past
        injectField(service, "cacheTime", thirteenHoursAgo)
        // Sets the cacheTime field directly to 13 hours ago
        // The service will now think the cache is stale and needs to be refreshed

        // ACT — call getRates again with the same base currency
        service.getRates("USD")
        // The cache appears expired (13h > 12h threshold), so a new fetch should happen

        // ASSERT
        verify(mockRestTemplate, times(2))
            .getForObject(url, ExchangeRateApiResponse::class.java)
        // Confirms the API was called TWICE — once for the initial fetch, once after expiry
        // If this passes, the 12-hour expiry logic is working correctly
    }


    // ─────────────────────────────────────────────────────────────────────────
    // TEST 4 — Cache is used when it is less than 12 hours old
    //
    // WHY THIS TEST EXISTS:
    // This is the complementary test to Test 3.
    // If the cache is only a few hours old, it should NOT be refreshed.
    // We simulate a cache that was set 6 hours ago and confirm no new fetch happens.
    // Together with Test 3, this gives us complete confidence in the expiry boundary.
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `does not fetch again when cache is less than 12 hours old`() {

        // ARRANGE
        val fakeRates = mapOf("USD" to 1.0, "EUR" to 0.91)
        val fakeResponse = ExchangeRateApiResponse("success", fakeRates)
        val url = "https://v6.exchangerate-api.com/v6/test-api-key/latest/USD"

        `when`(mockRestTemplate.getForObject(url, ExchangeRateApiResponse::class.java))
            .thenReturn(fakeResponse)

        // Perform first call to populate cache
        service.getRates("USD")

        // Simulate 6 hours passing — still within the 12-hour window
        val sixHoursAgo = Instant.now().minusSeconds(6 * 60 * 60)
        injectField(service, "cacheTime", sixHoursAgo)
        // Sets cacheTime to 6 hours ago — cache should still be considered fresh

        // ACT — call again with the same base currency
        service.getRates("USD")

        // ASSERT
        verify(mockRestTemplate, times(1))
            .getForObject(url, ExchangeRateApiResponse::class.java)
        // Confirms the API was only called ONCE — the second call used the cache
        // 6 hours is within the 12-hour window, so no new fetch should have happened
    }


    // ─────────────────────────────────────────────────────────────────────────
    // HELPER — injectField
    //
    // WHY THIS EXISTS:
    // ExchangeRateService has private fields (restTemplate, apiKey, cacheTime).
    // We cannot set private fields directly from outside the class in Kotlin/Java.
    // This helper uses Java Reflection to reach inside the object and set the field anyway.
    // Reflection is a standard, accepted technique in unit testing for exactly this purpose.
    // ─────────────────────────────────────────────────────────────────────────
    private fun injectField(target: Any, fieldName: String, value: Any) {
        val field: Field = target.javaClass.getDeclaredField(fieldName)
        // Finds the private field by name inside the target object's class definition

        field.isAccessible = true
        // Temporarily unlocks the field so we can write to it from outside the class
        // Without this line, Java would throw an IllegalAccessException

        field.set(target, value)
        // Sets the field's value to whatever we pass in
        // This is how we inject our mock RestTemplate, fake API key, and fake timestamps
    }
}