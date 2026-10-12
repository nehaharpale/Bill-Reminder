package com.example.billreminder.data

data class Reminder(
        val id: Long,
        val name: String,
        val description: String,
        val amount: Double?,
        val category: String,
        val reminderDay: Int,
        val reminderEndDay: Int?,
        val reminderTime: String,
        val enabled: Boolean
)

data class Payment(
        val id: Long,
        val reminderId: Long,
        val paymentDate: String,
        val billingMonth: String,
        val amount: Double?,
        val notes: String
)