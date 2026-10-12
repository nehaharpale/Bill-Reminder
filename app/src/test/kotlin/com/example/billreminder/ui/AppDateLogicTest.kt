package com.example.billreminder.ui

import com.example.billreminder.data.Reminder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.LocalDate

class AppDateLogicTest {

    private fun reminder(
        name: String,
        reminderDay: Int,
        reminderEndDay: Int? = null
    ): Reminder {
        return Reminder(
            id = 999,
            name = name,
            description = "",
            amount = null,
            category = "Utilities",
            reminderDay = reminderDay,
            reminderEndDay = reminderEndDay,
            reminderTime = "09:00",
            enabled = true
        )
    }

    @Test
    fun rent_range_is_not_overdue_before_or_on_end_date() {
        val rent = reminder(
            name = "Rent",
            reminderDay = 1,
            reminderEndDay = 5
        )

        assertFalse(
            isOverdue(
                rent,
                LocalDate.of(2026, 10, 4)
            )
        )

        assertFalse(
            isOverdue(
                rent,
                LocalDate.of(2026, 10, 5)
            )
        )
    }

    @Test
    fun rent_range_becomes_overdue_after_end_date() {
        val rent = reminder(
            name = "Rent",
            reminderDay = 1,
            reminderEndDay = 5
        )

        assertTrue(
            isOverdue(
                rent,
                LocalDate.of(2026, 10, 6)
            )
        )
    }

    @Test
    fun rent_month_boundary_starts_new_cycle() {
        val rent = reminder(
            name = "Rent",
            reminderDay = 1,
            reminderEndDay = 5
        )

        val novemberFirst = LocalDate.of(2026, 11, 1)

        assertEquals(
            LocalDate.of(2026, 11, 5),
            dueDateForMonth(
                rent,
                novemberFirst.year,
                novemberFirst.monthValue
            )
        )

        assertFalse(
            isOverdue(
                rent,
                novemberFirst
            )
        )

        assertEquals(
            4,
            daysUntilDue(
                rent,
                novemberFirst
            )
        )
    }

    @Test
    fun rent_is_overdue_again_after_new_month_due_date() {
        val rent = reminder(
            name = "Rent",
            reminderDay = 1,
            reminderEndDay = 5
        )

        assertFalse(
            isOverdue(
                rent,
                LocalDate.of(2026, 11, 5)
            )
        )

        assertTrue(
            isOverdue(
                rent,
                LocalDate.of(2026, 11, 6)
            )
        )
    }

    @Test
    fun day_31_bill_is_due_on_october_31() {
        val bill = reminder(
            name = "Day 31 Bill",
            reminderDay = 31
        )

        assertEquals(
            LocalDate.of(2026, 10, 31),
            dueDateForMonth(
                bill,
                2026,
                10
            )
        )

        assertEquals(
            1,
            daysUntilDue(
                bill,
                LocalDate.of(2026, 10, 30)
            )
        )

        assertEquals(
            0,
            daysUntilDue(
                bill,
                LocalDate.of(2026, 10, 31)
            )
        )

        assertFalse(
            isOverdue(
                bill,
                LocalDate.of(2026, 10, 31)
            )
        )
    }

    @Test
    fun day_31_bill_clamps_to_november_30() {
        val bill = reminder(
            name = "Day 31 Bill",
            reminderDay = 31
        )

        assertEquals(
            LocalDate.of(2026, 11, 30),
            dueDateForMonth(
                bill,
                2026,
                11
            )
        )

        assertEquals(
            29,
            daysUntilDue(
                bill,
                LocalDate.of(2026, 11, 1)
            )
        )

        assertEquals(
            0,
            daysUntilDue(
                bill,
                LocalDate.of(2026, 11, 30)
            )
        )

        assertFalse(
            isOverdue(
                bill,
                LocalDate.of(2026, 11, 30)
            )
        )
    }

    @Test
    fun day_31_bill_resets_to_december_cycle() {
        val bill = reminder(
            name = "Day 31 Bill",
            reminderDay = 31
        )

        val decemberFirst = LocalDate.of(2026, 12, 1)

        assertEquals(
            LocalDate.of(2026, 12, 31),
            dueDateForMonth(
                bill,
                decemberFirst.year,
                decemberFirst.monthValue
            )
        )

        assertEquals(
            30,
            daysUntilDue(
                bill,
                decemberFirst
            )
        )

        assertFalse(
            isOverdue(
                bill,
                decemberFirst
            )
        )
    }

    @Test
    fun day_31_bill_clamps_to_february_28() {
        val bill = reminder(
            name = "Day 31 Bill",
            reminderDay = 31
        )

        assertEquals(
            LocalDate.of(2027, 2, 28),
            dueDateForMonth(
                bill,
                2027,
                2
            )
        )

        assertEquals(
            1,
            daysUntilDue(
                bill,
                LocalDate.of(2027, 2, 27)
            )
        )

        assertEquals(
            0,
            daysUntilDue(
                bill,
                LocalDate.of(2027, 2, 28)
            )
        )

        assertFalse(
            isOverdue(
                bill,
                LocalDate.of(2027, 2, 28)
            )
        )
    }

    @Test
    fun march_starts_new_cycle_after_february() {
        val bill = reminder(
            name = "Day 31 Bill",
            reminderDay = 31
        )

        val marchFirst = LocalDate.of(2027, 3, 1)

        assertEquals(
            LocalDate.of(2027, 3, 31),
            dueDateForMonth(
                bill,
                marchFirst.year,
                marchFirst.monthValue
            )
        )

        assertEquals(
            30,
            daysUntilDue(
                bill,
                marchFirst
            )
        )

        assertFalse(
            isOverdue(
                bill,
                marchFirst
            )
        )
    }
}
