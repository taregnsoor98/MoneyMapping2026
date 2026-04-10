package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ExpenseRepository : JpaRepository<Expense, String> {

    // Fetches all expenses paid by a specific user, newest first
    fun findByPaidByOrderByDateDesc(paidBy: String): List<Expense>
    // Fetches a single expense by its ID and owner — used for edit/delete authorization
    fun findByIdAndPaidBy(id: String, paidBy: String): Optional<Expense>
    // fetches all expenses belonging to a specific group, newest first
    fun findByGroupIdOrderByDateDesc(groupId: Long): List<Expense>
}