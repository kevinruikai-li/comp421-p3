# COMP-421 Project 3: BookStore Admin Application
**Group 41** This repository contains the Java JDBC application for Project 3. It connects to the DB2 database hosted on the `winter2026-comp421.cs.mcgill.ca` server to manage a bookstore's inventory, sales, and user reviews.

## Prerequisites

1. **Environment Variables:** For security, this application does not hardcode database credentials. You must set your SOCS credentials in your terminal environment before running the app:
   ```bash
   export SOCSUSER="your_cs_username"
   export SOCSPASSWD="your_db_password"
   ```

2. **Database Setup:** Ensure the database is populated using our `createtbl.sql` and `loaddata.sql` scripts.

3. **Stored Procedure (Required for Menu Option 5):** Option 5 executes our custom `LevelRestock` stored procedure. You must compile it in DB2 before running the Java application:
   ```bash
   db2 -td@ -f procedure.sql
   ```

## Compilation & Execution

Because the DB2 JDBC driver is already globally configured in the McGill server's CLASSPATH, compilation and execution are straightforward.

**To compile the application:**
```bash
javac BookStoreApp.java
```

**To run the application:**
```bash
java BookStoreApp
```

## Menu Features & Rubric Mapping

Our application features a looping, console-based UI that gracefully handles `SQLExceptions` and provides the following functionalities:

1. **Find High-Spending Users:** A multi-table `JOIN` query utilizing `GROUP BY` and `HAVING` clauses.
2. [cite_start]**Search Book & Read Reviews (Dynamic Sub-Menu):** Fulfills the project requirement to generate a sub-menu out of a database query[cite: 480]. It searches for books via keyword, generates a dynamic numbered list, and allows the user to select a specific book to read its reviews.
3. [cite_start]**Reward Popular Books (Multiple SQL Statements):** Fulfills the requirement to execute more than one SQL statement in a single task[cite: 479]. It runs a `SELECT` query to find top-selling books, then executes a looped `UPDATE` statement to add bonus inventory to those specific books.
4. **Find Top Rated Publishers:** An advanced aggregation query calculating average book ratings per publisher.
5. **Auto-Level Low Stock (Stored Procedure):** Uses a `CallableStatement` with both `IN` and `OUT` parameters to execute the `LevelRestock` stored procedure created in Step 2 of the project.
6. **Quit:** Gracefully closes all database connections and scanners before terminating.
