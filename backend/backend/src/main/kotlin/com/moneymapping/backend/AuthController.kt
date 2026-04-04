package com.moneymapping.backend

import com.sirolf2009.modulith.account.GenerateAccessToken // generates a short-lived access token
import com.sirolf2009.modulith.account.GenerateRefreshToken // generates a long-lived refresh token
import com.sirolf2009.modulith.account.HashPassword // hashes the password with a salt
import com.sirolf2009.modulith.account.VerifyToken // verifies a JWT token is valid
import com.sirolf2009.modulith.account.dto.LoginResponse // the response containing access and refresh tokens
import com.sirolf2009.modulith.cqrs.execute // executes a command from the auth library
import org.springframework.http.HttpStatus // HTTP status codes like 200, 400, 500
import org.springframework.http.ResponseEntity // allows returning custom HTTP status codes with responses
import org.springframework.web.bind.annotation.GetMapping // marks a function as a GET endpoint
import org.springframework.web.bind.annotation.PostMapping // marks a function as a POST endpoint
import org.springframework.web.bind.annotation.RequestBody // reads the request body as JSON
import org.springframework.web.bind.annotation.RequestHeader // reads a specific request header
import org.springframework.web.bind.annotation.RequestMapping // sets the base path for all endpoints in this class
import org.springframework.web.bind.annotation.RequestParam // reads a query parameter from the URL
import org.springframework.web.bind.annotation.RestController // marks this class as a REST controller
import java.util.UUID // for generating unique IDs

@RestController
@RequestMapping("/account") // all endpoints in this class start with /account
class AuthController(
    private val userRepository: UserRepository, // injected automatically by Spring — handles database operations
    private val emailService: EmailService // injected automatically by Spring — handles sending emails
) {

    @PostMapping("/register") // handles POST /account/register
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<String> {
        if (userRepository.findByEmail(request.email).isPresent) { // check if email already exists in database
            return ResponseEntity.badRequest().body("This email is already registered") // block duplicate emails
        }
        if (userRepository.findByUsername(request.username).isPresent) { // check if username already exists in database
            return ResponseEntity.badRequest().body("This username is already taken") // block duplicate usernames
        }
        val salt = UUID.randomUUID().toString() // generate a random salt for password hashing
        val id = UUID.randomUUID().toString() // generate a unique ID for this user
        val hashedPassword = execute(HashPassword(request.password, salt)) // hash the password with the salt
        val confirmationToken = UUID.randomUUID().toString() // generate a unique token for email confirmation
        val user = User(id, request.username, hashedPassword, salt, request.email, confirmationToken, false) // create user, not confirmed yet
        userRepository.save(user) // save user to database
        emailService.sendConfirmationEmail(request.email, confirmationToken) // send confirmation email in background
        return ResponseEntity.ok("Please check your email to confirm your account") // respond immediately without waiting for email
    }

    @GetMapping("/confirm") // handles GET /account/confirm?token=xxx
    fun confirm(@RequestParam token: String): ResponseEntity<String> {
        val user = userRepository.findByConfirmationToken(token)
            .orElseThrow { RuntimeException("Invalid or expired confirmation link") } // find user by token or fail
        val confirmedUser = user.copy(confirmed = true, confirmationToken = null) // mark user as confirmed and clear token
        userRepository.save(confirmedUser) // save updated user to database
        return ResponseEntity.ok("Email confirmed! You can now login.") // success message
    }

    @PostMapping("/login") // handles POST /account/login
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        val user = userRepository.findByEmail(request.emailOrUsername) // try finding user by email first
            .orElseGet { userRepository.findByUsername(request.emailOrUsername).orElse(null) } // fall back to username
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No account found with this email or username") // no user found

        if (!user.confirmed) { // check if user has confirmed their email
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please confirm your email before logging in") // block unconfirmed users
        }

        val hashedPassword = execute(HashPassword(request.password, user.salt)) // hash the entered password with stored salt
        if (hashedPassword != user.password) { // compare hashed passwords
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect password") // wrong password
        }

        val accessToken = execute(GenerateAccessToken(user.id)) // generate a new access token
        val refreshToken = execute(GenerateRefreshToken(user.id)) // generate a new refresh token
        return ResponseEntity.ok(LoginResponse(accessToken, refreshToken)) // return tokens to the client
    }

    @PostMapping("/refresh") // handles POST /account/refresh
    fun refresh(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Any> {
        val token = authHeader.removePrefix("Bearer ") // extract the token from the Authorization header
        val tokenData = execute(VerifyToken(token)) // verify the token is valid
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token") // token is invalid
        val newAccessToken = execute(GenerateAccessToken(tokenData.userId)) // generate a new access token
        val newRefreshToken = execute(GenerateRefreshToken(tokenData.userId)) // generate a new refresh token
        return ResponseEntity.ok(LoginResponse(newAccessToken, newRefreshToken)) // return new tokens
    }

    @GetMapping("/search") // handles GET /account/search?query=xxx
    fun searchUsers(@RequestParam query: String): ResponseEntity<List<UserSearchResult>> {
        if (query.length < 2) { // require at least 2 characters to search
            return ResponseEntity.badRequest().build() // reject too-short queries
        }
        val results = userRepository.searchByUsernameOrEmail(query) // searches database for matching users
            .map { UserSearchResult(it.id, it.username, it.email) } // maps to a safe response without password
        return ResponseEntity.ok(results) // returns the list of matching users
        // there should be a limit to how many people can show in the search (also make it that its more than 2 characters for the search
        // check the notebook for other comments, also maybe dont show users until its almost a full match, like dont give the chance to see many users until you write most letters
    }
}

// the body sent when registering - contains email, username, and password
data class RegisterRequest(
    val email: String, // the email address used to confirm and login
    val username: String, // the chosen username
    val password: String // the chosen password
)

// the body sent when logging in - accepts either email or username plus password
data class LoginRequest(
    val emailOrUsername: String, // the user can type either their email or username here
    val password: String // the password
)

// the response returned when searching for users — never includes password or sensitive data
data class UserSearchResult(
    val id: String,       // the user's unique id
    val username: String, // the user's username
    val email: String     // the user's email
)