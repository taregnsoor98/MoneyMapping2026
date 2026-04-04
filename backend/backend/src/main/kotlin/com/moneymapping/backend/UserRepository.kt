package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findById etc for free
import org.springframework.data.jpa.repository.Query // allows us to write custom database queries
import java.util.Optional // used for nullable results

interface UserRepository : JpaRepository<User, String> {
    fun findByUsername(username: String): Optional<User> // find user by username
    fun findByEmail(email: String): Optional<User> // find user by email
    fun findByConfirmationToken(token: String): Optional<User> // find user by their confirmation token

    // Searches for users whose username or email contains the search query (case insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByUsernameOrEmail(query: String): List<User>
}