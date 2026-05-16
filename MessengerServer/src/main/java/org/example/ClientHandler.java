package org.example;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            username = in.readLine();

            long count = Server.getClients().stream()
                    .filter(c -> c != this && username.equals(c.getUsername()))
                    .count();

            if (count > 0) {
                out.println("ERROR:already_connected");
                Server.removeClient(this);
                socket.close();
                return;
            }

            System.out.println("🟢 Підключився: " + username);

            DataBaseManager.updateLastSeen(username);

            String time = LocalTime.now().format(TIME_FORMAT);
            Server.broadcast("[" + time + "] 🟢 " + username + " приєднався до чату", this);
            Server.showSessions();

            out.println(Server.getOnlineUsers());
            Server.broadcastAll("UPDATE_ONLINE:" + Server.getOnlineUsers());

            String message;
            while ((message = in.readLine()) != null) {
                time = LocalTime.now().format(TIME_FORMAT);

                if (message.startsWith("PRIVATE:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String toUser = parts[1];
                        String text = parts[2];
                        DataBaseManager.saveMessage(username, toUser, text, time);
                        Server.sendPrivate(
                                "PRIVATE_MSG:" + username + ":[" + time + "] " + username + ": " + text + " ✓",
                                "PRIVATE_MSG:" + toUser + ":[" + time + "] Ти: " + text + " ✓",
                                username, toUser, this
                        );
                    }
                } else if (message.startsWith("READ:")) {
                    String fromUser = message.substring(5);
                    Server.sendRead(username, fromUser);
                } else if (message.startsWith("GET_HISTORY:")) {
                    String otherUser = message.substring(12);
                    String history = DataBaseManager.getHistory(username, otherUser);
                    out.println("HISTORY:" + otherUser + ":" + history);
                } else if (message.startsWith("SEARCH:")) {
                    String query = message.substring(7);
                    String results = DataBaseManager.searchMessages(username, query);
                    out.println("SEARCH_RESULT:" + results);
                } else if (message.startsWith("GET_CONTACTS:")) {
                String contacts = DataBaseManager.getContacts(username);
                out.println("CONTACTS:" + contacts);
                } else if (message.startsWith("TYPING:")) {
                String toUser = message.substring(7);
                Server.sendTyping(username, toUser);
                } else if (message.startsWith("DELETE_MSG:")) {
                    String withoutPrefix = message.substring("DELETE_MSG:".length());
                    int first = withoutPrefix.indexOf(":");
                    String toUser = withoutPrefix.substring(0, first);
                    String rest = withoutPrefix.substring(first + 1);
                    String time2 = rest.substring(0, 5);
                    String content = rest.substring(6);
                    DataBaseManager.deleteMessage(username, toUser, content, time2);
                    Server.sendPrivate(
                            "MSG_DELETED:" + username + ":" + time2 + ":" + content,
                            "MSG_DELETED:" + toUser + ":" + time2 + ":" + content,
                            username, toUser, this
                    );
                } else if (message.startsWith("EDIT_MSG:")) {
                    String withoutPrefix = message.substring("EDIT_MSG:".length());
                    int first = withoutPrefix.indexOf(":");
                    String toUser = withoutPrefix.substring(0, first);
                    String rest = withoutPrefix.substring(first + 1);
                    String time2 = rest.substring(0, 5);
                    String rest2 = rest.substring(6);
                    int colonIdx = rest2.indexOf(":");
                    String oldContent = rest2.substring(0, colonIdx);
                    String newContent = rest2.substring(colonIdx + 1);
                    DataBaseManager.editMessage(username, toUser, oldContent, newContent, time2);
                    Server.sendPrivate(
                            "MSG_EDITED:" + username + ":" + time2 + ":" + oldContent + ":" + newContent,
                            "MSG_EDITED:" + toUser + ":" + time2 + ":" + oldContent + ":" + newContent,
                            username, toUser, this
                    );
                } else if (message.startsWith("GET_USER_INFO:")) {
                String targetUser = message.substring(14);
                String info = DataBaseManager.getUserInfo(targetUser);
                boolean isOnline = Server.isAlreadyConnected(targetUser);
                out.println("USER_INFO:" + info + "|" + isOnline);
                } else if (message.startsWith("SAVE_PROFILE:")) {
                String data = message.substring("SAVE_PROFILE:".length());
                String[] parts = data.split("\\|", 3);
                if (parts.length == 3) {
                    DataBaseManager.saveProfile(username, parts[0], parts[1], parts[2]);
                    Server.broadcastAll("PROFILE_UPDATED:" + username + "|" + parts[0] + "|" + parts[1]);
                }
                } else if (message.startsWith("GET_PROFILE:")) {
                    String targetUser = message.substring("GET_PROFILE:".length());
                    String profile = DataBaseManager.getProfile(targetUser);
                    out.println("PROFILE:" + targetUser + "|" + profile);
                } else {
                    Server.broadcast("[" + time + "] " + username + ": " + message, this);
                }
            }

        } catch (IOException e) {
            System.out.println("Помилка: " + e.getMessage());
        } finally {
            if (username != null) {
                String time = LocalTime.now().format(TIME_FORMAT);
                Server.broadcast("[" + time + "] 🔴 " + username + " покинув чат", this);
                Server.removeClient(this);
                DataBaseManager.updateLastSeen(username);
                Server.broadcastAll("UPDATE_ONLINE:" + Server.getOnlineUsers());
                Server.showSessions();
            }
            try { socket.close(); } catch (IOException e) {}
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }
}