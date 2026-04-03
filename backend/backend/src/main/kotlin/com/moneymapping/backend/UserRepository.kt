package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findById etc for free
import java.util.Optional // used for nullable results

interface UserRepository : JpaRepository<User, String> {
    fun findByUsername(username: String): Optional<User> // find user by username
    fun findByEmail(email: String): Optional<User> // find user by email
    fun findByConfirmationToken(token: String): Optional<User> // find user by their confirmation token
}