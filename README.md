# Bill & Rent Reminder — Kotlin Desktop + SQLite

A simple recurring bill/rent reminder desktop application.

## Defaults

- Electricity: every month on the 22nd at 09:00
- Rent: payment window from the 1st through the 5th at 09:00

The app starts with these reminders automatically on a new installation.

## Features

- Kotlin/JVM desktop application
- Compose for Desktop UI
- SQLite local database
- Monthly recurring reminders
- Electricity and rent defaults
- Add custom monthly reminders
- Enable/disable reminders
- Mark payments as paid
- Payment history
- Desktop system-tray notification when supported

## Requirements

- JDK 17+
- Internet access on first Gradle build to download dependencies
- Gradle wrapper can be generated with `gradle wrapper`

## Run

From the project root:

```bash
gradle wrapper
./gradlew :app:run
```

On Windows:

```bat
gradlew.bat :app:run
```

## Database location

The SQLite database is stored at:

`~/.bill-rent-reminder/reminders.db`

On Windows this resolves under the user's home directory; on macOS/Linux it is also under the user's home directory.

## Important scheduling behavior

The scheduler checks once per minute. A reminder fires when the current date is inside its configured day/window and the configured time matches.

For Rent, the default window is the 1st–5th. The first matching reminder time each day will produce a notification. Marking the payment as paid records it for the current month.

For production use, consider adding OS-specific startup registration, a more robust notification library, encrypted backup/export, and a migration/versioning system.
