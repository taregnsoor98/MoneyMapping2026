package com.moneymapping.backend

import org.springframework.web.bind.annotation.GetMapping        // marks a function as a GET endpoint
import org.springframework.web.bind.annotation.RequestMapping    // sets the base path for all endpoints in this class
import org.springframework.web.bind.annotation.RequestParam      // reads a query parameter from the URL
import org.springframework.web.bind.annotation.RestController    // marks this class as a REST controller

@RestController
@RequestMapping("/exchange-rates") // all endpoints in this class start with /exchange-rates
class ExchangeRateController(
    private val exchangeRateService: ExchangeRateService // injected automatically by Spring
) {

    // handles GET /exchange-rates?base=USD — returns exchange rates for the given base currency
    @GetMapping
    fun getRates(
        @RequestParam(defaultValue = "USD") base: String // base currency — defaults to USD if not provided
    ): Map<String, Double> {
        return exchangeRateService.getRates(base.uppercase()) // converts base to uppercase to avoid case issues e.g. "usd" vs "USD"
    }
}