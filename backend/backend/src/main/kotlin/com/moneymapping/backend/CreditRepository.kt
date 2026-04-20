package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // base interface that provides all standard database operations
import org.springframework.stereotype.Repository              // marks this interface as a Spring-managed repository bean

@Repository
interface CreditRepository : JpaRepository<Credit, Long> { // JpaRepository gives us save(), findAll(), deleteById() etc. for free

    // finds all credits belonging to a specific user — used to fetch credits when calculating the spent amount
    fun findByUserId(userId: String): List<Credit>
}