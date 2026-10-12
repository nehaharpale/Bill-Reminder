# Bill Reminder

A native desktop application for tracking recurring bills, recording payments, and receiving timely reminders. Built with Kotlin, Jetpack Compose Desktop, and SQLite.

**Repository:** [github.com/u1/Bill-Reminder](https://github.com/u1/Bill-Reminder)

## Overview

Bill & Rent Reminder helps users manage recurring expenses such as rent, electricity, internet, and credit card payments. It provides a dashboard for monitoring payment status, a payment history for tracking completed bills, and scheduled notifications to help users stay on top of due dates.

The project focuses on practical desktop application engineering: persistent data storage, billing-cycle accuracy, duplicate-payment prevention, reliable reminders, and safe database recovery.

## Features

- **Bill management:** Create, edit, and delete recurring bills with configurable due dates, payment windows, amounts, and categories.
- **Payment tracking:** Record payment dates, billing months, amounts, and notes.
- **Billing-cycle accuracy:** Associate payments with the intended billing month instead of assuming the payment date determines the billing period. This supports bills such as electricity, where a service cycle can span two calendar months.
- **Duplicate-payment prevention:** Enforce one payment per bill per billing month at the database level.
- **Dashboard and payment history:** Review upcoming, overdue, and paid bills, with recent payment activity.
- **Scheduled desktop notifications:** Check reminder schedules periodically and account for app startup after the configured reminder time.
- **Persistent notification history:** Track notifications across application restarts to prevent repeated alerts on the same day.
- **Database backup and restore:** Create backups, validate database integrity before restoration, and create a safety backup before replacing the active database.
- **Input validation:** Validate required fields and scheduling values before saving changes.

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Application logic and data handling |
| Jetpack Compose Desktop | Desktop user interface |
| Material 3 | UI components and styling |
| SQLite | Local persistent storage |
| JDBC / SQLite JDBC | Database connectivity |
| Gradle | Build and dependency management |
| JUnit-based tests | Regression testing for application logic |

## Engineering Highlights

### 1. Modeling billing periods separately from payment dates

A payment date and a billing period are not always the same. For example, an electricity bill covering September 26 through October 26 may be paid in October but belong to a separately selected billing cycle.

The application stores a `billing_month` independently from `payment_date`, allowing users to record the correct period and review historical payments accurately.

### 2. Preventing duplicate payments

The database enforces a unique constraint on the combination of reminder ID and billing month. This provides a persistent safeguard against duplicate records, rather than relying exclusively on UI validation.

### 3. Handling missed reminder checks

The reminder scheduler accounts for the possibility that the application starts after a configured reminder time or misses a scheduled check. It can send a reminder when the bill remains within its configured due window and a notification has not already been sent that day.

Notification history is persisted so the application can maintain this behavior across restarts.

### 4. Designing safer backup and restore operations

Backup and restore workflows include database-integrity validation and a safety backup before restoration. These safeguards help reduce the risk of replacing valid user data with a damaged or invalid database.

## Architecture

The application separates its main responsibilities into distinct areas:

- **Application entry point:** Initializes the desktop application.
- **UI layer:** Renders the dashboard, bill forms, payment dialogs, and backup/restore interactions.
- **Data layer:** Manages SQLite connectivity, schema migrations, bill records, payment history, and persistence.
- **Reminder scheduler:** Evaluates reminder schedules and coordinates desktop notifications.
- **Test layer:** Covers important date-boundary and application-logic behavior.

## Getting Started

### Prerequisites

- JDK 17
- Git
- A terminal
- macOS for the development environment described below

The project uses the Gradle Wrapper, so a separate Gradle installation is not required.

### Clone the repository

```bash
git clone https://github.com/u1/Bill-Reminder.git
cd Bill-Reminder
```

### Build and test

```bash
./gradlew clean test
```

To compile the application:

```bash
./gradlew :app:compileKotlin
```

### Run the application

```bash
./gradlew :app:run
```

On an Apple Silicon Mac, if your Java installation requires an explicit ARM architecture, use:

```bash
arch -arm64 ./gradlew clean test
arch -arm64 ./gradlew :app:run
```

## Data Storage and Recovery

The application stores its SQLite database locally at:

```text
~/.bill-rent-reminder/reminders.db
```

The database contains bill definitions, payment records, and notification history. Back up important data regularly, and verify backups before relying on them for recovery.

Do not commit personal database files or backups to the repository.

## Project Structure

```text
Bill-Reminder/
├── app/
│   ├── src/
│   │   ├── main/kotlin/com/example/billreminder/
│   │   │   ├── Main.kt
│   │   │   ├── ReminderScheduler.kt
│   │   │   ├── data/
│   │   │   │   ├── Database.kt
│   │   │   │   └── Models.kt
│   │   │   └── ui/
│   │   │       └── App.kt
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── .gitignore
└── README.md
```

## Testing

Run the automated test suite with:

```bash
./gradlew clean test
```

Important validation areas include date-boundary logic and payment-related application behavior. Backup and restore should also be tested with valid backups, invalid database files, cancellation, and restoration of existing data.

## Future Improvements

Potential areas for continued development include:

- Cross-platform packaging and distribution.
- Expanded automated tests for backup and restore workflows.
- Additional reporting and payment-history filters.
- Continuous integration for automated builds and regression tests.

## About This Project

Bill Reminder is a personal software project demonstrating desktop application development, relational database design, date-sensitive business logic, scheduled notifications, and data recovery practices.

