# Library Management System (Java Swing + JDBC + MySQL)

A desktop application with two roles - **Librarian** and **Member** - for managing a library catalog,
members, borrowing/returning, notifications and inventory reports.

## Features

**Librarian dashboard**
- Books: add / edit / delete (table view)
- Members: create / update / delete
- Transactions: issue and return books, full transaction table
- Notifications: send to one member or everyone; run the due-date check on demand
- Inventory reports: summary statistics + bar chart of borrowing trends

**Member dashboard**
- Search books by title / author / genre (searches are logged)
- Borrow and return books (14-day loan, max 5 active loans)
- Borrowing history and search history
- Profile management (name, email, password)
- Live notification alerts (auto-refresh every 10 s) and notification preference

## Tech stack
Java 17 · Swing · JDBC · MySQL 8 · Maven

## Setup

1. **Create the database**
   ```bash
   mysql -u root -p < sql/schema.sql
   ```
2. **Configure credentials**
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   # edit db.properties with your MySQL user/password
   ```
3. **Build and run**
   ```bash
   mvn clean package
   java -jar target/library-management-system-1.0.0.jar
   ```

### Demo logins
| Role      | Email               | Password  |
|-----------|---------------------|-----------|
| Librarian | admin@library.com   | admin123  |
| Member    | asha@example.com    | member123 |

New members can also self-register from the login screen.

## Project structure
```
src/main/java/com/library
├── Main.java                  entry point, starts background notifier + GUI
├── model/                     Person (abstract) → Librarian, Member; Book, LoanRecord, Notification
├── exception/                 LibraryException → BookNotAvailableException, AuthenticationException
├── dao/                       GenericDAO<T>, Searchable<T>, DBConnection, Book/Member/Transaction/Notification DAOs
├── service/                   LibraryService (synchronized), DueDateNotifier (background thread)
├── ui/                        LoginFrame, LibrarianDashboard, MemberDashboard, BarChartPanel, Ui helpers
└── util/                      PasswordUtil
sql/schema.sql                 tables + sample data
```

## Rubric mapping (where each concept lives)

| Concept | Where |
|---|---|
| Inheritance | `Person` → `Librarian`, `Member`; `BookNotAvailableException`/`AuthenticationException` → `LibraryException` |
| Polymorphism | `Person.getRole()` overridden per subclass; `MemberDAO.authenticate` returns a `Person`; `LoginFrame` routes via `instanceof` |
| Interfaces | `GenericDAO<T>`, `Searchable<T>`, `RowMapper<T>`, `Runnable` (`DueDateNotifier`) |
| Exception handling | Custom checked exceptions, rollback on failure, `Ui.button` shows errors in dialogs |
| Collections & Generics | `List`, `Map`/`LinkedHashMap`/`TreeMap`, streams + `Collectors.groupingBy`; generic `GenericDAO<T>`, `DBConnection.query<T>` |
| Multithreading & synchronization | `DueDateNotifier` (scheduled daemon thread), alert poller in `MemberDashboard` (+`invokeLater`), `synchronized` blocks in `LibraryService` |
| Database operation classes | `BookDAO`, `MemberDAO`, `TransactionDAO`, `NotificationDAO` |
| JDBC | `DBConnection` (DriverManager, PreparedStatement, ResultSet), transactions with commit/rollback and `SELECT ... FOR UPDATE` |

## Security notes
- All SQL uses `PreparedStatement` (no string concatenation of user input; the search column is whitelisted).
- Passwords are stored as SHA-256 hashes for simplicity. For real deployments use bcrypt/argon2 with a salt.
- `db.properties` is git-ignored so credentials are never committed.

## License
MIT
