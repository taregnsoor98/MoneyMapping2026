package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findAll, delete etc for free

interface PaidDebtRepository : JpaRepository<PaidDebt, Long> {

    // fetches all settled debts for a specific group
    fun findByGroupId(groupId: Long): List<PaidDebt>
}