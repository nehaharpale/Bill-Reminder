package com.example.billreminder.test

import com.example.billreminder.data.Reminder
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun dueDateForMonth(
    reminder: Reminder,
    year: Int,
    month: Int
): LocalDate {

    val monthDate = LocalDate.of(year, month, 1)

    val dueDay =
        (reminder.reminderEndDay ?: reminder.reminderDay)
            .coerceAtMost(monthDate.lengthOfMonth())

    return monthDate.withDayOfMonth(dueDay)
}

private fun daysUntilDue(
    reminder: Reminder,
    today: LocalDate
): Long {

    var target =
        dueDateForMonth(
            reminder,
            today.year,
            today.monthValue
        )

    if (target.isBefore(today)) {

        val nextMonth = today.plusMonths(1)

        target =
            dueDateForMonth(
                reminder,
                nextMonth.year,
                nextMonth.monthValue
            )
    }

    return ChronoUnit.DAYS.between(today, target)
}

private fun isOverdue(
    reminder: Reminder,
    today: LocalDate
): Boolean {

    val dueDate =
        dueDateForMonth(
            reminder,
            today.year,
            today.monthValue
        )

    return today.isAfter(dueDate)
}

fun main() {

    val day31Bill = Reminder(
        id = 99,
        name = "Test Day 31",
        description = "",
        amount = 100.0,
        category = "Test",
        reminderDay = 31,
        reminderEndDay = null,
        reminderTime = "09:00",
        enabled = true
    )

    val rent = Reminder(
        id = 100,
        name = "Test Rent",
        description = "",
        amount = 100.0,
        category = "Test",
        reminderDay = 1,
        reminderEndDay = 5,
        reminderTime = "09:00",
        enabled = true
    )

    println("=== MONTH BOUNDARY TESTS ===")

    val tests = listOf(
        "Oct 30, day-31 bill" to LocalDate.of(2026, 10, 30),
        "Oct 31, day-31 bill" to LocalDate.of(2026, 10, 31),
        "Nov 1, day-31 bill" to LocalDate.of(2026, 11, 1),
        "Nov 30, day-31 bill" to LocalDate.of(2026, 11, 30),
        "Dec 1, day-31 bill" to LocalDate.of(2026, 12, 1),
        "Feb 27, day-31 bill" to LocalDate.of(2027, 2, 27),
        "Feb 28, day-31 bill" to LocalDate.of(2027, 2, 28),
        "Mar 1, day-31 bill" to LocalDate.of(2027, 3, 1)
    )

    for ((label, date) in tests) {

        val dueDate =
            dueDateForMonth(
                day31Bill,
                date.year,
                date.monthValue
            )

        val overdue =
            isOverdue(
                day31Bill,
                date
            )

        val days =
            daysUntilDue(
                day31Bill,
                date
            )

        println(
            "$label | due=$dueDate | daysUntil=$days | overdue=$overdue"
        )
    }

    println()
    println("=== RENT RANGE TESTS ===")

    val rentTests = listOf(
        "Oct 4" to LocalDate.of(2026, 10, 4),
        "Oct 5" to LocalDate.of(2026, 10, 5),
        "Oct 6" to LocalDate.of(2026, 10, 6),
        "Nov 1" to LocalDate.of(2026, 11, 1),
        "Nov 5" to LocalDate.of(2026, 11, 5),
        "Nov 6" to LocalDate.of(2026, 11, 6)
    )

    for ((label, date) in rentTests) {

        val dueDate =
            dueDateForMonth(
                rent,
                date.year,
                date.monthValue
            )

        val overdue =
            isOverdue(
                rent,
                date
            )

        println(
            "$label | due=$dueDate | overdue=$overdue"
        )
    }
}
