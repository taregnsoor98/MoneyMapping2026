package com.moneymapping.backend

import com.sirolf2009.modulith.account.VerifyToken        // verifies the JWT access token
import com.sirolf2009.modulith.cqrs.execute               // executes commands from the auth library
import org.springframework.http.HttpStatus                // HTTP status codes like 200, 401, 404
import org.springframework.http.ResponseEntity            // allows returning custom HTTP status codes
import org.springframework.web.bind.annotation.GetMapping     // marks a function as a GET endpoint
import org.springframework.web.bind.annotation.PathVariable   // reads a value from the URL path
import org.springframework.web.bind.annotation.PostMapping    // marks a function as a POST endpoint
import org.springframework.web.bind.annotation.RequestBody    // reads the request body as JSON
import org.springframework.web.bind.annotation.RequestHeader  // reads a specific request header
import org.springframework.web.bind.annotation.RequestMapping // sets the base path for all endpoints
import org.springframework.web.bind.annotation.RestController // marks this class as a REST controller

@RestController
@RequestMapping("/limits") // all endpoints in this class start with /limits
class SpendingLimitController(
    private val spendingLimitRepository: SpendingLimitRepository, // injected automatically by Spring
    private val groupMemberRepository: GroupMemberRepository,     // injected automatically by Spring — used to check roles
    private val groupRepository: GroupRepository                  // injected automatically by Spring — used to check group type
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

    // handles POST /limits/personal — sets or updates the personal limit for the logged-in user
    @PostMapping("/personal")
    fun setPersonalLimit(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @RequestBody request: SetLimitRequest               // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        // checks if a personal limit already exists for this user
        val existing = spendingLimitRepository.findByUserIdAndGroupIdIsNull(userId)

        val limit = if (existing != null) {
            existing.copy(amount = request.amount, period = request.period) // updates existing limit
        } else {
            SpendingLimit(userId = userId, amount = request.amount, period = request.period) // creates new limit
        }

        return ResponseEntity.ok(spendingLimitRepository.save(limit)) // saves and returns the limit
    }

    // handles GET /limits/personal — returns the personal limit for the logged-in user
    @GetMapping("/personal")
    fun getPersonalLimit(
        @RequestHeader("Authorization") authHeader: String // reads the Authorization header
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val limit = spendingLimitRepository.findByUserIdAndGroupIdIsNull(userId) // finds the personal limit
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No personal limit set") // no limit found

        return ResponseEntity.ok(limit) // returns the personal limit
    }

    // handles POST /limits/group/{groupId} — sets or updates a limit for a group or a member within a group
    @PostMapping("/group/{groupId}")
    fun setGroupLimit(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable groupId: Long,                        // reads the group ID from the URL
        @RequestBody request: SetLimitRequest               // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val group = groupRepository.findById(groupId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found")

        // for FAMILY groups — only admins can set limits
        if (group.type == "FAMILY") {
            val membership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId).firstOrNull()
                ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a member of this group")
            if (membership.role != "ADMIN") {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can set limits in a Family group")
            }
        }

        // determines if this is a per-member limit or a group-wide limit
        val targetUserId = request.targetUserId // null means group-wide limit, non-null means per-member limit

        // checks if a limit already exists for this group and user combination
        val existing = if (targetUserId != null) {
            spendingLimitRepository.findByGroupIdAndUserId(groupId, targetUserId) // per-member limit
        } else {
            spendingLimitRepository.findByGroupIdAndUserIdIsNull(groupId) // group-wide limit
        }

        val limit = if (existing != null) {
            existing.copy(amount = request.amount, period = request.period) // updates existing limit
        } else {
            SpendingLimit(
                groupId = groupId,        // links to the group
                userId = targetUserId,    // null for group-wide, user ID for per-member
                amount = request.amount,  // the limit amount
                period = request.period   // the limit period
            )
        }

        return ResponseEntity.ok(spendingLimitRepository.save(limit)) // saves and returns the limit
    }

    // handles GET /limits/group/{groupId} — returns all limits for a group
    @GetMapping("/group/{groupId}")
    fun getGroupLimits(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable groupId: Long                         // reads the group ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val group = groupRepository.findById(groupId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found")

        val allLimits = spendingLimitRepository.findByGroupId(groupId) // fetches all limits for this group

        // for FAMILY groups — members can only see their own limit, admins see all
        if (group.type == "FAMILY") {
            val membership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId).firstOrNull()
                ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a member of this group")

            if (membership.role != "ADMIN") {
                // returns only this member's limit
                val myLimit = allLimits.filter { it.userId == userId }
                return ResponseEntity.ok(myLimit)
            }
        }

        return ResponseEntity.ok(allLimits) // admins and non-family groups see all limits
    }
}

// the request body sent when setting a limit
data class SetLimitRequest(
    val amount: Double,            // the limit amount
    val period: String,            // the period — "DAILY", "WEEKLY", or "MONTHLY"
    val targetUserId: String? = null // the user to set the limit for — null for group-wide limit
)