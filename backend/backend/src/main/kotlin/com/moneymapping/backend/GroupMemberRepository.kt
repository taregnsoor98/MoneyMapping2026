package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us basic database operations for free

// GroupMemberRepository gives us all basic database operations for the GroupMember entity
// Spring Boot automatically implements this interface — we don't need to write any SQL
interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    // finds all members belonging to a specific group
    fun findByGroupId(groupId: Long): List<GroupMember>

    // finds memberships by group ID and user ID — returns a list to avoid errors if duplicates exist
    fun findByGroupIdAndUserId(groupId: Long, userId: String): List<GroupMember>

    // deletes a specific membership by group ID and user ID
    fun deleteByGroupIdAndUserId(groupId: Long, userId: String)
}