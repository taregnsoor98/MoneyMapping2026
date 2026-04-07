package com.moneymapping.backend

import com.sirolf2009.modulith.account.VerifyToken        // verifies the JWT access token
import com.sirolf2009.modulith.cqrs.execute               // executes commands from the auth library
import org.springframework.http.HttpStatus                // HTTP status codes like 200, 401, 404
import org.springframework.http.ResponseEntity            // allows returning custom HTTP status codes
import org.springframework.transaction.annotation.Transactional // ensures database operations are atomic
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
    private val expenseRepository: ExpenseRepository,                     // injected automatically by Spring
    private val expenseItemRepository: ExpenseItemRepository,             // injected automatically by Spring
    private val itemAssignmentRepository: ItemAssignmentRepository        // injected automatically by Spring
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

    @Transactional // ensures expense, items and assignments are all saved together atomically
    @PostMapping // handles POST /expenses — saves a new expense, its items and their assignments
    fun createExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @RequestBody request: CreateExpenseRequest          // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

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

        val saved = expenseRepository.save(expense) // saves the expense to the database

        // Saves each item and its assignments linked to this expense
        val savedItems = request.items.map { item ->
            val savedItem = expenseItemRepository.save(
                ExpenseItem(
                    expenseId = saved.id,        // links the item to this expense
                    name = item.name,            // item name
                    unitPrice = item.unitPrice,  // price per unit
                    quantity = item.quantity,    // quantity
                    totalPrice = item.totalPrice // total price for this item
                )
            )

            // Saves each assignment for this item
            val savedAssignments = item.assignments.map { assignment ->
                itemAssignmentRepository.save(
                    ItemAssignment(
                        expenseItemId = savedItem.id,        // links assignment to this item
                        personName = assignment.personName,  // the person's name
                        quantity = assignment.quantity,      // how many units they are taking
                        shareAmount = assignment.shareAmount // how much they owe for this item
                    )
                )
            }

            savedItem to savedAssignments // pairs the saved item with its saved assignments
        }

        return ResponseEntity.ok(saved.toResponse(savedItems)) // returns the saved expense with items and assignments
    }

    @GetMapping("/{id}") // handles GET /expenses/{id} — returns a single expense with its items and assignments
    fun getExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String                            // reads the expense ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val expense = expenseRepository.findByIdAndPaidBy(id, userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found or access denied")

        val items = expenseItemRepository.findByExpenseId(id) // fetches all items for this expense
        val itemsWithAssignments = items.map { item ->
            val assignments = itemAssignmentRepository.findByExpenseItemId(item.id) // fetches assignments for each item
            item to assignments // pairs each item with its assignments
        }

        return ResponseEntity.ok(expense.toResponse(itemsWithAssignments)) // returns expense with items and assignments
    }

    @GetMapping // handles GET /expenses — returns all expenses for the logged-in user
    fun getExpenses(
        @RequestHeader("Authorization") authHeader: String // reads the Authorization header
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val expenses = expenseRepository.findByPaidByOrderByDateDesc(userId) // fetches expenses newest first
        return ResponseEntity.ok(expenses.map { expense ->
            val items = expenseItemRepository.findByExpenseId(expense.id) // fetches items for each expense
            val itemsWithAssignments = items.map { item ->
                val assignments = itemAssignmentRepository.findByExpenseItemId(item.id) // fetches assignments for each item
                item to assignments // pairs each item with its assignments
            }
            expense.toResponse(itemsWithAssignments) // returns expense with items and assignments
        })
    }

    @Transactional // ensures expense, items and assignments are all updated together atomically
    @PutMapping("/{id}") // handles PUT /expenses/{id} — updates an existing expense, its items and assignments
    fun updateExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String,                           // reads the expense ID from the URL
        @RequestBody request: CreateExpenseRequest          // reads the updated data as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val existing = expenseRepository.findByIdAndPaidBy(id, userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found or access denied")

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

        val saved = expenseRepository.save(updated) // saves the updated expense

        // Deletes all existing assignments for each item before deleting the items
        val existingItems = expenseItemRepository.findByExpenseId(id) // fetches existing items
        existingItems.forEach { item ->
            itemAssignmentRepository.deleteByExpenseItemId(item.id) // deletes assignments for this item
        }

        expenseItemRepository.deleteByExpenseId(id) // deletes all old items after their assignments are gone

        // Saves the updated items and their assignments
        val savedItems = request.items.map { item ->
            val savedItem = expenseItemRepository.save(
                ExpenseItem(
                    expenseId = id,              // links the item to this expense
                    name = item.name,            // updated item name
                    unitPrice = item.unitPrice,  // updated unit price
                    quantity = item.quantity,    // updated quantity
                    totalPrice = item.totalPrice // updated total price
                )
            )

            // Saves each assignment for this updated item
            val savedAssignments = item.assignments.map { assignment ->
                itemAssignmentRepository.save(
                    ItemAssignment(
                        expenseItemId = savedItem.id,        // links assignment to this item
                        personName = assignment.personName,  // the person's name
                        quantity = assignment.quantity,      // how many units they are taking
                        shareAmount = assignment.shareAmount // how much they owe for this item
                    )
                )
            }

            savedItem to savedAssignments // pairs the saved item with its saved assignments
        }

        return ResponseEntity.ok(saved.toResponse(savedItems)) // returns the updated expense with items and assignments
    }

    @Transactional // ensures the expense, its items and assignments are all deleted together atomically
    @DeleteMapping("/{id}") // handles DELETE /expenses/{id} — deletes an expense, its items and assignments
    fun deleteExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String                            // reads the expense ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val existing = expenseRepository.findByIdAndPaidBy(id, userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found or access denied")

        // Deletes all assignments for each item before deleting the items themselves
        val existingItems = expenseItemRepository.findByExpenseId(id) // fetches all items for this expense
        existingItems.forEach { item ->
            itemAssignmentRepository.deleteByExpenseItemId(item.id) // deletes assignments for this item first
        }

        expenseItemRepository.deleteByExpenseId(id) // deletes all items after their assignments are gone
        expenseRepository.delete(existing)           // then deletes the expense itself
        return ResponseEntity.ok("Expense deleted successfully") // confirms deletion
    }
}

// The request body for a single assignment sent when creating or updating an expense
data class ItemAssignmentRequest(
    val personName: String,  // the person's name — either a username or guest name
    val quantity: Double,    // how many units of the item this person is taking
    val shareAmount: Double  // how much this person owes for this item
)

// The request body for a single item sent when creating or updating an expense
data class ExpenseItemRequest(
    val name: String,                                          // item name
    val unitPrice: Double,                                     // price per unit
    val quantity: Double,                                      // quantity — Double to support weight-based items
    val totalPrice: Double,                                    // total price for this item
    val assignments: List<ItemAssignmentRequest> = emptyList() // list of people assigned to this item
)

// The request body sent when creating or updating an expense
data class CreateExpenseRequest(
    val groupId: Int? = null,                          // optional group ID
    val amount: Double,                                // total amount
    val currency: String,                              // e.g. "USD"
    val description: String,                           // what the expense was for
    val category: String,                              // e.g. "Food"
    val date: String,                                  // date string in "yyyy-MM-dd" format
    val isOneTimeSplit: Boolean = false,                // true if one-time split
    val receiptImages: List<String> = emptyList(),     // optional receipt image URIs
    val items: List<ExpenseItemRequest> = emptyList()  // list of items in this expense
)

// The response object for a single assignment returned to the client
data class ItemAssignmentResponse(
    val id: Long,            // the assignment's unique ID
    val personName: String,  // the person's name
    val quantity: Double,    // how many units they are taking
    val shareAmount: Double  // how much they owe for this item
)

// The response object for a single item returned to the client
data class ExpenseItemResponse(
    val id: Long,                                              // the item's unique ID
    val name: String,                                          // item name
    val unitPrice: Double,                                     // price per unit
    val quantity: Double,                                      // quantity
    val totalPrice: Double,                                    // total price for this item
    val assignments: List<ItemAssignmentResponse> = emptyList() // list of assignments for this item
)

// Converts an Expense entity to a response object safe to send to the client
// Takes a list of pairs — each pair is an item and its list of assignments
fun Expense.toResponse(items: List<Pair<ExpenseItem, List<ItemAssignment>>> = emptyList()) = ExpenseResponse(
    id = id,                                         // the expense ID
    paidBy = paidBy,                                 // the user ID of the payer
    groupId = groupId,                               // the group ID if applicable
    amount = amount,                                 // the total amount
    currency = currency,                             // the currency
    description = description,                       // the description
    category = category,                             // the category
    date = date.toString(),                          // converts LocalDate back to string for JSON
    isOneTimeSplit = isOneTimeSplit,                 // the split flag
    receiptImages = receiptImages,                   // the receipt image URIs
    items = items.map { (item, assignments) ->       // converts each item and its assignments to response objects
        ExpenseItemResponse(
            id = item.id,                            // item ID
            name = item.name,                        // item name
            unitPrice = item.unitPrice,              // unit price
            quantity = item.quantity,                // quantity
            totalPrice = item.totalPrice,            // total price
            assignments = assignments.map { a ->     // converts each assignment to a response object
                ItemAssignmentResponse(
                    id = a.id,                       // assignment ID
                    personName = a.personName,       // person's name
                    quantity = a.quantity,           // their quantity
                    shareAmount = a.shareAmount      // their share amount
                )
            }
        )
    }
)

// The response object returned to the client after creating, updating or fetching an expense
data class ExpenseResponse(
    val id: String,                                    // the expense ID
    val paidBy: String,                                // the user ID of the payer
    val groupId: Int?,                                 // optional group ID
    val amount: Double,                                // total amount
    val currency: String,                              // e.g. "USD"
    val description: String,                           // what the expense was for
    val category: String,                              // e.g. "Food"
    val date: String,                                  // date as string "yyyy-MM-dd"
    val isOneTimeSplit: Boolean,                       // true if one-time split
    val receiptImages: List<String>,                   // receipt image URIs
    val items: List<ExpenseItemResponse> = emptyList() // list of items with their assignments
)