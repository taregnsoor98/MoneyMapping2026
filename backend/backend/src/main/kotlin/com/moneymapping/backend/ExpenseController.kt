package com.moneymapping.backend

import com.sirolf2009.modulith.account.VerifyToken        // verifies the JWT access token
import com.sirolf2009.modulith.cqrs.execute               // executes commands from the auth library
import org.springframework.http.HttpStatus                // HTTP status codes like 200, 401, 404
import org.springframework.http.ResponseEntity            // allows returning custom HTTP status codes
import org.springframework.web.bind.annotation.DeleteMapping  // marks a function as a DELETE endpoint
import org.springframework.web.bind.annotation.GetMapping     // marks a function as a GET endpoint
import org.springframework.web.bind.annotation.PathVariable   // reads a value from the URL path
import org.springframework.web.bind.annotation.PostMapping    // marks a function as a POST endpoint
import org.springframework.web.bind.annotation.PutMapping     // marks a function as a PUT endpoint
import org.springframework.web.bind.annotation.RequestBody    // reads the request body as JSON
import org.springframework.web.bind.annotation.RequestHeader  // reads a specific request header
import org.springframework.web.bind.annotation.RequestMapping // sets the base path for all endpoints
import org.springframework.web.bind.annotation.RestController // marks this class as a REST controller
import java.time.LocalDate                                // used to parse the date string
import java.util.UUID                                     // used to generate unique IDs

@RestController
@RequestMapping("/expenses") // all endpoints in this class start with /expenses
class ExpenseController(
    private val expenseRepository: ExpenseRepository // injected automatically by Spring
) {

    // Extracts and verifies the Bearer token — returns the userId or null if invalid
    private fun getUserId(authHeader: String?): String? {
        val token = authHeader?.removePrefix("Bearer ")?.trim() ?: return null // strips "Bearer " prefix
        return try {
            execute(VerifyToken(token))?.userId // returns the userId from the token
        } catch (e: Exception) {
            null // returns null if token is invalid or expired
        }
    }

    @PostMapping // handles POST /expenses — saves a new expense
    fun createExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @RequestBody request: CreateExpenseRequest          // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token") // blocks unauthorized requests

        val expense = Expense(
            id = UUID.randomUUID().toString(),       // generates a unique ID for this expense
            paidBy = userId,                         // the logged-in user is the payer
            groupId = request.groupId,               // null if solo expense
            amount = request.amount,                 // total amount
            currency = request.currency,             // e.g. "USD"
            description = request.description,       // what the expense was for
            category = request.category,             // e.g. "Food"
            date = LocalDate.parse(request.date),    // parses "yyyy-MM-dd" string to LocalDate
            isOneTimeSplit = request.isOneTimeSplit, // true if one-time split
            receiptImages = request.receiptImages    // list of image URIs
        )

        val saved = expenseRepository.save(expense)  // saves the expense to the database
        return ResponseEntity.ok(saved.toResponse()) // returns the saved expense as JSON
    }

    @GetMapping // handles GET /expenses — returns all expenses for the logged-in user
    fun getExpenses(
        @RequestHeader("Authorization") authHeader: String // reads the Authorization header
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token") // blocks unauthorized requests

        val expenses = expenseRepository.findByPaidByOrderByDateDesc(userId) // fetches expenses newest first
        return ResponseEntity.ok(expenses.map { it.toResponse() })           // returns as list of response objects
    }

    @PutMapping("/{id}") // handles PUT /expenses/{id} — updates an existing expense
    fun updateExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String,                           // reads the expense ID from the URL
        @RequestBody request: CreateExpenseRequest          // reads the updated data as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token") // blocks unauthorized requests

        val existing = expenseRepository.findByIdAndPaidBy(id, userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found or access denied") // blocks access to others' expenses

        val updated = existing.copy(
            groupId = request.groupId,               // updates group ID
            amount = request.amount,                 // updates amount
            currency = request.currency,             // updates currency
            description = request.description,       // updates description
            category = request.category,             // updates category
            date = LocalDate.parse(request.date),    // updates date
            isOneTimeSplit = request.isOneTimeSplit, // updates split flag
            receiptImages = request.receiptImages    // updates receipt images
        )

        val saved = expenseRepository.save(updated)  // saves the updated expense
        return ResponseEntity.ok(saved.toResponse()) // returns the updated expense as JSON
    }

    @DeleteMapping("/{id}") // handles DELETE /expenses/{id} — deletes an expense
    fun deleteExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String                            // reads the expense ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token") // blocks unauthorized requests

        val existing = expenseRepository.findByIdAndPaidBy(id, userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found or access denied") // blocks access to others' expenses

        expenseRepository.delete(existing)                    // deletes the expense from the database
        return ResponseEntity.ok("Expense deleted successfully") // confirms deletion
    }
}

// The request body sent when creating or updating an expense
data class CreateExpenseRequest(
    val groupId: Int? = null,                        // optional group ID
    val amount: Double,                              // total amount
    val currency: String,                            // e.g. "USD"
    val description: String,                         // what the expense was for
    val category: String,                            // e.g. "Food"
    val date: String,                                // date string in "yyyy-MM-dd" format
    val isOneTimeSplit: Boolean = false,              // true if one-time split
    val receiptImages: List<String> = emptyList()    // optional receipt image URIs
)

// Converts an Expense entity to a response object safe to send to the client
fun Expense.toResponse() = ExpenseResponse(
    id = id,                                         // the expense ID
    paidBy = paidBy,                                 // the user ID of the payer
    groupId = groupId,                               // the group ID if applicable
    amount = amount,                                 // the total amount
    currency = currency,                             // the currency
    description = description,                       // the description
    category = category,                             // the category
    date = date.toString(),                          // converts LocalDate back to string for JSON
    isOneTimeSplit = isOneTimeSplit,                 // the split flag
    receiptImages = receiptImages                    // the receipt image URIs
)

// The response object returned to the client after creating, updating or fetching an expense
data class ExpenseResponse(
    val id: String,                                  // the expense ID
    val paidBy: String,                              // the user ID of the payer
    val groupId: Int?,                               // optional group ID
    val amount: Double,                              // total amount
    val currency: String,                            // e.g. "USD"
    val description: String,                         // what the expense was for
    val category: String,                            // e.g. "Food"
    val date: String,                                // date as string "yyyy-MM-dd"
    val isOneTimeSplit: Boolean,                     // true if one-time split
    val receiptImages: List<String>                  // receipt image URIs
)