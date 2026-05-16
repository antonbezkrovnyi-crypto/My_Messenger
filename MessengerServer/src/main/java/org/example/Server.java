package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        DataBaseManager.initialize();
        System.out.println("╔════════════════════════════╗");
        System.out.println("║   Messenger Server v1.0    ║");
        System.out.println("║   Порт: " + PORT + "               ║");
        System.out.println("╚════════════════════════════╝");

        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void broadcastAll(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static void sendPrivate(String messageToReceiver, String messageToSender,
                                   String fromUser, String toUser, ClientHandler sender) {
        System.out.println("[Приватне] " + fromUser + " → " + toUser);
        boolean found = false;
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(toUser)) {
                client.sendMessage(messageToReceiver);
                found = true;
            }
            if (client == sender) {
                client.sendMessage(messageToSender);
            }
        }
        if (!found) {
            sender.sendMessage("❌ Користувач " + toUser + " не в мережі");
        }
    }

    public static void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    public static boolean isAlreadyConnected(String username) {
        for (ClientHandler client : clients) {
            if (username.equals(client.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public static String getOnlineUsers() {
        StringBuilder sb = new StringBuilder("ONLINE:");
        Set<String> seen = new LinkedHashSet<>();
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                seen.add(client.getUsername());
            }
        }
        sb.append(String.join(",", seen));
        return sb.toString();
    }

    public static void showSessions() {
        System.out.println("\n╔══════ Активні сесії (" + clients.size() + ") ══════╗");
        for (ClientHandler client : clients) {
            System.out.println("║  🟢 " + client.getUsername());
        }
        System.out.println("╚═══════════════════════════╝");
    }

    public static List<ClientHandler> getClients() {
        return clients;
    }

    public static void sendRead(String reader, String toUser) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(toUser)) {
                client.sendMessage("READ_CONFIRM:" + reader);
            }
        }
    }

    public static void sendTyping(String from, String toUser) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(toUser)) {
                client.sendMessage("TYPING:" + from);
            }
        }
    }
}