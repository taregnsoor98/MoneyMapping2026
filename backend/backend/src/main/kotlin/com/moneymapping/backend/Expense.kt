package com.moneymapping.backend

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "expenses")
data class Expense(

    @Id
    val id: String,                          // unique UUID for this expense

    val paidBy: String,                      // user ID of whoever paid

    val groupId: Int? = null,                // optional — null if solo expense

    val amount: Double,                      // total amount of the expense

    val currency: String,                    // e.g. "USD", "EUR"

    val description: String,                 // what the expense was for

    val category: String,                    // e.g. "Food", "Transport"

    val date: LocalDate,                     // the date the expense occurred

    val isOneTimeSplit: Boolean = false,      // true if this was a one-time split

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_receipt_images", joinColumns = [JoinColumn(name = "expense_id")])
    @Column(name = "image_uri")
    val receiptImages: List<String> = emptyList() // optional receipt image URIs
)