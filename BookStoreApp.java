import java.sql.*;
import java.util.Scanner;

public class BookStoreApp {

    public static void main(String[] args) {
        String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/comp421";
        String your_userid = System.getenv("SOCSUSER");
        String your_password = System.getenv("SOCSPASSWD");

        if (your_userid == null || your_password == null) {
            System.err
                    .println("Error: Database credentials not found in environment variables (SOCSUSER / SOCSPASSWD).");
            System.exit(1);
        }

        Connection con = null;
        Scanner scanner = new Scanner(System.in);

        try {
            // Register the driver
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
            con = DriverManager.getConnection(url, your_userid, your_password);

            // Setting auto-commit to true for general use, but can be controlled for
            // transactions
            con.setAutoCommit(true);

            boolean keepRunning = true;

            while (keepRunning) {
                System.out.println("\n=========================================");
                System.out.println("      McGILL BOOKSTORE ADMIN MENU");
                System.out.println("=========================================");
                System.out.println("1. Find High-Spending Users");
                System.out.println("2. Search Book & Read Reviews (Dynamic Sub-Menu)");
                System.out.println("3. Reward Popular Books with Extra Stock (Multiple Statements)");
                System.out.println("4. Find Top Rated Publishers");
                System.out.println("5. Auto-Level Low Stock (Run Stored Procedure)");
                System.out.println("6. Quit");
                System.out.print("Please Enter Your Option: ");

                String input = scanner.nextLine();
                int choice = 0;

                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number between 1 and 6.");
                    continue;
                }

                switch (choice) {
                    case 1:
                        findHighSpenders(con, scanner);
                        break;
                    case 2:
                        searchBookSubMenu(con, scanner);
                        break;
                    case 3:
                        addStockToPopularBooks(con, scanner);
                        break;
                    case 4:
                        findTopPublishers(con, scanner);
                        break;
                    case 5:
                        runRestockProcedure(con, scanner);
                        break;
                    case 6:
                        System.out.println("Closing connections and shutting down... Goodbye!");
                        keepRunning = false;
                        break;
                    default:
                        System.out.println("Invalid option. Please choose 1-6.");
                }
            }
        } catch (SQLException e) {
            System.out.println("\n--- FATAL DATABASE ERROR ---");
            System.out.println("SQL Code: " + e.getErrorCode() + " | SQL State: " + e.getSQLState());
            System.out.println("Message: " + e.getMessage());
        } finally {
            // Gracefully close resources [cite: 482]
            try {
                if (scanner != null)
                    scanner.close();
                if (con != null && !con.isClosed())
                    con.close();
            } catch (SQLException e) {
                System.out.println("Error while closing connection: " + e.getMessage());
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // OPTION 1: Find high spenders (Basic Query)
    // ----------------------------------------------------------------------------------
    private static void findHighSpenders(Connection con, Scanner scanner) {
        System.out.print("Enter the minimum dollar amount spent: $");
        double amount = Double.parseDouble(scanner.nextLine());

        String query = "SELECT U.name, SUM(PC.price * PC.qty) as total_spent " +
                "FROM Users U " +
                "JOIN Purchase P ON U.user_id = P.user_id " +
                "JOIN Contains PC ON P.purchase_id = PC.purchase_id " +
                "GROUP BY U.user_id, U.name " +
                "HAVING SUM(PC.price * PC.qty) > ? " +
                "ORDER BY total_spent DESC";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setDouble(1, amount);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- HIGH SPENDERS ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("Name: %-20s | Total Spent: $%.2f%n", rs.getString("name"),
                        rs.getDouble("total_spent"));
            }
            if (!found)
                System.out.println("No users found who spent more than $" + amount);
            rs.close();
        } catch (SQLException e) {
            printSQLError(e);
        }
    }

    // ----------------------------------------------------------------------------------
    // OPTION 2: Search & Sub-Menu (Dynamic Menu Requirement)
    // ----------------------------------------------------------------------------------
    private static void searchBookSubMenu(Connection con, Scanner scanner) {
        System.out.print("Enter a keyword to search for a book: ");
        String keyword = scanner.nextLine();

        String query = "SELECT isbn, name FROM Book WHERE LOWER(name) LIKE LOWER(?)";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            // Store results temporarily to build the sub-menu
            java.util.List<String> isbns = new java.util.ArrayList<>();
            java.util.List<String> names = new java.util.ArrayList<>();

            int count = 1;
            System.out.println("\n--- SEARCH RESULTS ---");
            while (rs.next()) {
                isbns.add(rs.getString("isbn"));
                names.add(rs.getString("name"));
                System.out.println(count + ". " + rs.getString("name") + " (ISBN: " + rs.getString("isbn") + ")");
                count++;
            }
            rs.close();

            if (isbns.isEmpty()) {
                System.out.println("No books found matching '" + keyword + "'.");
                return;
            }

            // The Dynamic Sub-Menu Prompt
            System.out.println(count + ". Return to Main Menu");
            System.out.print("Select a number to read its reviews: ");
            int subChoice = Integer.parseInt(scanner.nextLine());

            if (subChoice > 0 && subChoice <= isbns.size()) {
                String selectedIsbn = isbns.get(subChoice - 1);
                printBookReviews(con, selectedIsbn, names.get(subChoice - 1));
            }

        } catch (SQLException e) {
            printSQLError(e);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Returning to main menu.");
        }
    }

    private static void printBookReviews(Connection con, String isbn, String bookName) {
        String query = "SELECT rating, review_comment FROM Reviews WHERE isbn = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- REVIEWS FOR: " + bookName + " ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("Rating: " + rs.getInt("rating") + "/5 | " + rs.getString("review_comment"));
            }
            if (!found)
                System.out.println("No reviews found for this book.");
            rs.close();
        } catch (SQLException e) {
            printSQLError(e);
        }
    }

    // ----------------------------------------------------------------------------------
    // OPTION 3: Add Stock to Popular Books (Multiple SQL Statements Requirement)
    // ----------------------------------------------------------------------------------
    private static void addStockToPopularBooks(Connection con, Scanner scanner) {
        System.out.print("Enter the minimum number of total units sold to qualify as 'popular': ");
        int minPurchases = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter the quantity of books to add to their stock: ");
        int addQty = Integer.parseInt(scanner.nextLine());

        // Statement 1: Find the popular books
        String findQuery = "SELECT B.isbn, B.name, SUM(C.qty) as total_sold " +
                "FROM Book B JOIN Contains C ON B.isbn = C.isbn " +
                "GROUP BY B.isbn, B.name HAVING SUM(C.qty) >= ?";

        // Statement 2: Update their stock
        String updateQuery = "UPDATE Stocks SET qty = qty + ? WHERE isbn = ?";

        try (PreparedStatement findStmt = con.prepareStatement(findQuery);
                PreparedStatement updateStmt = con.prepareStatement(updateQuery)) {

            findStmt.setInt(1, minPurchases);
            ResultSet rs = findStmt.executeQuery();

            System.out.println("\n--- REWARDING POPULAR BOOKS ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                String isbn = rs.getString("isbn");
                String name = rs.getString("name");

                // Execute the second SQL statement for each book found
                updateStmt.setInt(1, addQty);
                updateStmt.setString(2, isbn);
                int rowsAffected = updateStmt.executeUpdate();

                System.out.println(
                        "Added " + addQty + " stock to '" + name + "' across " + rowsAffected + " warehouse(s).");
            }
            if (!found)
                System.out.println("No books have sold " + minPurchases + " or more units.");
            rs.close();

        } catch (SQLException e) {
            printSQLError(e);
        }
    }

    // ----------------------------------------------------------------------------------
    // OPTION 4: Find Top Rated Publishers (Aggregation Query)
    // ----------------------------------------------------------------------------------
    private static void findTopPublishers(Connection con, Scanner scanner) {
        System.out.print("Enter the minimum average review rating (e.g., 4.0): ");
        double minRating = Double.parseDouble(scanner.nextLine());

        // Cast to DECIMAL ensures Db2 does floating point division properly
        String query = "SELECT P.name, AVG(CAST(R.rating AS DECIMAL(5,2))) as avg_rating " +
                "FROM Publisher P " +
                "JOIN Book B ON P.publisher_id = B.publisher_id " +
                "JOIN Reviews R ON B.isbn = R.isbn " +
                "GROUP BY P.name " +
                "HAVING AVG(CAST(R.rating AS DECIMAL(5,2))) >= ? " +
                "ORDER BY avg_rating DESC";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setDouble(1, minRating);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- TOP PUBLISHERS ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("Publisher: %-25s | Avg Rating: %.2f%n", rs.getString("name"),
                        rs.getDouble("avg_rating"));
            }
            if (!found)
                System.out.println("No publishers meet that rating threshold.");
            rs.close();
        } catch (SQLException e) {
            printSQLError(e);
        }
    }

    // ----------------------------------------------------------------------------------
    // OPTION 5: Call Stored Procedure
    // ----------------------------------------------------------------------------------
    private static void runRestockProcedure(Connection con, Scanner scanner) {
        System.out.print("Enter the global stock threshold to trigger auto-restock: ");
        int threshold = Integer.parseInt(scanner.nextLine());

        try (CallableStatement cstmt = con.prepareCall("{CALL LevelRestock(?, ?)}")) {
            // Set the IN parameter (the threshold)
            cstmt.setInt(1, threshold);
            // Register the OUT parameter (the counter)
            cstmt.registerOutParameter(2, java.sql.Types.INTEGER);

            System.out.println("\nExecuting Supply Chain Leveling Procedure...");
            cstmt.execute();

            // Retrieve the output
            int booksRestocked = cstmt.getInt(2);
            System.out.println("SUCCESS! " + booksRestocked + " unique book titles were re-leveled across warehouses.");

        } catch (SQLException e) {
            System.out.println("Failed to execute stored procedure. Did you compile it in Db2 first?");
            printSQLError(e);
        }
    }

    // Helper method to keep error printing clean
    private static void printSQLError(SQLException e) {
        System.out.println("\n[SQL ERROR] Code: " + e.getErrorCode() + " | State: " + e.getSQLState());
        System.out.println(e.getMessage());
    }
}