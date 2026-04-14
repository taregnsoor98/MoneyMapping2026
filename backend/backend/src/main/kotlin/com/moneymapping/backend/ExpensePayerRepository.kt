package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findAll, delete etc for free

interface ExpensePayerRepository : JpaRepository<ExpensePayer, String> {

    // Fetches all payers belonging to a specific expense
    fun findByExpenseId(expenseId: String): List<ExpensePayer>

    // Deletes all payers belonging to a specific expense — used when updating an expense
    fun deleteByExpenseId(expenseId: String)
}