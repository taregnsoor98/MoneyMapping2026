package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us basic database operations for free

// SpendingLimitRepository gives us all basic database operations for the SpendingLimit entity
// Spring Boot automatically implements this interface — we don't need to write any SQL
interface SpendingLimitRepository : JpaRepository<SpendingLimit, Long> {

    // finds all limits for a specific group
    fun findByGroupId(groupId: Long): List<SpendingLimit>

    // finds a specific limit for a user within a group — used for per-member limits in FAMILY groups
    fun findByGroupIdAndUserId(groupId: Long, userId: String): SpendingLimit?

    // finds the personal limit for a specific user — used for solo limits on the home screen
    fun findByUserIdAndGroupIdIsNull(userId: String): SpendingLimit?

    // finds the group-wide limit — where userId is null — used for FRIEND groups
    fun findByGroupIdAndUserIdIsNull(groupId: Long): SpendingLimit?
}