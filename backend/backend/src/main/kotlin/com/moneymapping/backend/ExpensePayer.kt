package com.moneymapping.backend

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "expense_payers")                  // stored in its own table, one row per payer
data class ExpensePayer(

    @Id
    val id: String,                              // unique UUID for this payer record

    val expenseId: String,                       // links back to the parent Expense

    val payerName: String,                       // username or guest name of the person who paid

    val amountPaid: Double                       // how much this person paid toward the expense
)