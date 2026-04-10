package com.moneymapping.backend

import com.sirolf2009.modulith.account.VerifyToken        // verifies the JWT access token
import com.sirolf2009.modulith.cqrs.execute               // executes commands from the auth library
import org.springframework.http.HttpStatus                // HTTP status codes like 200, 401, 404
import org.springframework.http.ResponseEntity            // allows returning custom HTTP status codes
import org.springframework.transaction.annotation.Transactional // ensures database operations are atomic
import org.springframework.web.bind.annotation.GetMapping     // marks a function as a GET endpoint
import org.springframework.web.bind.annotation.PathVariable   // reads a value from the URL path
import org.springframework.web.bind.annotation.PostMapping    // marks a function as a POST endpoint
import org.springframework.web.bind.annotation.PutMapping     // marks a function as a PUT endpoint
import org.springframework.web.bind.annotation.RequestBody    // reads the request body as JSON
import org.springframework.web.bind.annotation.RequestHeader  // reads a specific request header
import org.springframework.web.bind.annotation.RequestMapping // sets the base path for all endpoints
import org.springframework.web.bind.annotation.RestController // marks this class as a REST controller

@RestController
@RequestMapping("/groups") // all endpoints in this class start with /groups
class GroupController(
    private val groupRepository: GroupRepository,             // injected automatically by Spring
    private val groupMemberRepository: GroupMemberRepository, // injected automatically by Spring
    private val userRepository: UserRepository                // injected automatically by Spring — used to look up usernames
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

    // looks up the username for a given user ID — returns the ID itself if not found
    private fun getUsernameById(userId: String): String {
        return userRepository.findById(userId).map { it.username }.orElse(userId) // returns username or ID as fallback
    }

    @Transactional // ensures group and its first member are saved together atomically
    @PostMapping // handles POST /groups — creates a new group
    fun createGroup(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @RequestBody request: CreateGroupRequest            // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        // creates and saves the group
        val group = groupRepository.save(
            Group(
                name = request.name,  // the group name
                type = request.type,  // FRIEND, FAMILY, or ONE_TIME
                createdBy = userId    // the logged-in user is the creator
            )
        )

        // adds the creator as the first member only if they are not already in the group
        val alreadyMember = groupMemberRepository.findByGroupIdAndUserId(group.id, userId).isNotEmpty()
        if (!alreadyMember) {
            groupMemberRepository.save(
                GroupMember(
                    groupId = group.id,                                             // links to the newly created group
                    userId = userId,                                                // the creator's user ID
                    guestName = null,                                               // not a guest
                    role = if (request.type == "FAMILY") "ADMIN" else "MEMBER"     // admins in family, member in others
                )
            )
        }

        return ResponseEntity.ok(group.toResponse(listOf(), ::getUsernameById)) // returns the created group
    }

    @GetMapping // handles GET /groups — returns all groups the logged-in user belongs to
    fun getGroups(
        @RequestHeader("Authorization") authHeader: String // reads the Authorization header
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        val allGroups = groupRepository.findAll() // fetches all groups from the database

        // filters groups where the user is a member — checks if the list is not empty
        val userGroups = allGroups.filter { group ->
            groupMemberRepository.findByGroupIdAndUserId(group.id, userId).isNotEmpty() // checks if user is a member
        }

        return ResponseEntity.ok(userGroups.map { group ->
            val members = groupMemberRepository.findByGroupId(group.id) // fetches members for each group
            group.toResponse(members, ::getUsernameById)                 // converts to response object with usernames
        })
    }

    @Transactional // ensures member is added atomically
    @PostMapping("/{id}/members") // handles POST /groups/{id}/members — adds a member to a group
    fun addMember(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: Long,                             // reads the group ID from the URL
        @RequestBody request: AddMemberRequest              // reads the request body as JSON
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        groupRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found")

        // checks if the user is already a member — skips saving if they are
        if (request.userId != null) {
            val alreadyMember = groupMemberRepository.findByGroupIdAndUserId(id, request.userId).isNotEmpty()
            if (alreadyMember) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User is already a member of this group") // blocks duplicates
            }
        }

        // saves the new member
        val member = groupMemberRepository.save(
            GroupMember(
                groupId = id,                  // links to the group
                userId = request.userId,       // null if guest
                guestName = request.guestName, // null if real user
                role = "MEMBER"                // new members always start as MEMBER
            )
        )

        return ResponseEntity.ok(member) // returns the saved member
    }

    @Transactional // ensures role update is atomic
    @PutMapping("/{id}/members/{memberId}/promote") // handles PUT /groups/{id}/members/{memberId}/promote
    fun promoteMember(
        @RequestHeader("Authorization") authHeader: String, // reads the Authorization header
        @PathVariable id: Long,                             // reads the group ID from the URL
        @PathVariable memberId: Long                        // reads the member ID from the URL
    ): ResponseEntity<Any> {
        val userId = getUserId(authHeader)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token")

        // checks that the requester is an admin of this group — gets first result from the list
        val requesterMembership = groupMemberRepository.findByGroupIdAndUserId(id, userId).firstOrNull()
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a member of this group")

        if (requesterMembership.role != "ADMIN") {                                        // blocks non-admins
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can promote members")
        }

        val member = groupMemberRepository.findById(memberId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found")

        val promoted = member.copy(role = "ADMIN") // promotes the member to admin
        groupMemberRepository.save(promoted)        // saves the updated role

        return ResponseEntity.ok(promoted) // returns the promoted member
    }
}

// the request body sent when creating a group
data class CreateGroupRequest(
    val name: String, // the group name
    val type: String  // FRIEND, FAMILY, or ONE_TIME
)

// the request body sent when adding a member to a group
data class AddMemberRequest(
    val userId: String?,   // the user ID — null if adding a guest
    val guestName: String? // the guest name — null if adding a real user
)

// the response object returned to the client for a group member
data class GroupMemberResponse(
    val id: Long,           // the membership ID
    val userId: String?,    // the user ID — null if guest
    val username: String?,  // the username — null if guest
    val guestName: String?, // the guest name — null if real user
    val role: String        // ADMIN or MEMBER
)

// the response object returned to the client for a group
data class GroupResponse(
    val id: Long,                          // the group ID
    val name: String,                      // the group name
    val type: String,                      // FRIEND, FAMILY, or ONE_TIME
    val createdBy: String,                 // the user ID of the creator
    val isLocked: Boolean,                 // true if the group is closed and read-only
    val members: List<GroupMemberResponse> // list of members in this group
)

// converts a Group entity to a GroupResponse safe to send to the client
// takes a function to look up usernames by user ID
fun Group.toResponse(members: List<GroupMember>, getUsernameById: (String) -> String) = GroupResponse(
    id = id,                          // the group ID
    name = name,                      // the group name
    type = type,                      // the group type
    createdBy = createdBy,            // the creator's user ID
    isLocked = isLocked,              // whether the group is locked
    members = members.map { member -> // converts each member to a response object
        GroupMemberResponse(
            id = member.id,                                                              // membership ID
            userId = member.userId,                                                      // user ID or null
            username = member.userId?.let { getUsernameById(it) },                       // looks up username by user ID
            guestName = member.guestName,                                                // guest name or null
            role = member.role                                                           // ADMIN or MEMBER
        )
    }
)