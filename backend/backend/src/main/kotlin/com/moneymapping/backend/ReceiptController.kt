package com.moneymapping.backend

import com.fasterxml.jackson.databind.ObjectMapper   // parses the JSON response from Claude
import com.sirolf2009.modulith.account.VerifyToken   // verifies the JWT access token
import com.sirolf2009.modulith.cqrs.execute           // executes commands from the auth library
import org.springframework.beans.factory.annotation.Value // reads values from application.properties
import org.springframework.http.HttpStatus            // HTTP status codes
import org.springframework.http.ResponseEntity        // allows returning custom HTTP status codes
import org.springframework.web.bind.annotation.PostMapping   // marks a function as a POST endpoint
import org.springframework.web.bind.annotation.RequestBody   // reads the request body as JSON
import org.springframework.web.bind.annotation.RequestHeader // reads a specific request header
import org.springframework.web.bind.annotation.RequestMapping // sets the base path for all endpoints
import org.springframework.web.bind.annotation.RestController // marks this class as a REST controller
import java.net.URI                                   // used to build the API URL
import java.net.http.HttpClient                       // Java's built-in HTTP client
import java.net.http.HttpRequest                      // builds the HTTP request
import java.net.http.HttpResponse                     // reads the HTTP response

@RestController
@RequestMapping("/receipt") // all endpoints in this class start with /receipt
class ReceiptController {

    @Value("\${anthropic.api.key}") // reads the API key from application.properties
    private lateinit var anthropicApiKey: String

    @PostMapping("/scan") // handles POST /receipt/scan
    fun scanReceipt(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @RequestBody request: ScanReceiptRequest             // reads the base64 image from the Android app
    ): ResponseEntity<Any> {

        // Verifies the token before processing
        val token = authHeader.removePrefix("Bearer ").trim() // strips "Bearer " prefix
        val tokenData = execute(VerifyToken(token))
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        // Builds the prompt that asks Claude to extract items from the receipt image
        val prompt = """
                You are a receipt parser. I will give you an image of a receipt. Your job is to identify only the purchased items with their quantity, unit price, and total price. 
                Ignore everything else like store name, cashier name, date, address, totals, taxes, discounts. 
                Return ONLY a JSON array with no explanation, no markdown, no extra text. Just the raw JSON array. 
                Each item must have exactly these fields: "name" (string), "quantity" (integer), "unitPrice" (decimal), "totalPrice" (decimal). 
                Keep item names in their original language exactly as they appear on the receipt.
                If you cannot find any items, return an empty array: []""".trimIndent()
        println("sending request to claude")

        // Builds the request body — sends the image directly to Claude as a base64 encoded image
        val requestBody = "{" +
                "\"model\":\"claude-haiku-4-5-20251001\"," +
                "\"max_tokens\":1000," +
                "\"messages\":[{" +
                "\"role\":\"user\"," +
                "\"content\":[" +
                "{" +
                "\"type\":\"image\"," +
                "\"source\":{" +
                "\"type\":\"base64\"," +
                "\"media_type\":\"${request.mediaType}\"," +
                "\"data\":\"${request.base64Image}\"" +
                "}" +
                "}," +
                "{" +
                "\"type\":\"text\"," +
                "\"text\":\"${escapeJson(prompt)}\"" +
                "}" +
                "]" +
                "}]" +
                "}"

        // Calls the Claude API using Java's built-in HTTP client
        return try {
            val client = HttpClient.newHttpClient() // creates the HTTP client
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages")) // Claude API endpoint
                .header("Content-Type", "application/json")               // tells the API we're sending JSON
                .header("x-api-key", anthropicApiKey)                     // authenticates with our API key
                .header("anthropic-version", "2023-06-01")                // required API version header
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))   // sends the request body
                .build()

            // Sends the request and reads the response with UTF-8 to support all languages including Russian
            val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            val responseBody = response.body() // gets the response text
            println(responseBody)

            // Parses Claude's response using Jackson to safely extract the text field
            val mapper = ObjectMapper()                                       // creates a JSON parser
            val root = mapper.readTree(responseBody)                          // reads the full response
            val rawText = root["content"]?.get(0)?.get("text")?.asText() ?: "[]" // extracts the text field

            // Removes markdown code block markers in case Claude wrapped the JSON in them
            val jsonText = rawText
                .replace(Regex("```json\\s*"), "") // removes opening ```json marker
                .replace(Regex("```\\s*"), "")     // removes closing ``` marker
                .trim()                            // removes extra whitespace

            ResponseEntity.ok(jsonText) // returns the clean JSON array to the Android app
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to process receipt: ${e.message}") // returns error if something goes wrong
        }
    }

    // Escapes a string so it can be safely embedded inside a JSON string value
    private fun escapeJson(text: String): String {
        val escaped = text
            .replace("\\", "\\\\") // escapes backslashes
            .replace("\"", "\\\"") // escapes quotes
            .replace("\n", "\\n")  // escapes newlines
            .replace("\r", "\\r")  // escapes carriage returns
            .replace("\t", "\\t")  // escapes tabs
        return escaped // returns the escaped text
    }
}

// The request body sent from the Android app — contains the base64 encoded receipt image
data class ScanReceiptRequest(
    val base64Image: String, // the receipt image encoded as base64
    val mediaType: String    // the image type e.g. "image/jpeg" or "image/png"
)