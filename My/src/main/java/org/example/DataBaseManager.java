package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseManager {

    private static final String URL = "jdbc:sqlite:messenger.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }

    public static void initialize() {
        try (Statement stmt = getConnection().createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL
                )
            """);

            System.out.println("База даних ініціалізована успішно!");

        } catch (SQLException e) {
            System.err.println("Помилка ініціалізації БД: " + e.getMessage());
        }
    }

    public static String getContacts(String username) {
        StringBuilder sb = new StringBuilder();
        try {
            var stmt = getConnection().prepareStatement("""
            SELECT DISTINCT 
                CASE WHEN sender = ? THEN receiver ELSE sender END as contact
            FROM messages
            WHERE sender = ? OR receiver = ?
        """);
            stmt.setString(1, username);
            stmt.setString(2, username);
            stmt.setString(3, username);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("contact")).append(",");
            }
        } catch (SQLException e) {
            System.err.println("Помилка: " + e.getMessage());
        }
        return sb.toString();
    }
}