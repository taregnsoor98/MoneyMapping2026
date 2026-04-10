package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us basic database operations for free

// GroupRepository gives us all basic database operations for the Group entity
// Spring Boot automatically implements this interface — we don't need to write any SQL
interface GroupRepository : JpaRepository<Group, Long> {

    // finds all groups where the createdBy field matches the given userId
    fun findByCreatedBy(createdBy: String): List<Group>
}