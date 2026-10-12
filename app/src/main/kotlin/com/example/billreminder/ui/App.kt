
package com.example.billreminder.ui


import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.nio.file.Path
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.billreminder.data.Database
import com.example.billreminder.data.Reminder
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.ui.Alignment

private val AppBackground = Color(0xFFF7F7F2)
private val ForestGreen = Color(0xFF28634E)
private val MutedText = Color(0xFF68736D)
private val CardBorder = Color(0xFFE5E9E3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(db: Database) {
    var refresh by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<java.nio.file.Path?>(null) }


    fun chooseBackupDestination(): Path? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Back Up Bill Reminder"
            dialogType = JFileChooser.SAVE_DIALOG
            fileFilter = FileNameExtensionFilter("SQLite database (*.db)", "db")
            selectedFile = File("bill-rent-reminder-backup.db")
        }

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null
        }

        var file = chooser.selectedFile
        if (!file.name.endsWith(".db", ignoreCase = true)) {
            file = File(file.parentFile, "${file.name}.db")
        }

        if (file.exists()) {
            backupMessage = "That file already exists. Choose a new filename."
            return null
        }

        return file.toPath()
    }

    fun chooseRestoreSource(): Path? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Select a Bill Reminder Backup"
            dialogType = JFileChooser.OPEN_DIALOG
            fileFilter = FileNameExtensionFilter("SQLite database (*.db)", "db")
        }

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null
        }

        return chooser.selectedFile.toPath()
    }


    // Reload database-backed data after every successful change or refresh.
    val reminders = remember(refresh) { db.getReminders() }
    val payments = remember(refresh) { db.getPayments() }

    val today = LocalDate.now()
    val currentBillingMonth = "%04d-%02d".format(
            today.year,
            today.monthValue
    )

    val paidReminderIds = remember(refresh, reminders, currentBillingMonth) {
        reminders
                .filter {
                    db.isPaidForBillingMonth(it.id, currentBillingMonth)
                }
                .map { it.id }
                .toSet()
    }

    val paidCount = paidReminderIds.size

    val overdueCount = reminders.count { reminder ->
        reminder.enabled &&
                reminder.id !in paidReminderIds &&
                isOverdue(reminder, today)
    }

    val upcomingCount = reminders.count { reminder ->
        reminder.enabled &&
                reminder.id !in paidReminderIds &&
                !isOverdue(reminder, today)
    }

    MaterialTheme(
            colorScheme = lightColorScheme(
                    primary = ForestGreen,
                    onPrimary = Color.White,
                    background = AppBackground,
                    surface = Color.White,
                    onSurface = Color(0xFF202923),
                    surfaceVariant = Color(0xFFEAF0E9),
                    onSurfaceVariant = MutedText,
                    outline = CardBorder
            )
    ) {
        Scaffold(
                containerColor = AppBackground,
                topBar = {
                    TopAppBar(
                            title = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                            "Bill Reminder",
                                            style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                            "YOUR MONTHLY OVERVIEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedText
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = AppBackground,
                                    titleContentColor = ForestGreen
                            )
                    )
                }
        ) { padding ->
            Column(
                    modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                            text = today.month.name
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() } + " ${today.year}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF202923)
                    )
                    Text(
                            "Here is the status of your bills.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                    )
                }

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                            title = "Upcoming",
                            value = upcomingCount.toString(),
                            accent = Color(0xFFB7791F),
                            modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                            title = "Overdue",
                            value = overdueCount.toString(),
                            accent = Color(0xFFB54747),
                            modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                            title = "Paid",
                            value = paidCount.toString(),
                            accent = ForestGreen,
                            modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                            title = "Total Bills",
                            value = reminders.size.toString(),
                            accent = Color(0xFF536779),
                            modifier = Modifier.weight(1f)
                    )
                }


                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                                "Your bills",
                                style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                                "${reminders.size} bills in your list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                                onClick = {
                                    try {
                                        val destination = chooseBackupDestination()
                                        if (destination != null) {
                                            db.backupToFile(destination)
                                            backupMessage = "Backup saved successfully."
                                        }
                                    } catch (e: Exception) {
                                        backupMessage = "Backup failed: ${e.message}"
                                    }
                                }
                        ) {
                            Text("Back Up")
                        }

                        OutlinedButton(
                                onClick = {
                                    try {
                                        val source = chooseRestoreSource()
                                        if (source != null) {
                                            db.validateBackup(source)
                                            pendingRestoreFile = source
                                            showRestoreConfirmation = true
                                            backupMessage = null
                                        }
                                    } catch (e: Exception) {
                                        backupMessage = "Invalid backup: ${e.message}"
                                    }
                                }
                        ) {
                            Text("Restore")
                        }

                        Button(
                                onClick = { showAdd = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(
                                        horizontal = 18.dp,
                                        vertical = 12.dp
                                )
                        ) {
                            Text("+ Add Bill")
                        }
                    }
                }


                backupMessage?.let { message ->
                    Text(
                            text = message,
                            color = if (message.startsWith("Backup failed") ||
                                    message.startsWith("Invalid backup")) {
                                MaterialTheme.colorScheme.error
                            } else {
                                ForestGreen
                            },
                            style = MaterialTheme.typography.bodyMedium
                    )
                }


                LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (reminders.isEmpty()) {
                        item {
                            Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                            "No bills yet",
                                            style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                            "Add your first bill to start tracking due dates and payments.",
                                            color = MutedText
                                    )
                                }
                            }
                        }
                    }

                    items(
                            items = reminders,
                            key = { it.id }
                    ) { reminder ->
                        ReminderCard(
                                reminder = reminder,
                                db = db,
                                paid = reminder.id in paidReminderIds,
                                onChanged = { refresh++ }
                        )
                    }

                    if (payments.isNotEmpty()) {
                        item {
                            Text(
                                    "Recent payments",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(payments.take(5)) { payment ->
                            Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Row(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                                payment.billingMonth,
                                                style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                                payment.notes.ifBlank { "Bill payment" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MutedText
                                        )
                                    }

                                    Text(
                                            payment.amount?.let { "$%.2f".format(it) }
                                                    ?: "Amount not entered",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ForestGreen
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                        onClick = { refresh++ },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Refresh")
                }
            }
        }

        if (showAdd) {
            AddReminderDialog(
                    onDismiss = { showAdd = false },
                    onSave = { name, description, amount, category, day, endDay, time ->
                        db.addReminder(
                                name = name,
                                description = description,
                                amount = amount,
                                category = category,
                                day = day,
                                endDay = endDay,
                                time = time
                        )
                        showAdd = false
                        refresh++
                    }
            )
        }

        if (showRestoreConfirmation) {
            AlertDialog(
                    onDismissRequest = {
                        showRestoreConfirmation = false
                        pendingRestoreFile = null
                    },
                    title = {
                        Text("Restore database?")
                    },
                    text = {
                        Text(
                                "This will replace your current bills and payment history " +
                                        "with the selected backup. A safety backup of your " +
                                        "current database will be created first."
                        )
                    },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    val source = pendingRestoreFile
                                    showRestoreConfirmation = false
                                    pendingRestoreFile = null

                                    if (source != null) {
                                        try {
                                            val safetyBackup = db.restoreFromFile(source)
                                            refresh++
                                            backupMessage =
                                                    "Restore successful. Safety backup: " +
                                                            safetyBackup.fileName
                                        } catch (e: Exception) {
                                            backupMessage =
                                                    "Restore failed: ${e.message}"
                                        }
                                    }
                                }
                        ) {
                            Text("Restore")
                        }
                    },
                    dismissButton = {
                        TextButton(
                                onClick = {
                                    showRestoreConfirmation = false
                                    pendingRestoreFile = null
                                }
                        ) {
                            Text("Cancel")
                        }
                    }
            )
        }

    }
}

@Composable
private fun SummaryCard(
        title: String,
        value: String,
        accent: Color,
        modifier: Modifier = Modifier
) {
    Card(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                    containerColor = Color.White
            ),
            border = BorderStroke(1.dp, CardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedText
            )
            Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent
            )
        }
    }
}



@Composable
private fun ReminderCard(
        reminder: Reminder,
        db: Database,
        paid: Boolean,
        onChanged: () -> Unit
) {
    var showPay by remember { mutableStateOf(false) }
    var showDuplicatePayment by remember { mutableStateOf(false) }
    var duplicateBillingMonth by remember { mutableStateOf("") }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val days = daysUntilDue(reminder, today)

    val overdue = !paid &&
            reminder.enabled &&
            isOverdue(reminder, today)

    val statusText = when {
        !reminder.enabled -> "DISABLED"
        paid -> "PAID THIS MONTH"
        overdue -> "OVERDUE"
        days == 0L -> "DUE TODAY"
        days == 1L -> "DUE TOMORROW"
        else -> "$days DAYS REMAINING"
    }

    val statusColor = when {
        !reminder.enabled -> MutedText
        paid -> ForestGreen
        overdue -> Color(0xFFB54747)
        else -> Color(0xFFB7791F)
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                    containerColor = Color.White
            ),
            border = BorderStroke(1.dp, CardBorder),
            elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
            )
    ) {
        Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                            text = reminder.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF202923)
                    )

                    if (reminder.category.isNotBlank()) {
                        Text(
                                text = reminder.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                reminder.amount?.let {
                    Text(
                            text = "$%.2f".format(it),
                            style = MaterialTheme.typography.titleLarge,
                            color = ForestGreen
                    )
                }
            }

            if (reminder.description.isNotBlank()) {
                Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4F5B53)
                )
            }

            Text(
                    text = if (reminder.reminderEndDay == null) {
                        "Due on the ${reminder.reminderDay}${ordinal(reminder.reminderDay)}"
                    } else {
                        "Payment window: " +
                                "${reminder.reminderDay}${ordinal(reminder.reminderDay)}–" +
                                "${reminder.reminderEndDay}${ordinal(reminder.reminderEndDay)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
            )

            Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.10f)
            ) {
                Text(
                        text = statusText,
                        modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                )
            }

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!paid) {
                    Button(
                            onClick = { showPay = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(
                                    horizontal = 14.dp,
                                    vertical = 10.dp
                            )
                    ) {
                        Text("Mark as Paid")
                    }
                } else {
                    OutlinedButton(
                            onClick = { showPay = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                            )
                    ) {
                        Text("Mark as Unpaid")
                    }
                }

                OutlinedButton(
                        onClick = { showEdit = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(
                                horizontal = 14.dp,
                                vertical = 10.dp
                        )
                ) {
                    Text("Edit")
                }

                OutlinedButton(
                        onClick = { showDelete = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(
                                horizontal = 14.dp,
                                vertical = 10.dp
                        )
                ) {
                    Text("Delete")
                }
            }

            OutlinedButton(
                    onClick = {
                        db.setEnabled(reminder.id, !reminder.enabled)
                        onChanged()
                    },
                    shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (reminder.enabled) "Disable Reminder" else "Enable Reminder")
            }
        }
    }

    if (showPay) {
        PaymentDialog(
                name = reminder.name,
                onDismiss = { showPay = false },
                onSave = { paymentDate, billingMonth, amount, notes ->
                    val inserted = db.addPayment(
                            reminderId = reminder.id,
                            paymentDate = paymentDate,
                            billingMonth = billingMonth,
                            amount = amount,
                            notes = notes
                    )

                    if (inserted) {
                        showPay = false
                        onChanged()
                    } else {
                        duplicateBillingMonth = billingMonth
                        showDuplicatePayment = true
                    }
                }
        )
    }

    if (showDuplicatePayment) {
        AlertDialog(
                onDismissRequest = {
                    showDuplicatePayment = false
                },
                title = {
                    Text("Duplicate Payment Cannot Be Created")
                },
                text = {
                    Text(
                            "A payment for ${reminder.name} for billing month " +
                                    "$duplicateBillingMonth already exists."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showDuplicatePayment = false
                            }
                    ) {
                        Text("OK")
                    }
                }
        )
    }

    if (showEdit) {
        EditReminderDialog(
                reminder = reminder,
                onDismiss = { showEdit = false },
                onSave = { name, description, amount, category, day, endDay, time ->
                    db.updateReminder(
                            id = reminder.id,
                            name = name,
                            description = description,
                            amount = amount,
                            category = category,
                            day = day,
                            endDay = endDay,
                            time = time
                    )
                    showEdit = false
                    onChanged()
                }
        )
    }

    if (showDelete) {
        AlertDialog(
                onDismissRequest = {
                    showDelete = false
                },
                title = {
                    Text("Delete ${reminder.name}?")
                },
                text = {
                    Text("This will permanently delete this reminder.")
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                db.deleteReminder(reminder.id)
                                showDelete = false
                                onChanged()
                            }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showDelete = false
                            }
                    ) {
                        Text("Cancel")
                    }
                }
        )
    }
}


internal fun dueDateForMonth(
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

internal fun daysUntilDue(
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

        val nextMonth =
                today.plusMonths(1)

        target =
                dueDateForMonth(
                        reminder,
                        nextMonth.year,
                        nextMonth.monthValue
                )
    }

    return ChronoUnit.DAYS.between(
            today,
            target
    )
}

internal fun isOverdue(
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

private fun ordinal(n: Int): String =
        when {

            n % 100 in 11..13 ->
                "th"

            n % 10 == 1 ->
                "st"

            n % 10 == 2 ->
                "nd"

            n % 10 == 3 ->
                "rd"

            else ->
                "th"
        }


@Composable
private fun PaymentDialog(
        name: String,
        onDismiss: () -> Unit,
        onSave: (
                LocalDate,
                String,
                Double?,
                String
        ) -> Unit
) {
    var paymentDate by remember {
        mutableStateOf(LocalDate.now().toString())
    }

    var billingMonth by remember {
        mutableStateOf(LocalDate.now().toString().substring(0, 7))
    }

    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                            text = "Record Payment",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ForestGreen
                    )
                    Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MutedText
                    )
                }
            },
            text = {
                Column(
                        modifier = Modifier.widthIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                            text = "Enter the payment details below. The billing month identifies which bill cycle this payment covers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                    )

                    OutlinedTextField(
                            value = paymentDate,
                            onValueChange = {
                                paymentDate = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Payment date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            supportingText = { Text("The day you made the payment") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = billingMonth,
                            onValueChange = {
                                billingMonth = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Billing month") },
                            placeholder = { Text("YYYY-MM") },
                            supportingText = {
                                Text("Example: September 2026 → 2026-09")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Amount paid") },
                            placeholder = { Text("e.g. 50.00") },
                            prefix = { Text("$") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = notes,
                            onValueChange = {
                                notes = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Notes (optional)") },
                            placeholder = {
                                Text("e.g. September electricity cycle")
                            },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                    )

                    errorMessage?.let { message ->
                        Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                    text = message,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            try {
                                val parsedDate = LocalDate.parse(paymentDate)

                                val validBillingMonth =
                                        billingMonth.matches(Regex("\\d{4}-\\d{2}"))

                                if (!validBillingMonth) {
                                    errorMessage = "Billing month must use YYYY-MM format."
                                } else {
                                    val year = billingMonth.substring(0, 4).toInt()
                                    val month = billingMonth.substring(5, 7).toInt()

                                    if (month !in 1..12) {
                                        errorMessage = "Enter a valid billing month."
                                    } else if (year < 1) {
                                        errorMessage = "Enter a valid billing year."
                                    } else if (
                                            amount.isNotBlank() &&
                                            (amount.toDoubleOrNull() == null ||
                                                    amount.toDoubleOrNull()!! < 0)
                                    ) {
                                        errorMessage = "Enter a valid non-negative amount."
                                    } else {
                                        onSave(
                                                parsedDate,
                                                billingMonth,
                                                amount.toDoubleOrNull(),
                                                notes.trim()
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Payment date must use YYYY-MM-DD format."
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
    )
}

@Composable
private fun AddReminderDialog(
        onDismiss: () -> Unit,
        onSave: (
                String,
                String,
                Double?,
                String,
                Int,
                Int?,
                String
        ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var day by remember { mutableStateOf("1") }
    var endDay by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("09:00") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFFFFFEFB),
            title = {
                Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                            text = "Add Bill",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ForestGreen
                    )
                    Text(
                            text = "Set up a bill or recurring payment reminder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                    )
                }
            },
            text = {
                Column(
                        modifier = Modifier
                                .widthIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorMessage = null
                            },
                            label = { Text("Bill name") },
                            placeholder = { Text("e.g. Electricity") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            placeholder = { Text("Add details about this bill") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                errorMessage = null
                            },
                            label = { Text("Amount (optional)") },
                            placeholder = { Text("e.g. 75.00") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            placeholder = { Text("e.g. Utilities, Housing") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider(color = CardBorder)

                    Text(
                            text = "REMINDER SCHEDULE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedText
                    )

                    OutlinedTextField(
                            value = day,
                            onValueChange = {
                                day = it
                                errorMessage = null
                            },
                            label = { Text("Due day (1–31)") },
                            placeholder = { Text("1") },
                            singleLine = true,
                            isError = day.toIntOrNull()?.let { it !in 1..31 } ?: true,
                            supportingText = {
                                if (day.toIntOrNull()?.let { it !in 1..31 } != false) {
                                    Text(
                                            text = "Enter a day between 1 and 31.",
                                            color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = endDay,
                            onValueChange = {
                                endDay = it
                                errorMessage = null
                            },
                            label = { Text("End day (optional)") },
                            supportingText = {
                                Text("For a due window, such as rent due from the 1st–5th.")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                            value = time,
                            onValueChange = {
                                time = it
                                errorMessage = null
                            },
                            label = { Text("Reminder time (HH:mm)") },
                            placeholder = { Text("09:00") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage != null) {
                        Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val d = day.toIntOrNull()
                            val e = if (endDay.isBlank()) null else endDay.toIntOrNull()
                            val a = if (amount.isBlank()) null else amount.toDoubleOrNull()
                            val validTime = runCatching {
                                java.time.LocalTime.parse(time)
                            }.isSuccess

                            errorMessage = when {
                                trimmedName.isBlank() ->
                                    "Please enter a bill name."
                                d == null || d !in 1..31 ->
                                    "Due day must be between 1 and 31."
                                endDay.isNotBlank() && (e == null || e !in 1..31) ->
                                    "End day must be between 1 and 31."
                                e != null && d != null && e < d ->
                                    "End day must be on or after the due day."
                                amount.isNotBlank() && (a == null || !a.isFinite() || a < 0) ->
                                    "Enter a valid non-negative amount."
                                !validTime ->
                                    "Enter a valid time, such as 09:00."
                                else -> null
                            }

                            if (errorMessage == null) {
                                onSave(
                                        trimmedName,
                                        description.trim(),
                                        a,
                                        category.trim().ifBlank { "Other" },
                                        d!!,
                                        e,
                                        time.trim()
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                                containerColor = ForestGreen
                        )
                ) {
                    Text("Add Bill")
                }
            },
            dismissButton = {
                OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text("Cancel", color = MutedText)
                }
            }
    )
}

@Composable
private fun EditReminderDialog(
        reminder: Reminder,
        onDismiss: () -> Unit,
        onSave: (
                String,
                String,
                Double?,
                String,
                Int,
                Int?,
                String
        ) -> Unit
) {

    var name by remember { mutableStateOf(reminder.name) }
    var description by remember { mutableStateOf(reminder.description) }

    var amount by remember {
        mutableStateOf(
                reminder.amount?.toString() ?: ""
        )
    }

    var category by remember {
        mutableStateOf(reminder.category)
    }

    var day by remember {
        mutableStateOf(
                reminder.reminderDay.toString()
        )
    }

    var endDay by remember {
        mutableStateOf(
                reminder.reminderEndDay?.toString() ?: ""
        )
    }

    var time by remember {
        mutableStateOf(reminder.reminderTime)
    }

    AlertDialog(

            onDismissRequest = onDismiss,

            title = {
                Text("Edit Bill")
            },

            text = {

                Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                            },
                            label = {
                                Text("Bill Name")
                            }
                    )

                    OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                            },
                            label = {
                                Text("Description")
                            }
                    )

                    OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                            },
                            label = {
                                Text("Amount")
                            }
                    )

                    OutlinedTextField(
                            value = category,
                            onValueChange = {
                                category = it
                            },
                            label = {
                                Text("Category")
                            }
                    )

                    OutlinedTextField(
                            value = day,
                            onValueChange = {
                                day = it
                            },
                            label = {
                                Text("Due Day (1–31)")
                            }
                    )

                    OutlinedTextField(
                            value = endDay,
                            onValueChange = {
                                endDay = it
                            },
                            label = {
                                Text("End Day (optional)")
                            }
                    )

                    OutlinedTextField(
                            value = time,
                            onValueChange = {
                                time = it
                            },
                            label = {
                                Text("Reminder Time (HH:mm)")
                            }
                    )
                }
            },

            confirmButton = {

                Button(
                        onClick = {

                            val d = day.toIntOrNull()
                            val e = endDay.toIntOrNull()
                            val a = amount.toDoubleOrNull()

                            if (
                                    !name.isBlank() &&
                                    d != null &&
                                    d in 1..31
                            ) {

                                onSave(
                                        name,
                                        description,
                                        a,
                                        category,
                                        d,
                                        e,
                                        time
                                )
                            }
                        }
                ) {
                    Text("Save")
                }
            },

            dismissButton = {

                OutlinedButton(
                        onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
    )

}