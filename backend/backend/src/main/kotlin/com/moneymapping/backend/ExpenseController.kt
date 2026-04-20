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
    private val itemAssignmentRepository: ItemAssignmentRepository,       // injected automatically by Spring
    private val expensePayerRepository: ExpensePayerRepository            // injected automatically by Spring
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

    // Saves all payers for an expense — used in both create and update
    private fun savePayers(expenseId: String, payers: List<ExpensePayerRequest>): List<ExpensePayer> {
        return payers.map { payer ->
            expensePayerRepository.save(
                ExpensePayer(
                    id = UUID.randomUUID().toString(), // generates a unique ID for this payer record
                    expenseId = expenseId,             // links the payer to this expense
                    payerName = payer.payerName,       // the name of the person who paid
                    amountPaid = payer.amountPaid      // how much they paid
                )
            )
        }
    }

    @Transactional // ensures expense, items, assignments and payers are all saved together atomically
    @PostMapping // handles POST /expenses — saves a new expense, its items, assignments and payers
    fun createExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @RequestBody request: CreateExpenseRequest          // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val expense = Expense(
            id = UUID.randomUUID().toString(),       // generates a unique ID for this expense
            paidBy = userId,                         // stores the userId so GET /expenses can fetch by owner
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

        val savedPayers = savePayers(saved.id, request.payers) // saves all payers for this expense

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

        return ResponseEntity.ok(saved.toResponse(savedItems, savedPayers)) // returns the saved expense with items, assignments and payers
    }

    @GetMapping("/{id}") // handles GET /expenses/{id} — returns a single expense with its items, assignments and payers
    fun getExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String                            // reads the expense ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val expense = expenseRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found")

        val payers = expensePayerRepository.findByExpenseId(id)          // fetches all payers for this expense
        val items = expenseItemRepository.findByExpenseId(id)            // fetches all items for this expense
        val itemsWithAssignments = items.map { item ->
            val assignments = itemAssignmentRepository.findByExpenseItemId(item.id) // fetches assignments for each item
            item to assignments // pairs each item with its assignments
        }

        return ResponseEntity.ok(expense.toResponse(itemsWithAssignments, payers)) // returns expense with items, assignments and payers
    }

    @GetMapping // handles GET /expenses — returns all expenses for the logged-in user
    fun getExpenses(
        @RequestHeader("Authorization") authHeader: String // reads the Authorization header
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val expenses = expenseRepository.findByPaidByOrderByDateDesc(userId) // fetches expenses newest first
        return ResponseEntity.ok(expenses.map { expense ->
            val payers = expensePayerRepository.findByExpenseId(expense.id)  // fetches payers for each expense
            val items = expenseItemRepository.findByExpenseId(expense.id)    // fetches items for each expense
            val itemsWithAssignments = items.map { item ->
                val assignments = itemAssignmentRepository.findByExpenseItemId(item.id) // fetches assignments for each item
                item to assignments // pairs each item with its assignments
            }
            expense.toResponse(itemsWithAssignments, payers) // returns expense with items, assignments and payers
        })
    }

    @GetMapping("/group/{groupId}") // handles GET /expenses/group/{groupId} — returns all expenses for a specific group
    fun getGroupExpenses(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable groupId: Int                          // reads the group ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val expenses = expenseRepository.findByGroupIdOrderByDateDesc(groupId.toLong()) // fetches group expenses newest first
        return ResponseEntity.ok(expenses.map { expense ->
            val payers = expensePayerRepository.findByExpenseId(expense.id)  // fetches payers for each expense
            val items = expenseItemRepository.findByExpenseId(expense.id)    // fetches items for each expense
            val itemsWithAssignments = items.map { item ->
                val assignments = itemAssignmentRepository.findByExpenseItemId(item.id) // fetches assignments for each item
                item to assignments // pairs each item with its assignments
            }
            expense.toResponse(itemsWithAssignments, payers) // returns expense with items, assignments and payers
        })
    }

    @Transactional // ensures expense, items, assignments and payers are all updated together atomically
    @PutMapping("/{id}") // handles PUT /expenses/{id} — updates an existing expense
    fun updateExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String,                           // reads the expense ID from the URL
        @RequestBody request: CreateExpenseRequest          // reads the updated expense data from the request body
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val existing = expenseRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found")

        val updated = existing.copy(
            amount = request.amount,                 // updates total amount
            currency = request.currency,             // updates currency
            description = request.description,       // updates description
            category = request.category,             // updates category
            date = LocalDate.parse(request.date),    // updates date
            isOneTimeSplit = request.isOneTimeSplit, // updates split flag
            receiptImages = request.receiptImages    // updates receipt images
        )

        val saved = expenseRepository.save(updated) // saves the updated expense

        // Deletes old payers and saves the new ones
        expensePayerRepository.deleteByExpenseId(id)
        val savedPayers = savePayers(saved.id, request.payers)

        // Deletes old items and their assignments before saving the new ones
        val existingItems = expenseItemRepository.findByExpenseId(id) // fetches old items
        existingItems.forEach { item ->
            itemAssignmentRepository.deleteByExpenseItemId(item.id) // deletes old assignments first
        }
        expenseItemRepository.deleteByExpenseId(id) // deletes old items

        // Saves the new items and their assignments
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

        return ResponseEntity.ok(saved.toResponse(savedItems, savedPayers)) // returns the updated expense with items, assignments and payers
    }

    @Transactional // ensures the expense, its items, assignments and payers are all deleted together atomically
    @DeleteMapping("/{id}") // handles DELETE /expenses/{id} — deletes an expense, its items, assignments and payers
    fun deleteExpense(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: String                            // reads the expense ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val existing = expenseRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found")

        expensePayerRepository.deleteByExpenseId(id) // deletes all payers for this expense first

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

// The request body for a single payer sent when creating or updating an expense
data class ExpensePayerRequest(
    val payerName: String,   // the name of the person who paid — username or guest name
    val amountPaid: Double   // how much this person paid toward the expense
)

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
    val groupId: Int? = null,                                  // optional group ID
    val amount: Double,                                        // total amount
    val currency: String,                                      // e.g. "USD"
    val description: String,                                   // what the expense was for
    val category: String,                                      // e.g. "Food"
    val date: String,                                          // date string in "yyyy-MM-dd" format
    val isOneTimeSplit: Boolean = false,                        // true if one-time split
    val receiptImages: List<String> = emptyList(),             // optional receipt image URIs
    val items: List<ExpenseItemRequest> = emptyList(),         // list of items in this expense
    val payers: List<ExpensePayerRequest> = emptyList()        // list of payers for this expense
)

// The response object for a single payer returned to the client
data class ExpensePayerResponse(
    val id: String,          // the payer record's unique ID
    val payerName: String,   // the name of the person who paid
    val amountPaid: Double   // how much they paid
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
// Takes a list of item+assignment pairs, and a list of payers
fun Expense.toResponse(
    items: List<Pair<ExpenseItem, List<ItemAssignment>>> = emptyList(),
    payers: List<ExpensePayer> = emptyList()
) = ExpenseResponse(
    id = id,                                         // the expense ID
    paidBy = paidBy,                                 // kept for backwards compatibility — may be null
    groupId = groupId,                               // the group ID if applicable
    amount = amount,                                 // the total amount
    currency = currency,                             // the currency
    description = description,                       // the description
    category = category,                             // the category
    date = date.toString(),                          // converts LocalDate back to string for JSON
    isOneTimeSplit = isOneTimeSplit,                 // the split flag
    receiptImages = receiptImages,                   // the receipt image URIs
    payers = payers.map { p ->                       // converts each payer entity to a response object
        ExpensePayerResponse(
            id = p.id,                               // payer record ID
            payerName = p.payerName,                 // payer's name
            amountPaid = p.amountPaid                // how much they paid
        )
    },
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
    val id: String,                                            // the expense ID
    val paidBy: String?,                                       // kept for backwards compatibility — may be null
    val groupId: Int?,                                         // optional group ID
    val amount: Double,                                        // total amount
    val currency: String,                                      // e.g. "USD"
    val description: String,                                   // what the expense was for
    val category: String,                                      // e.g. "Food"
    val date: String,                                          // date as string "yyyy-MM-dd"
    val isOneTimeSplit: Boolean,                               // true if one-time split
    val receiptImages: List<String>,                           // receipt image URIs
    val payers: List<ExpensePayerResponse> = emptyList(),      // list of payers for this expense
    val items: List<ExpenseItemResponse> = emptyList()         // list of items with their assignments
)