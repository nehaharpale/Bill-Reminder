
package com.example.billreminder

import com.example.billreminder.data.Database
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.time.LocalDate
import java.time.LocalTime
import java.util.Timer
import java.util.TimerTask

class ReminderScheduler(
        private val db: Database,
        private val onReminder: (String) -> Unit
) {
    private val timer = Timer("reminder-checker", true)

    fun start() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    checkReminders()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 0L, 60_000L)
    }

    private fun checkReminders() {
        val now = LocalDate.now()

        val currentTime = LocalTime.now()

        db.getReminders()
                .filter { it.enabled }
                .forEach { reminder ->
                    try {
                        val inWindow =
                                if (reminder.reminderEndDay == null) {
                                    now.dayOfMonth == reminder.reminderDay
                                } else {
                                    now.dayOfMonth in
                                            reminder.reminderDay..reminder.reminderEndDay
                                }

                        val configuredTime = LocalTime.parse(
                                reminder.reminderTime.trim().let { time ->
                                    if (time.matches(Regex("\\d{1}:\\d{2}"))) {
                                        "0$time"
                                    } else {
                                        time
                                    }
                                }
                        )

                        val reminderTimeReached =
                                !currentTime.isBefore(configuredTime)

                        if (
                                inWindow &&
                                reminderTimeReached &&
                                !db.wasReminderNotifiedOnDate(reminder.id, now)
                        ) {

                            val message =
                                    "${reminder.name} reminder: ${reminder.description}"

                            // Persist the notification date before sending.
                            db.recordReminderNotification(reminder.id, now)

                            onReminder(message)
                            showSystemNotification(reminder.name, message)
                        }
                    } catch (e: Exception) {
                        System.err.println(
                                "Failed to process reminder ${reminder.id}: ${e.message}"
                        )
                    }
                }
    }



    private fun showSystemNotification(
            title: String,
            message: String
    ) {
        try {
            val script = """
            display notification "${message
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")}" with title "${title
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")}"
        """.trimIndent()

            val process = ProcessBuilder(
                    "/usr/bin/osascript",
                    "-e",
                    script
            )
                    .redirectErrorStream(true)
                    .start()

            val output = process.inputStream.bufferedReader().use {
                it.readText()
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                println("macOS notification command succeeded: $title")
            } else {
                println(
                        "macOS notification failed (exit $exitCode): $output"
                )
            }
        } catch (e: Exception) {
            println("Unable to send macOS notification: ${e.message}")
        }
    }

}
