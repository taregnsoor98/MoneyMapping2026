package com.moneymapping.backend

import com.sirolf2009.modulith.account.GenerateAccessToken // generates access token
import com.sirolf2009.modulith.account.GenerateRefreshToken // generates refresh token
import com.sirolf2009.modulith.account.HashPassword // hashes the password
import com.sirolf2009.modulith.account.dto.Credentials // login/register request body
import com.sirolf2009.modulith.account.dto.LoginResponse // the token response
import com.sirolf2009.modulith.cqrs.execute // executes a command
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID // for generating unique IDs

@RestController
@RequestMapping("/Account")
class AuthController(
    private val userRepository: UserRepository // injected automatically by Spring
) {

    @PostMapping("/register") // handles POST /Account/register
    fun register(@RequestBody credentials: Credentials): LoginResponse {
        val salt = UUID.randomUUID().toString() // generate a random salt
        val id = UUID.randomUUID().toString() // generate a unique user ID
        val hashedPassword = execute(HashPassword(credentials.password, salt)) // hash the password
        val user = User(id, credentials.username, hashedPassword, salt) // create the user object
        userRepository.save(user) // save user to database
        val accessToken = execute(GenerateAccessToken(id)) // generate access token
        val refreshToken = execute(GenerateRefreshToken(id)) // generate refresh token
        return LoginResponse(accessToken, refreshToken) // return tokens
    }
    // note : we need to work on refreshing tokens too
    // paths are inconsistent (as in the first letter is either all Caps, or none, Account, login (standard is no cap)

    @PostMapping("/login") // handles POST /Account/login
    fun login(@RequestBody credentials: Credentials): LoginResponse {
        val user = userRepository.findByUsername(credentials.username)
            .orElseThrow { RuntimeException("User not found") } // find user or throw error
        val hashedPassword = execute(HashPassword(credentials.password, user.salt)) // hash the input password
        if (hashedPassword != user.password) throw RuntimeException("Invalid password") // compare passwords
        val accessToken = execute(GenerateAccessToken(user.id)) // generate access token
        val refreshToken = execute(GenerateRefreshToken(user.id)) // generate refresh token
        return LoginResponse(accessToken, refreshToken) // return tokens
    }
}