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
        try (var stmt = getConnection().createStatement()) {

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                username  TEXT NOT NULL UNIQUE,
                password  TEXT NOT NULL,
                last_seen TEXT DEFAULT ''
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                sender   TEXT NOT NULL,
                receiver TEXT NOT NULL,
                content  TEXT NOT NULL,
                sent_at  TEXT NOT NULL
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS last_seen (
                username TEXT PRIMARY KEY,
                seen_at  TEXT NOT NULL
            )
        """);
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS profiles (
                username TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                avatar_color TEXT DEFAULT '#e94560',
                bio TEXT DEFAULT ''
            )
        """);

            try { stmt.execute("ALTER TABLE users ADD COLUMN last_seen TEXT DEFAULT ''"); }
            catch (SQLException ignored) {}

            System.out.println("База даних сервера ініціалізована!");

        } catch (SQLException e) {
            System.err.println("Помилка БД: " + e.getMessage());
        }
    }

    public static void saveMessage(String sender, String receiver, String content, String time) {
        try {
            var stmt = getConnection().prepareStatement(
                    "INSERT INTO messages (sender, receiver, content, sent_at) VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, content);
            stmt.setString(4, time);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка збереження: " + e.getMessage());
        }
    }

    public static String getHistory(String user1, String user2) {
        StringBuilder sb = new StringBuilder();
        try {
            var stmt = getConnection().prepareStatement("""
                SELECT sender, content, sent_at FROM messages
                WHERE (sender = ? AND receiver = ?)
                   OR (sender = ? AND receiver = ?)
                ORDER BY id ASC
            """);
            stmt.setString(1, user1);
            stmt.setString(2, user2);
            stmt.setString(3, user2);
            stmt.setString(4, user1);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("sender")).append("|")
                        .append(rs.getString("content")).append("|")
                        .append(rs.getString("sent_at")).append(";");
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання історії: " + e.getMessage());
        }
        return sb.toString();
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

    public static String searchMessages(String user, String query) {
        StringBuilder sb = new StringBuilder();
        try {
            var stmt = getConnection().prepareStatement("""
                SELECT sender, receiver, content, sent_at FROM messages
                WHERE (sender = ? OR receiver = ?)
                  AND content LIKE ?
                ORDER BY id ASC
            """);
            stmt.setString(1, user);
            stmt.setString(2, user);
            stmt.setString(3, "%" + query + "%");
            var rs = stmt.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("sender")).append("|")
                        .append(rs.getString("receiver")).append("|")
                        .append(rs.getString("content")).append("|")
                        .append(rs.getString("sent_at")).append(";");
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку: " + e.getMessage());
        }
        return sb.toString();
    }
    public static void deleteMessage(String sender, String receiver, String content, String time) {
        try {
            var stmt = getConnection().prepareStatement(
                    "DELETE FROM messages WHERE sender = ? AND receiver = ? AND content = ? AND sent_at = ?"
            );
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, content);
            stmt.setString(4, time);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка видалення: " + e.getMessage());
        }
    }

    public static void editMessage(String sender, String receiver, String oldContent, String newContent, String time) {
        try {
            var stmt = getConnection().prepareStatement(
                    "UPDATE messages SET content = ? WHERE sender = ? AND receiver = ? AND content = ? AND sent_at = ?"
            );
            stmt.setString(1, newContent);
            stmt.setString(2, sender);
            stmt.setString(3, receiver);
            stmt.setString(4, oldContent);
            stmt.setString(5, time);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка редагування: " + e.getMessage());
        }
    }
    public static void updateLastSeen(String username) {
        try {
            var stmt = getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO last_seen (username, seen_at) VALUES (?, ?)"
            );
            stmt.setString(1, username);
            stmt.setString(2, java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка updateLastSeen: " + e.getMessage());
        }
    }

    public static String getLastSeen(String username) {
        try {
            var stmt = getConnection().prepareStatement(
                    "SELECT last_seen FROM users WHERE username = ?"
            );
            stmt.setString(1, username);
            var rs = stmt.executeQuery();
            if (rs.next() && rs.getString("last_seen") != null) {
                return rs.getString("last_seen");
            }
        } catch (SQLException e) {
            System.err.println("Помилка getLastSeen: " + e.getMessage());
        }
        return "невідомо";
    }

    public static String getUserInfo(String username) {
        try {
            var stmt = getConnection().prepareStatement(
                    "SELECT seen_at FROM last_seen WHERE username = ?"
            );
            stmt.setString(1, username);
            var rs = stmt.executeQuery();
            if (rs.next() && rs.getString("seen_at") != null) {
                return username + "|" + rs.getString("seen_at");
            }
        } catch (SQLException e) {
            System.err.println("Помилка getUserInfo: " + e.getMessage());
        }
        return username + "|невідомо";
    }

    public static void saveProfile(String username, String displayName, String color, String bio) {
        try {
            var stmt = getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO profiles (username, display_name, avatar_color, bio) VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, username);
            stmt.setString(2, displayName);
            stmt.setString(3, color);
            stmt.setString(4, bio);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка saveProfile: " + e.getMessage());
        }
    }

    public static String getProfile(String username) {
        try {
            var stmt = getConnection().prepareStatement(
                    "SELECT display_name, avatar_color, bio FROM profiles WHERE username = ?"
            );
            stmt.setString(1, username);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("display_name") + "|" +
                        rs.getString("avatar_color") + "|" +
                        (rs.getString("bio") != null ? rs.getString("bio") : "");
            }
        } catch (SQLException e) {
            System.err.println("Помилка getProfile: " + e.getMessage());
        }
        return username + "|#e94560|";
    }
}