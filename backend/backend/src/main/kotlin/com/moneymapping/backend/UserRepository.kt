package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findById etc for free
import java.util.Optional // used for nullable results

interface UserRepository : JpaRepository<User, String> { // String is the type of our ID field
    fun findByUsername(username: String): Optional<User> // find a user by their username
}