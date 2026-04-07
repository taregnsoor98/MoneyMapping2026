package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findById etc for free

interface ExpenseItemRepository : JpaRepository<ExpenseItem, Long> {

    // Fetches all items belonging to a specific expense
    fun findByExpenseId(expenseId: String): List<ExpenseItem>

    // Deletes all items belonging to a specific expense — used when updating an expense
    fun deleteByExpenseId(expenseId: String)
}