package com.example.billreminder

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.billreminder.data.Database
import com.example.billreminder.ui.App

fun main() = application {
    val db = Database()

    ReminderScheduler(db) { }.start()

    Window(
        onCloseRequest = {
            db.close()
            exitApplication()
        },
        title = "Bill & Rent Reminder"
    ) {
        App(db)
    }
}
