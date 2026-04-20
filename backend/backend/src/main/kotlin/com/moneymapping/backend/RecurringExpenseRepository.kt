package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // provides all standard CRUD operations out of the box

interface RecurringExpenseRepository : JpaRepository<RecurringExpense, Long> { // Long is the type of the primary key

    fun findByUserId(userId: String): List<RecurringExpense> // fetches all recurring expenses belonging to a specific user
}