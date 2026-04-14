package com.moneymapping.backend

import com.sirolf2009.modulith.account.VerifyToken        // verifies the JWT access token
import com.sirolf2009.modulith.cqrs.execute               // executes commands from the auth library
import org.springframework.http.HttpStatus                // HTTP status codes like 200, 401, 404
import org.springframework.http.ResponseEntity            // allows returning custom HTTP status codes
import org.springframework.transaction.annotation.Transactional // ensures database operations are atomic
import org.springframework.web.bind.annotation.GetMapping     // marks a function as a GET endpoint
import org.springframework.web.bind.annotation.PathVariable   // reads a value from the URL path
import org.springframework.web.bind.annotation.PostMapping    // marks a function as a POST endpoint
import org.springframework.web.bind.annotation.RequestBody    // reads the request body as JSON
import org.springframework.web.bind.annotation.RequestHeader  // reads a specific request header
import org.springframework.web.bind.annotation.RequestMapping // sets the base path for all endpoints
import org.springframework.web.bind.annotation.RestController // marks this class as a REST controller
import java.time.LocalDate                                // used to parse and work with dates
import java.time.temporal.ChronoUnit                      // used to calculate the number of days/weeks/months between dates

@RestController
@RequestMapping("/payment-plans") // all endpoints in this class start with /payment-plans
class PaymentPlanController(
    private val paymentPlanRepository: PaymentPlanRepository,                       // injected automatically by Spring
    private val paymentPlanInstallmentRepository: PaymentPlanInstallmentRepository, // injected automatically by Spring
    private val groupMemberRepository: GroupMemberRepository                        // injected automatically by Spring — used to verify membership
) {

    // extracts and verifies the Bearer token — returns the userId or null if invalid
    private fun getUserId(authHeader: String?): String? {
        val token = authHeader?.removePrefix("Bearer ")?.trim() ?: return null // strips "Bearer " prefix
        return try {
            execute(VerifyToken(token))?.userId // returns the userId from the token
        } catch (e: Exception) {
            null // returns null if token is invalid or expired
        }
    }

    @Transactional // ensures the plan and all its installments are saved together atomically
    @PostMapping // handles POST /payment-plans — creates a new payment plan and generates installments
    fun createPaymentPlan(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @RequestBody request: CreatePaymentPlanRequest      // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        // checks that the requester is a member of the group
        val isMember = groupMemberRepository.findByGroupIdAndUserId(request.groupId, userId).isNotEmpty()
        if (!isMember) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a member of this group")
        }

        // converts the frequency string to the enum — returns 400 if the value is not valid
        val frequency = try {
            PaymentFrequency.valueOf(request.frequency.uppercase()) // converts "MONTHLY" -> PaymentFrequency.MONTHLY
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid frequency: ${request.frequency}") // unknown frequency
        }

        val startDate = LocalDate.parse(request.startDate) // parses "yyyy-MM-dd" string to LocalDate
        val endDate = LocalDate.parse(request.endDate)     // parses "yyyy-MM-dd" string to LocalDate

        // calculates the number of installments based on frequency and date range
        val installmentCount = when (frequency) {
            PaymentFrequency.DAILY -> ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1    // one per day
            PaymentFrequency.WEEKLY -> ChronoUnit.WEEKS.between(startDate, endDate).toInt() + 1  // one per week
            PaymentFrequency.MONTHLY -> ChronoUnit.MONTHS.between(startDate, endDate).toInt() + 1 // one per month
        }

        if (installmentCount <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("End date must be after start date")
        }

        // calculates how much each installment costs — rounded to 2 decimal places
        val installmentAmount = Math.round((request.totalAmount / installmentCount) * 100) / 100.0

        // saves the payment plan to the database
        val plan = paymentPlanRepository.save(
            PaymentPlan(
                groupId = request.groupId,       // the group this plan belongs to
                fromUserId = request.fromUserId, // the person who owes money
                toUserId = request.toUserId,     // the person who is owed money
                totalAmount = request.totalAmount, // the total debt amount
                startDate = request.startDate,   // the start date as a string
                endDate = request.endDate,       // the end date as a string
                frequency = frequency,           // the converted enum value
                installmentAmount = installmentAmount // calculated amount per installment
            )
        )

        // generates each installment based on the frequency and date range
        val installments = (0 until installmentCount).map { index ->
            val dueDate = when (frequency) {
                PaymentFrequency.DAILY -> startDate.plusDays(index.toLong())     // adds days one by one
                PaymentFrequency.WEEKLY -> startDate.plusWeeks(index.toLong())   // adds weeks one by one
                PaymentFrequency.MONTHLY -> startDate.plusMonths(index.toLong()) // adds months one by one
            }

            paymentPlanInstallmentRepository.save(
                PaymentPlanInstallment(
                    paymentPlanId = plan.id,       // links this installment to the plan
                    dueDate = dueDate.toString(),  // stores the due date as "yyyy-MM-dd"
                    amount = installmentAmount     // how much is due for this installment
                )
            )
        }

        return ResponseEntity.ok(PaymentPlanResponse(plan, installments)) // returns the plan with all installments
    }

    @GetMapping("/group/{groupId}") // handles GET /payment-plans/group/{groupId} — returns all plans for a group
    fun getGroupPaymentPlans(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable groupId: Long                         // reads the group ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        // checks that the requester is a member of the group
        val isMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isNotEmpty()
        if (!isMember) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a member of this group")
        }

        // fetches all payment plans for this group
        val plans = paymentPlanRepository.findByGroupId(groupId)

        // for each plan, also fetches its installments and returns them together
        val response = plans.map { plan ->
            val installments = paymentPlanInstallmentRepository.findByPaymentPlanId(plan.id) // fetches installments for this plan
            PaymentPlanResponse(plan, installments) // wraps plan and installments into one response object
        }

        return ResponseEntity.ok(response) // returns the list of plans with their installments
    }

    @Transactional // ensures the installment update is atomic
    @PostMapping("/{id}/installments/{installmentId}/pay") // handles marking a single installment as paid
    fun payInstallment(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: Long,                             // reads the payment plan ID from the URL
        @PathVariable installmentId: Long                   // reads the installment ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        // fetches the payment plan to verify it exists and get the group ID
        val plan = paymentPlanRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment plan not found")

        // checks that the requester is a member of the group
        val isMember = groupMemberRepository.findByGroupIdAndUserId(plan.groupId, userId).isNotEmpty()
        if (!isMember) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a member of this group")
        }

        // fetches the installment to mark as paid
        val installment = paymentPlanInstallmentRepository.findById(installmentId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Installment not found")

        // marks the installment as paid with today's date
        val paid = installment.copy(
            isPaid = true,                         // marks as paid
            paidDate = LocalDate.now().toString()  // records today as the paid date
        )
        paymentPlanInstallmentRepository.save(paid) // saves the updated installment

        // checks if all installments for this plan are now paid
        val allInstallments = paymentPlanInstallmentRepository.findByPaymentPlanId(plan.id)
        val allPaid = allInstallments.all { it.isPaid } // true if every installment is marked paid

        // if all installments are paid, marks the plan itself as COMPLETED
        if (allPaid) {
            paymentPlanRepository.save(plan.copy(status = "COMPLETED")) // updates plan status
        }

        return ResponseEntity.ok(paid) // returns the updated installment
    }
}

// the request body sent when creating a payment plan — frequency is a plain string to match Android's JSON
data class CreatePaymentPlanRequest(
    val groupId: Long,       // the group this plan belongs to
    val fromUserId: String,  // the person who owes money
    val toUserId: String,    // the person who is owed money
    val totalAmount: Double, // the total debt amount
    val startDate: String,   // the start date as "yyyy-MM-dd"
    val endDate: String,     // the end date as "yyyy-MM-dd"
    val frequency: String    // "DAILY", "WEEKLY", or "MONTHLY" — sent as plain string from Android
)

// the response object returned to the client for a payment plan — includes the plan and all its installments
data class PaymentPlanResponse(
    val id: Long,                                    // the payment plan ID
    val groupId: Long,                               // the group this plan belongs to
    val fromUserId: String,                          // the person who owes money
    val toUserId: String,                            // the person who is owed money
    val totalAmount: Double,                         // the total debt amount
    val startDate: String,                           // the start date
    val endDate: String,                             // the end date
    val frequency: String,                           // "DAILY", "WEEKLY", or "MONTHLY" — returned as plain string
    val installmentAmount: Double,                   // the amount per installment
    val status: String,                              // ACTIVE or COMPLETED
    val installments: List<PaymentPlanInstallment>   // all installments for this plan
) {
    // secondary constructor that builds the response directly from the entity objects
    constructor(plan: PaymentPlan, installments: List<PaymentPlanInstallment>) : this(
        id = plan.id,                                // the plan ID
        groupId = plan.groupId,                      // the group ID
        fromUserId = plan.fromUserId,                // the debtor
        toUserId = plan.toUserId,                    // the creditor
        totalAmount = plan.totalAmount,              // total debt
        startDate = plan.startDate,                  // start date
        endDate = plan.endDate,                      // end date
        frequency = plan.frequency.name,             // converts enum to string e.g. PaymentFrequency.MONTHLY -> "MONTHLY"
        installmentAmount = plan.installmentAmount,  // per installment amount
        status = plan.status,                        // ACTIVE or COMPLETED
        installments = installments                  // the list of installments
    )
}