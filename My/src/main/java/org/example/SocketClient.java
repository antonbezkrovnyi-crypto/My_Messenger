package org.example;

import java.io.*;
import java.net.*;

public class SocketClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private MessageListener listener;
    private OnlineListener onlineListener;
    private PrivateListener privateListener;
    private ReadListener readListener;
    private HistoryListener historyListener;
    private SearchListener searchListener;
    private ContactsListener contactsListener;
    private TypingListener typingListener;
    private DeleteListener deleteListener;
    private EditListener editListener;

    public interface MessageListener {
        void onMessage(String message);
    }

    public interface OnlineListener {
        void onOnlineUpdate(String[] users);
    }

    public interface PrivateListener {
        void onPrivateMessage(String from, String message);
    }

    public interface ReadListener {
        void onReadConfirm(String from);
    }

    public interface HistoryListener {
        void onHistory(String from, String history);
    }

    public interface SearchListener {
        void onSearchResult(String results);
    }

    public interface ContactsListener {
        void onContacts(String[] contacts);
    }

    public interface TypingListener {
        void onTyping(String from);
    }

    public interface DeleteListener {
        void onMessageDeleted(String from, String time, String content);
    }

    public interface EditListener {
        void onMessageEdited(String from, String time, String oldContent, String newContent);
    }

    public void setMessageListener(MessageListener l) {
        this.listener = l;
    }

    public void setOnlineListener(OnlineListener l) {
        this.onlineListener = l;
    }

    public void setPrivateListener(PrivateListener l) {
        this.privateListener = l;
    }

    public void setReadListener(ReadListener l) {
        this.readListener = l;
    }

    public void setHistoryListener(HistoryListener l) {
        this.historyListener = l;
    }

    public void setSearchListener(SearchListener l) {
        this.searchListener = l;
    }

    public void setContactsListener(ContactsListener l) {
        this.contactsListener = l;
    }

    public void setTypingListener(TypingListener l) {
        this.typingListener = l;
    }

    public void setDeleteListener(DeleteListener l) {
        this.deleteListener = l;
    }

    public void setEditListener(EditListener l) {
        this.editListener = l;
    }

    public boolean connect(String username) {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(username);
            new Thread(this::listenMessages).start();
            return true;
        } catch (IOException e) {
            System.err.println("Не вдалось підключитись: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public void sendPrivateMessage(String toUser, String message) {
        if (out != null) out.println("PRIVATE:" + toUser + ":" + message);
    }

    public void sendRead(String toUser) {
        if (out != null) out.println("READ:" + toUser);
    }

    public void requestHistory(String otherUser) {
        if (out != null) out.println("GET_HISTORY:" + otherUser);
    }

    public void searchMessages(String query) {
        if (out != null) out.println("SEARCH:" + query);
    }

    public void requestContacts() {
        if (out != null) out.println("GET_CONTACTS:");
    }

    public void sendTyping(String toUser) {
        if (out != null) out.println("TYPING:" + toUser);
    }

    public void deleteMessage(String toUser, String time, String content) {
        if (out != null) out.println("DELETE_MSG:" + toUser + ":" + time + ":" + content);
    }

    public void editMessage(String toUser, String time, String oldContent, String newContent) {
        if (out != null) out.println("EDIT_MSG:" + toUser + ":" + time + ":" + oldContent + ":" + newContent);
    }

    private void listenMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String msg = message;
                if (msg.startsWith("ONLINE:") || msg.startsWith("UPDATE_ONLINE:")) {
                    String raw = msg.replace("UPDATE_ONLINE:", "").replace("ONLINE:", "");
                    String[] users = raw.isEmpty() ? new String[0] : raw.split(",");
                    if (onlineListener != null)
                        javafx.application.Platform.runLater(() -> onlineListener.onOnlineUpdate(users));
                } else if (msg.startsWith("PRIVATE_MSG:")) {
                    String withoutPrefix = msg.substring("PRIVATE_MSG:".length());
                    int colonIndex = withoutPrefix.indexOf(":");
                    String from = withoutPrefix.substring(0, colonIndex);
                    String text = withoutPrefix.substring(colonIndex + 1);
                    if (privateListener != null)
                        javafx.application.Platform.runLater(() -> privateListener.onPrivateMessage(from, text));
                } else if (msg.startsWith("READ_CONFIRM:")) {
                    String from = msg.substring("READ_CONFIRM:".length());
                    if (readListener != null)
                        javafx.application.Platform.runLater(() -> readListener.onReadConfirm(from));
                } else if (msg.startsWith("HISTORY:")) {
                    String withoutPrefix = msg.substring("HISTORY:".length());
                    int colonIdx = withoutPrefix.indexOf(":");
                    String from = withoutPrefix.substring(0, colonIdx);
                    String history = withoutPrefix.substring(colonIdx + 1);
                    if (historyListener != null)
                        javafx.application.Platform.runLater(() -> historyListener.onHistory(from, history));
                } else if (msg.startsWith("SEARCH_RESULT:")) {
                    String results = msg.substring("SEARCH_RESULT:".length());
                    if (searchListener != null)
                        javafx.application.Platform.runLater(() -> searchListener.onSearchResult(results));
                } else if (msg.startsWith("CONTACTS:")) {
                    String raw = msg.substring("CONTACTS:".length());
                    String[] contacts = raw.isEmpty() ? new String[0] : raw.split(",");
                    if (contactsListener != null)
                        javafx.application.Platform.runLater(() -> contactsListener.onContacts(contacts));
                } else if (msg.startsWith("TYPING:")) {
                    String from = msg.substring(7);
                    if (typingListener != null)
                        javafx.application.Platform.runLater(() -> typingListener.onTyping(from));
                } else if (msg.startsWith("MSG_DELETED:")) {
                    String withoutPrefix = msg.substring("MSG_DELETED:".length());
                    int first = withoutPrefix.indexOf(":");
                    String from = withoutPrefix.substring(0, first);
                    String rest = withoutPrefix.substring(first + 1);
                    String time = rest.substring(0, 5);
                    String content = rest.substring(6);
                    if (deleteListener != null) {
                        javafx.application.Platform.runLater(() -> deleteListener.onMessageDeleted(from, time, content));
                    }
                } else if (msg.startsWith("MSG_EDITED:")) {
                    String withoutPrefix = msg.substring("MSG_EDITED:".length());
                    int first = withoutPrefix.indexOf(":");
                    String from = withoutPrefix.substring(0, first);
                    String rest = withoutPrefix.substring(first + 1);
                    String time = rest.substring(0, 5);
                    String rest2 = rest.substring(6);
                    int colonIdx = rest2.indexOf(":");
                    String oldContent = rest2.substring(0, colonIdx);
                    String newContent = rest2.substring(colonIdx + 1);
                    if (editListener != null) {
                        javafx.application.Platform.runLater(() -> editListener.onMessageEdited(from, time, oldContent, newContent));
                    }
                } else if (msg.startsWith("USER_INFO:")) {
                    String data = msg.substring("USER_INFO:".length());
                    String[] parts = data.split("\\|");
                    if (parts.length == 3) {
                        String uname = parts[0];
                        String lastSeen = parts[1];
                        boolean isOnline = Boolean.parseBoolean(parts[2]);
                        if (userInfoListener != null) {
                            javafx.application.Platform.runLater(() -> userInfoListener.onUserInfo(uname, lastSeen, isOnline));
                        }
                    }
                } else if (msg.startsWith("PROFILE:")) {
                    String data = msg.substring("PROFILE:".length());
                    String[] parts = data.split("\\|", 4);
                    if (parts.length == 4 && profileListener != null) {
                        javafx.application.Platform.runLater(() ->
                                profileListener.onProfile(parts[0], parts[1], parts[2], parts[3]));
                    }
                } else if (msg.startsWith("PROFILE_UPDATED:")) {
                    String data = msg.substring("PROFILE_UPDATED:".length());
                    String[] parts = data.split("\\|", 3);
                    if (parts.length == 3 && profileUpdatedListener != null) {
                        javafx.application.Platform.runLater(() ->
                                profileUpdatedListener.onProfileUpdated(parts[0], parts[1], parts[2]));
                    }
                } else {
                    if (listener != null) javafx.application.Platform.runLater(() -> listener.onMessage(msg));
                }
            }
        } catch (IOException e) {
            System.err.println("З'єднання втрачено");
        } finally {
            if (onlineListener != null) {
                javafx.application.Platform.runLater(() -> onlineListener.onOnlineUpdate(new String[0]));
            }
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
        }
    }

    public interface UserInfoListener {
        void onUserInfo(String username, String lastSeen, boolean isOnline);
    }

    private UserInfoListener userInfoListener;

    public void setUserInfoListener(UserInfoListener l) {
        this.userInfoListener = l;
    }

    public void requestUserInfo(String username) {
        if (out != null) out.println("GET_USER_INFO:" + username);
    }

    public interface ProfileListener {
        void onProfile(String username, String displayName, String color, String bio);
    }

    public interface ProfileUpdatedListener {
        void onProfileUpdated(String username, String displayName, String color);
    }

    private ProfileListener profileListener;
    private ProfileUpdatedListener profileUpdatedListener;

    public void setProfileListener(ProfileListener l) {
        this.profileListener = l;
    }

    public void setProfileUpdatedListener(ProfileUpdatedListener l) {
        this.profileUpdatedListener = l;
    }

    public void saveProfile(String displayName, String color, String bio) {
        if (out != null) out.println("SAVE_PROFILE:" + displayName + "|" + color + "|" + bio);
    }

    public void getProfile(String username) {
        if (out != null) out.println("GET_PROFILE:" + username);
    }
}