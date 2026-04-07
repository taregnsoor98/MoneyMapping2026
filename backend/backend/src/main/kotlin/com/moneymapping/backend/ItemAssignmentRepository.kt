package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findAll, delete etc for free
import org.springframework.data.jpa.repository.Modifying     // marks a query that modifies data
import org.springframework.data.jpa.repository.Query         // allows us to write custom database queries
import org.springframework.transaction.annotation.Transactional // ensures the delete runs in a transaction

interface ItemAssignmentRepository : JpaRepository<ItemAssignment, Long> {

    // Fetches all assignments for a specific expense item
    fun findByExpenseItemId(expenseItemId: Long): List<ItemAssignment>

    // Deletes all assignments for a specific expense item — used when updating or deleting an item
    @Transactional
    @Modifying
    @Query("DELETE FROM ItemAssignment a WHERE a.expenseItemId = :expenseItemId")
    fun deleteByExpenseItemId(expenseItemId: Long)
}