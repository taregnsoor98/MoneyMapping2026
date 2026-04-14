package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findAll, delete etc for free

interface PaymentPlanRepository : JpaRepository<PaymentPlan, Long> {

    // fetches all payment plans belonging to a specific group
    fun findByGroupId(groupId: Long): List<PaymentPlan>
}