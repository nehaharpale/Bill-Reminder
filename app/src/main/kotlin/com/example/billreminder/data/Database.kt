package com.example.billreminder.data

import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.format.DateTimeFormatter



class Database {

    private val dbDir =
            Paths.get(System.getProperty("user.home"), ".bill-rent-reminder")

    private val dbFile = dbDir.resolve("reminders.db")

    private lateinit var connection: Connection


    private fun openConnection(): Connection {
        val conn = DriverManager.getConnection(
                "jdbc:sqlite:${dbFile.toAbsolutePath()}"
        )

        conn.createStatement().use { st ->
            st.executeUpdate("PRAGMA foreign_keys = ON")
        }

        return conn
    }


    init {

        Files.createDirectories(dbDir)

        Class.forName("org.sqlite.JDBC")

        connection = openConnection()

        connection.createStatement().use { st ->

            st.executeUpdate("PRAGMA foreign_keys = ON")

            st.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    amount REAL,
                    category TEXT NOT NULL DEFAULT 'Other',
                    reminder_day INTEGER NOT NULL,
                    reminder_end_day INTEGER,
                    reminder_time TEXT NOT NULL DEFAULT '09:00',
                    frequency TEXT NOT NULL DEFAULT 'MONTHLY',
                    enabled INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL
                )
                """.trimIndent()
            )

            st.executeUpdate(
                    """
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reminder_id INTEGER NOT NULL,
                    payment_date TEXT NOT NULL,
                    billing_month TEXT,
                    amount REAL,
                    notes TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(reminder_id)
                        REFERENCES reminders(id)
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )
            st.executeUpdate(
                    """
    CREATE TABLE IF NOT EXISTS reminder_notification_history (
        reminder_id INTEGER PRIMARY KEY,
        last_notified_date TEXT NOT NULL,
        FOREIGN KEY(reminder_id)
            REFERENCES reminders(id)
            ON DELETE CASCADE
    )
    """.trimIndent()
            )
        }

        migrateDatabase()

        seedDefaults()
    }

    /**
     * Handles database changes from earlier versions of the app.
     *
     * Existing payment records are preserved.
     */
    private fun migrateDatabase() {

        /*
         * Check reminder columns.
         */
        val reminderColumns = mutableSetOf<String>()

        connection.createStatement().use { st ->

            st.executeQuery(
                    "PRAGMA table_info(reminders)"
            ).use { rs ->

                while (rs.next()) {
                    reminderColumns.add(
                            rs.getString("name")
                    )
                }
            }
        }

        connection.createStatement().use { st ->

            if ("amount" !in reminderColumns) {

                st.executeUpdate(
                        "ALTER TABLE reminders ADD COLUMN amount REAL"
                )
            }

            if ("category" !in reminderColumns) {

                st.executeUpdate(
                        """
                    ALTER TABLE reminders
                    ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'
                    """.trimIndent()
                )
            }
        }

        /*
         * Check payment columns.
         */
        val paymentColumns = mutableSetOf<String>()

        connection.createStatement().use { st ->

            st.executeQuery(
                    "PRAGMA table_info(payments)"
            ).use { rs ->

                while (rs.next()) {
                    paymentColumns.add(
                            rs.getString("name")
                    )
                }
            }
        }

        /*
         * Add billing_month to databases created
         * by the previous version of the app.
         */
        if ("billing_month" !in paymentColumns) {

            connection.createStatement().use { st ->

                st.executeUpdate(
                        """
                    ALTER TABLE payments
                    ADD COLUMN billing_month TEXT
                    """.trimIndent()
                )
            }
        }

        /*
         * Populate billing_month for existing payments.
         *
         * Example:
         *
         * payment_date = 2026-10-05
         * billing_month = 2026-10
         */
        connection.createStatement().use { st ->

            st.executeUpdate(
                    """
                UPDATE payments
                SET billing_month = substr(payment_date, 1, 7)
                WHERE billing_month IS NULL
                   OR billing_month = ''
                """.trimIndent()
            )
        }

        /*
         * Remove the old unique index that was based
         * on payment_date.
         */
        connection.createStatement().use { st ->

            st.executeUpdate(
                    """
                DROP INDEX IF EXISTS idx_payments_reminder_month
                """.trimIndent()
            )
        }

        /*
         * New database-level rule:
         *
         * One bill can have only one payment
         * for a particular billing month.
         */
        connection.createStatement().use { st ->

            st.executeUpdate(
                    """
                CREATE UNIQUE INDEX IF NOT EXISTS
                idx_payments_reminder_billing_month
                ON payments(reminder_id, billing_month)
                """.trimIndent()
            )
        }
    }

    private fun seedDefaults() {

        if (countReminders() == 0L) {

            addReminder(
                    name = "Electricity",
                    description = "Monthly electricity bill",
                    amount = null,
                    category = "Utilities",
                    day = 22,
                    endDay = null,
                    time = "09:00"
            )

            addReminder(
                    name = "Rent",
                    description = "Monthly rent payment window",
                    amount = null,
                    category = "Housing",
                    day = 1,
                    endDay = 5,
                    time = "09:00"
            )
        }
    }

    private fun countReminders(): Long =

            connection.createStatement().use { st ->

                st.executeQuery(
                        "SELECT COUNT(*) FROM reminders"
                ).use { rs ->

                    rs.next()

                    rs.getLong(1)
                }
            }

    fun getReminders(): List<Reminder> {

        val result = mutableListOf<Reminder>()

        connection.prepareStatement(
                """
            SELECT
                id,
                name,
                description,
                amount,
                category,
                reminder_day,
                reminder_end_day,
                reminder_time,
                enabled
            FROM reminders
            ORDER BY reminder_day
            """.trimIndent()
        ).use { ps ->

            ps.executeQuery().use { rs ->

                while (rs.next()) {

                    result += Reminder(

                            id = rs.getLong("id"),

                            name = rs.getString("name"),

                            description = rs.getString("description"),

                            amount =
                            rs.getObject("amount")?.let {
                                rs.getDouble("amount")
                            },

                            category =
                            rs.getString("category"),

                            reminderDay =
                            rs.getInt("reminder_day"),

                            reminderEndDay =
                            rs.getObject("reminder_end_day")?.let {
                                rs.getInt("reminder_end_day")
                            },

                            reminderTime =
                            rs.getString("reminder_time"),

                            enabled =
                            rs.getInt("enabled") == 1
                    )
                }
            }
        }

        return result
    }

    fun addReminder(
            name: String,
            description: String,
            amount: Double?,
            category: String,
            day: Int,
            endDay: Int?,
            time: String
    ) {

        connection.prepareStatement(
                """
            INSERT INTO reminders(
                name,
                description,
                amount,
                category,
                reminder_day,
                reminder_end_day,
                reminder_time,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { ps ->

            ps.setString(1, name)

            ps.setString(2, description)

            if (amount == null) {
                ps.setNull(3, java.sql.Types.REAL)
            } else {
                ps.setDouble(3, amount)
            }

            ps.setString(4, category)

            ps.setInt(5, day)

            if (endDay == null) {
                ps.setNull(
                        6,
                        java.sql.Types.INTEGER
                )
            } else {
                ps.setInt(6, endDay)
            }

            ps.setString(7, time)

            ps.setString(
                    8,
                    LocalDateTime.now().toString()
            )

            ps.executeUpdate()
        }
    }

    fun updateReminder(
            id: Long,
            name: String,
            description: String,
            amount: Double?,
            category: String,
            day: Int,
            endDay: Int?,
            time: String
    ) {

        connection.prepareStatement(
                """
            UPDATE reminders
            SET
                name = ?,
                description = ?,
                amount = ?,
                category = ?,
                reminder_day = ?,
                reminder_end_day = ?,
                reminder_time = ?
            WHERE id = ?
            """.trimIndent()
        ).use { ps ->

            ps.setString(1, name)

            ps.setString(2, description)

            if (amount == null) {
                ps.setNull(3, java.sql.Types.REAL)
            } else {
                ps.setDouble(3, amount)
            }

            ps.setString(4, category)

            ps.setInt(5, day)

            if (endDay == null) {
                ps.setNull(
                        6,
                        java.sql.Types.INTEGER
                )
            } else {
                ps.setInt(6, endDay)
            }

            ps.setString(7, time)

            ps.setLong(8, id)

            ps.executeUpdate()
        }
    }

    fun deleteReminder(id: Long) {

        connection.prepareStatement(
                "DELETE FROM reminders WHERE id = ?"
        ).use { ps ->

            ps.setLong(1, id)

            ps.executeUpdate()
        }
    }

    fun setEnabled(
            id: Long,
            enabled: Boolean
    ) {

        connection.prepareStatement(
                """
            UPDATE reminders
            SET enabled = ?
            WHERE id = ?
            """.trimIndent()
        ).use { ps ->

            ps.setInt(
                    1,
                    if (enabled) 1 else 0
            )

            ps.setLong(2, id)

            ps.executeUpdate()
        }
    }

    /**
     * Adds a payment for a specific billing month.
     *
     * Returns true when a new payment was inserted.
     * Returns false when that bill already has a
     * payment for the selected billing month.
     */
    fun addPayment(
            reminderId: Long,
            paymentDate: LocalDate,
            billingMonth: String,
            amount: Double?,
            notes: String
    ): Boolean {

        val inserted =
                connection.prepareStatement(
                        """
                INSERT OR IGNORE INTO payments(
                    reminder_id,
                    payment_date,
                    billing_month,
                    amount,
                    notes
                )
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
                ).use { ps ->

                    ps.setLong(
                            1,
                            reminderId
                    )

                    ps.setString(
                            2,
                            paymentDate.toString()
                    )

                    ps.setString(
                            3,
                            billingMonth
                    )

                    if (amount == null) {

                        ps.setNull(
                                4,
                                java.sql.Types.REAL
                        )

                    } else {

                        ps.setDouble(
                                4,
                                amount
                        )
                    }

                    ps.setString(
                            5,
                            notes
                    )

                    ps.executeUpdate()
                }

        return inserted > 0
    }

    fun getPayments(): List<Payment> {

        val result = mutableListOf<Payment>()

        connection.prepareStatement(
                """
            SELECT
                id,
                reminder_id,
                payment_date,
                billing_month,
                amount,
                notes
            FROM payments
            ORDER BY payment_date DESC
            """.trimIndent()
        ).use { ps ->

            ps.executeQuery().use { rs ->

                while (rs.next()) {

                    result += Payment(

                            id = rs.getLong("id"),

                            reminderId =
                            rs.getLong("reminder_id"),

                            paymentDate =
                            rs.getString("payment_date"),

                            billingMonth =
                            rs.getString("billing_month"),

                            amount =
                            rs.getObject("amount")?.let {
                                rs.getDouble("amount")
                            },

                            notes =
                            rs.getString("notes")
                    )
                }
            }
        }

        return result
    }

    /**
     * Checks whether this bill has been paid
     * for the selected billing month.
     */
    fun isPaidForBillingMonth(
            reminderId: Long,
            billingMonth: String
    ): Boolean {

        connection.prepareStatement(
                """
            SELECT COUNT(*)
            FROM payments
            WHERE reminder_id = ?
              AND billing_month = ?
            """.trimIndent()
        ).use { ps ->

            ps.setLong(
                    1,
                    reminderId
            )

            ps.setString(
                    2,
                    billingMonth
            )

            ps.executeQuery().use { rs ->

                rs.next()

                return rs.getInt(1) > 0
            }
        }
    }



    /**
     * Creates and validates a consistent SQLite backup.
     * The destination must not already exist.
     */
    fun backupToFile(destination: Path): Path {
        val target = destination.toAbsolutePath().normalize()
        val activeFile = dbFile.toAbsolutePath().normalize()

        require(target != activeFile) {
            "The backup destination cannot be the active database."
        }

        require(!Files.exists(target)) {
            "A file already exists at the selected destination."
        }

        val parent = target.parent
                ?: throw IllegalArgumentException("Invalid backup location.")

        Files.createDirectories(parent)

        try {
            val escapedPath = target.toString().replace("'", "''")

            connection.createStatement().use { st ->
                st.execute("VACUUM INTO '$escapedPath'")
            }

            validateBackup(target)
            return target
        } catch (e: Exception) {
            try {
                Files.deleteIfExists(target)
            } catch (cleanupError: Exception) {
                e.addSuppressed(cleanupError)
            }

            throw e
        }
    }


    /**
     * Validates the SQLite integrity and essential application tables.
     */
    fun validateBackup(file: Path) {
        val source = file.toAbsolutePath().normalize()

        require(Files.isRegularFile(source) && Files.size(source) > 0) {
            "The selected backup file is missing or empty."
        }

        DriverManager.getConnection("jdbc:sqlite:file:${source}?mode=ro").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA integrity_check").use { rs ->
                    require(rs.next() && rs.getString(1) == "ok") {
                        "The backup failed SQLite integrity validation."
                    }
                }

                val requiredTables = setOf(
                        "reminders",
                        "payments",
                        "reminder_notification_history"
                )

                val foundTables = mutableSetOf<String>()

                st.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type = 'table'"
                ).use { rs ->
                    while (rs.next()) {
                        foundTables.add(rs.getString("name"))
                    }
                }

                val missingTables = requiredTables - foundTables

                require(missingTables.isEmpty()) {
                    "This file is not a compatible Bill Reminder backup. " +
                            "Missing tables: ${missingTables.joinToString()}. " +
                            "Found tables: ${foundTables.joinToString()}."
                }
            }
        }
    }

    /**
     * Creates a safety backup, then replaces the active database.
     */
    fun restoreFromFile(sourceFile: Path): Path {
        val source = sourceFile.toAbsolutePath().normalize()
        val activeFile = dbFile.toAbsolutePath().normalize()

        require(source != activeFile) {
            "Choose a backup file other than the active database."
        }

        validateBackup(source)

        val timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        )

        val safetyBackup = dbDir.resolve(
                "backups/reminders-before-restore-$timestamp.db"
        )

        backupToFile(safetyBackup)

        val temporaryFile = Files.createTempFile(
                dbDir,
                "restore-",
                ".db"
        )

        try {
            Files.copy(
                    source,
                    temporaryFile,
                    StandardCopyOption.REPLACE_EXISTING
            )

            connection.close()

            try {
                Files.move(
                        temporaryFile,
                        dbFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(
                        temporaryFile,
                        dbFile,
                        StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            Files.deleteIfExists(temporaryFile)

            if (!connection.isClosed) {
                connection.close()
            }

            connection = openConnection()
            migrateDatabase()
        }

        return safetyBackup
    }


    fun close() {
        connection.close()
    }
    fun wasReminderNotifiedOnDate(
            reminderId: Long,
            date: LocalDate
    ): Boolean {
        connection.prepareStatement(
                """
        SELECT 1
        FROM reminder_notification_history
        WHERE reminder_id = ?
          AND last_notified_date = ?
        LIMIT 1
        """.trimIndent()
        ).use { ps ->
            ps.setLong(1, reminderId)
            ps.setString(2, date.toString())

            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    fun recordReminderNotification(
            reminderId: Long,
            date: LocalDate
    ) {
        connection.prepareStatement(
                """
        INSERT INTO reminder_notification_history (
            reminder_id,
            last_notified_date
        )
        VALUES (?, ?)
        ON CONFLICT(reminder_id)
        DO UPDATE SET last_notified_date = excluded.last_notified_date
        """.trimIndent()
        ).use { ps ->
            ps.setLong(1, reminderId)
            ps.setString(2, date.toString())
            ps.executeUpdate()
        }
    }
}