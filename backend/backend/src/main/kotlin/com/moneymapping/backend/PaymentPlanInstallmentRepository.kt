package com.moneymapping.backend

import org.springframework.data.jpa.repository.JpaRepository // gives us save, findAll, delete etc for free

interface PaymentPlanInstallmentRepository : JpaRepository<PaymentPlanInstallment, Long> {

    // fetches all installments belonging to a specific payment plan
    fun findByPaymentPlanId(paymentPlanId: Long): List<PaymentPlanInstallment>
}