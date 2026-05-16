package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.SocketClient;
import org.example.SoundManager;
import org.example.model.User;
import java.util.*;

public class ChatController {

    @FXML private ListView<String> contactsList;
    @FXML private ListView<String> messagesList;
    @FXML private TextField messageField;
    @FXML private TextField searchField;
    @FXML private Label currentUserLabel;
    @FXML private Label chatNameLabel;
    @FXML private Label chatStatusLabel;
    @FXML private Label typingLabel;
    @FXML private Circle myAvatar;
    @FXML private Circle chatAvatar;

    private User currentUser;
    private SocketClient socketClient;
    private String selectedContact = null;
    private Map<String, List<String>> chatHistory = new HashMap<>();
    private Map<String, Integer> unreadCount = new HashMap<>();
    private Map<String, String> avatarColors = new HashMap<>();
    private Set<String> knownContacts = new LinkedHashSet<>();
    private String[] lastOnlineUsers = new String[0];
    private String[] colors = {"#e94560", "#4ecca3", "#f5a623", "#7b68ee", "#ff6b6b", "#48dbfb"};
    private int colorIndex = 0;
    private String myBio = "";
    private javafx.animation.Timeline typingTimer;
    private javafx.stage.Popup emojiPopup = null;
    private Map<String, String> pendingProfileBio = new HashMap<>();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        currentUserLabel.setText(user.getUsername());
        myAvatar.setStyle("-fx-fill: " + getAvatarColor(user.getUsername()) + ";");

        setupContactsList();
        setupMessagesList();
        setupSocket(user);

        messageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedContact != null && !newVal.isEmpty()) {
                socketClient.sendTyping(selectedContact);
            }
        });

        chatNameLabel.setText("Оберіть контакт");
        chatStatusLabel.setText("");
    }

    private String getAvatarColor(String username) {
        if (!avatarColors.containsKey(username)) {
            avatarColors.put(username, colors[colorIndex % colors.length]);
            colorIndex++;
        }
        return avatarColors.get(username);
    }

    private String getInitial(String username) {
        return username.isEmpty() ? "?" : String.valueOf(username.charAt(0)).toUpperCase();
    }

    private void refreshContactsList(String myUsername, String[] onlineUsers) {
        Set<String> onlineSet = new HashSet<>(Arrays.asList(onlineUsers));
        contactsList.getItems().clear();

        for (String contact : knownContacts) {
            if (onlineSet.contains(contact)) contactsList.getItems().add("🟢 " + contact);
        }
        for (String contact : knownContacts) {
            if (!onlineSet.contains(contact)) contactsList.getItems().add("⚫ " + contact);
        }
        for (String contact : knownContacts) {
            chatHistory.putIfAbsent(contact, new ArrayList<>());
            unreadCount.putIfAbsent(contact, 0);
        }

        if (selectedContact != null) {
            for (String item : contactsList.getItems()) {
                String name = item.replace("🟢 ", "").replace("⚫ ", "");
                if (name.equals(selectedContact)) {
                    boolean isOnline = item.startsWith("🟢");
                    chatStatusLabel.setText(isOnline ? "в мережі" : "не в мережі");
                    chatStatusLabel.setStyle("-fx-text-fill: " + (isOnline ? "#4ecca3" : "#888") + "; -fx-font-size: 11px;");
                    break;
                }
            }
        }
    }

    private void setupContactsList() {
        contactsList.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                    setStyle("-fx-background-color: #16213e;");
                    return;
                }

                String cleanName = item.replace("🟢 ", "").replace("⚫ ", "");
                boolean isOnline = item.startsWith("🟢");
                int unread = unreadCount.getOrDefault(cleanName, 0);
                String color = getAvatarColor(cleanName);

                StackPane avatar = new StackPane();
                Circle circle = new Circle(18);
                circle.setStyle("-fx-fill: " + color + ";");
                Label initial = new Label(getInitial(cleanName));
                initial.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                Circle online = new Circle(5);
                online.setStyle("-fx-fill: " + (isOnline ? "#4ecca3" : "#888") + "; -fx-stroke: #16213e; -fx-stroke-width: 1.5;");
                StackPane.setAlignment(online, Pos.BOTTOM_RIGHT);
                avatar.getChildren().addAll(circle, initial, online);

                Label nameLabel = new Label(cleanName);
                nameLabel.setStyle("-fx-text-fill: " + (unread > 0 ? "white" : (isOnline ? "#ccc" : "#888")) +
                        "; -fx-font-size: 13px;" + (unread > 0 ? " -fx-font-weight: bold;" : ""));

                VBox info = new VBox(2, nameLabel);
                HBox.setHgrow(info, Priority.ALWAYS);
                HBox cell = new HBox(12, avatar, info);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setStyle("-fx-padding: 8 10;");

                if (unread > 0) {
                    Label badge = new Label(String.valueOf(unread));
                    badge.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10;");
                    cell.getChildren().add(badge);
                    setStyle("-fx-background-color: #1a2a4a;");
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #0f3460;");
                } else {
                    setStyle("-fx-background-color: transparent;");
                }

                setGraphic(cell);
                setText(null);
            }
        });

        contactsList.setOnMouseClicked(event -> {
            String selected = contactsList.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            if (event.getClickCount() == 2) {
                String contact = selected.replace("🟢 ", "").replace("⚫ ", "");
                socketClient.getProfile(contact);
                new Thread(() -> {
                    try { Thread.sleep(200); } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> socketClient.requestUserInfo(contact));
                }).start();
                return;
            }

            selectedContact = selected.replace("🟢 ", "").replace("⚫ ", "");
            chatNameLabel.setText(selectedContact);
            boolean isOnline = selected.startsWith("🟢");
            chatStatusLabel.setText(isOnline ? "в мережі" : "не в мережі");
            chatStatusLabel.setStyle("-fx-text-fill: " + (isOnline ? "#4ecca3" : "#888") + "; -fx-font-size: 11px;");
            typingLabel.setText("");
            chatAvatar.setStyle("-fx-fill: " + getAvatarColor(selectedContact) + ";");
            messageField.setPromptText("Написати " + selectedContact + "...");
            chatHistory.putIfAbsent(selectedContact, new ArrayList<>());
            unreadCount.put(selectedContact, 0);
            socketClient.sendRead(selectedContact);
            socketClient.requestHistory(selectedContact);
            contactsList.refresh();

        });
    }

    private void setupMessagesList() {
        messagesList.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                    setStyle("-fx-background-color: #1a1a2e;");
                    return;
                }

                boolean isMyMessage = item.contains("] Ти:");
                boolean isSystem = item.contains("приєднався") || item.contains("покинув");

                if (isSystem) {
                    Label label = new Label(item);
                    label.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-padding: 2 8;");
                    HBox box = new HBox(label);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                String time = "";
                String text = item;
                String checks = "";

                if (item.startsWith("[")) {
                    int end = item.indexOf("]");
                    if (end > 0) { time = item.substring(1, end); text = item.substring(end + 2); }
                }

                if (text.endsWith(" ✓✓")) { checks = "✓✓"; text = text.substring(0, text.length() - 3); }
                else if (text.endsWith(" ✓")) { checks = "✓"; text = text.substring(0, text.length() - 2); }

                String sender = "";
                int colonIdx = text.indexOf(": ");
                if (colonIdx > 0) { sender = text.substring(0, colonIdx); text = text.substring(colonIdx + 2); }

                Pos alignment = isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
                String bubbleColor = isMyMessage ? "#0f3460" : "#16213e";

                StackPane avatar = new StackPane();
                Circle circle = new Circle(15);
                circle.setStyle("-fx-fill: " + (isMyMessage ? getAvatarColor(currentUser.getUsername()) : getAvatarColor(sender)) + ";");
                Label initial = new Label(isMyMessage ? getInitial(currentUser.getUsername()) : getInitial(sender));
                initial.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                avatar.getChildren().addAll(circle, initial);

                VBox bubble = new VBox(2);
                bubble.setStyle("-fx-background-color: " + bubbleColor + "; -fx-background-radius: " + (isMyMessage ? "15 15 0 15" : "15 15 15 0") + "; -fx-padding: 8 12;");
                bubble.setMaxWidth(300);

                Label msgLabel = new Label(text);
                msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                msgLabel.setWrapText(true);
                msgLabel.setMaxWidth(280);

                HBox meta = new HBox(4);
                meta.setAlignment(Pos.CENTER_RIGHT);
                Label timeLabel = new Label(time);
                timeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
                meta.getChildren().add(timeLabel);

                if (!checks.isEmpty() && isMyMessage) {
                    Label checkLabel = new Label(checks);
                    checkLabel.setStyle("-fx-text-fill: " + (checks.equals("✓✓") ? "#4ecca3" : "#888") + "; -fx-font-size: 10px; -fx-font-weight: bold;");
                    meta.getChildren().add(checkLabel);
                }

                bubble.getChildren().addAll(msgLabel, meta);

                if (isMyMessage) {
                    final String finalText = text;
                    final String finalTime = time;

                    ContextMenu contextMenu = new ContextMenu();
                    contextMenu.setStyle("-fx-background-color: #16213e; -fx-border-color: #0f3460; -fx-border-width: 1;");

                    MenuItem editItem = new MenuItem("✏️ Редагувати");
                    editItem.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                    editItem.setOnAction(e -> {
                        TextInputDialog dialog = new TextInputDialog(finalText);
                        dialog.setTitle("Редагувати");
                        dialog.setHeaderText(null);
                        dialog.setContentText("Новий текст:");
                        dialog.getDialogPane().setStyle("-fx-background-color: #16213e;");
                        dialog.getEditor().setStyle("-fx-background-color: #0f3460; -fx-text-fill: white;");
                        dialog.showAndWait().ifPresent(newText -> {
                            if (!newText.isEmpty() && !newText.equals(finalText)) {
                                List<String> history = chatHistory.getOrDefault(selectedContact, new ArrayList<>());
                                for (int i = 0; i < history.size(); i++) {
                                    if (history.get(i).contains("[" + finalTime + "]") && history.get(i).contains(finalText)) {
                                        history.set(i, history.get(i).replace(finalText, newText + " ✏️"));
                                    }
                                }
                                loadChat(selectedContact);
                                socketClient.editMessage(selectedContact, finalTime, finalText, newText);
                            }
                        });
                    });

                    MenuItem deleteItem = new MenuItem("🗑 Видалити");
                    deleteItem.setStyle("-fx-text-fill: #e94560; -fx-font-size: 13px;");
                    deleteItem.setOnAction(e -> {
                        List<String> history = chatHistory.getOrDefault(selectedContact, new ArrayList<>());
                        history.removeIf(msg -> msg.contains("[" + finalTime + "]") && msg.contains(finalText));
                        loadChat(selectedContact);
                        socketClient.deleteMessage(selectedContact, finalTime, finalText);
                    });

                    contextMenu.getItems().addAll(editItem, deleteItem);
                    bubble.setOnContextMenuRequested(e -> contextMenu.show(bubble, e.getScreenX(), e.getScreenY()));
                }

                HBox row = new HBox(8);
                row.setAlignment(alignment);
                row.setStyle("-fx-padding: 3 10;");

                if (isMyMessage) row.getChildren().addAll(bubble, avatar);
                else row.getChildren().addAll(avatar, bubble);

                setGraphic(row);
                setStyle("-fx-background-color: transparent;");
                setText(null);
            }
        });
    }

    private void setupSocket(User user) {
        socketClient = new SocketClient();

        socketClient.setContactsListener(contacts -> {
            for (String contact : contacts) {
                if (!contact.isEmpty() && !contact.equals(user.getUsername())) knownContacts.add(contact);
            }
            refreshContactsList(user.getUsername(), lastOnlineUsers);
        });

        socketClient.setMessageListener(message -> {});

        socketClient.setOnlineListener(users -> {
            lastOnlineUsers = users;
            for (String u : users) {
                if (!u.isEmpty() && !u.equals(user.getUsername())) knownContacts.add(u);
            }
            refreshContactsList(user.getUsername(), users);
        });

        socketClient.setPrivateListener((from, message) -> {
            knownContacts.add(from);
            chatHistory.putIfAbsent(from, new ArrayList<>());
            chatHistory.get(from).add(message);
            if (from.equals(selectedContact)) {
                loadChat(selectedContact);
            } else {
                unreadCount.put(from, unreadCount.getOrDefault(from, 0) + 1);
                SoundManager.playMessage();
                contactsList.refresh();
            }
        });

        socketClient.setReadListener(from -> {
            List<String> history = chatHistory.getOrDefault(from, new ArrayList<>());
            for (int i = 0; i < history.size(); i++) {
                if (history.get(i).endsWith(" ✓")) history.set(i, history.get(i).replace(" ✓", " ✓✓"));
            }
            if (from.equals(selectedContact)) loadChat(selectedContact);
        });

        socketClient.setHistoryListener((from, history) -> {
            if (history.isEmpty()) return;
            knownContacts.add(from);
            chatHistory.putIfAbsent(from, new ArrayList<>());
            List<String> messages = chatHistory.get(from);
            messages.clear();

            for (String entry : history.split(";")) {
                if (entry.isEmpty()) continue;
                String[] parts = entry.split("\\|", 3);
                if (parts.length == 3) {
                    String sender = parts[0];
                    String content = parts[1];
                    String time = parts[2];
                    if (sender.equals(currentUser.getUsername())) {
                        messages.add("[" + time + "] Ти: " + content + " ✓✓");
                    } else {
                        messages.add("[" + time + "] " + sender + ": " + content);
                    }
                }
            }
            if (from.equals(selectedContact)) loadChat(from);
        });

        socketClient.setTypingListener(from -> {
            if (from.equals(selectedContact)) {
                typingLabel.setText("друкує...");
                if (typingTimer != null) typingTimer.stop();
                typingTimer = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> typingLabel.setText(""))
                );
                typingTimer.play();
            }
        });

        socketClient.setDeleteListener((from, time, content) -> {
            String key = from.equals(currentUser.getUsername()) ? selectedContact : from;
            if (key == null) return;
            List<String> history = chatHistory.getOrDefault(key, new ArrayList<>());
            history.removeIf(msg -> msg.contains("[" + time + "]") && msg.contains(content));
            if (key.equals(selectedContact)) loadChat(selectedContact);
        });

        socketClient.setEditListener((from, time, oldContent, newContent) -> {
            String key = from.equals(currentUser.getUsername()) ? selectedContact : from;
            if (key == null) return;
            List<String> history = chatHistory.getOrDefault(key, new ArrayList<>());
            for (int i = 0; i < history.size(); i++) {
                if (history.get(i).contains("[" + time + "]") && history.get(i).contains(oldContent)) {
                    history.set(i, history.get(i).replace(oldContent, newContent + " ✏️"));
                }
            }
            if (key.equals(selectedContact)) loadChat(selectedContact);
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedContact == null) return;
            if (newVal.isEmpty()) { loadChat(selectedContact); return; }
            if (newVal.length() >= 2) {
                List<String> history = chatHistory.getOrDefault(selectedContact, new ArrayList<>());
                messagesList.getItems().clear();
                for (String msg : history) {
                    if (msg.toLowerCase().contains(newVal.toLowerCase())) messagesList.getItems().add(msg);
                }
                if (messagesList.getItems().isEmpty()) messagesList.getItems().add("Нічого не знайдено");
            }
        });

        socketClient.setUserInfoListener((username, lastSeen, isOnline) -> {
            showUserProfile(username, lastSeen, isOnline);
        });

        socketClient.setProfileUpdatedListener((username, displayName, color) -> {
            avatarColors.put(username, color);
            contactsList.refresh();
            if (username.equals(selectedContact)) {
                chatAvatar.setStyle("-fx-fill: " + color + ";");
                chatNameLabel.setText(displayName);
            }
        });

        socketClient.setProfileListener((username, displayName, color, bio) -> {
            if (username.equals(currentUser.getUsername())) {
                avatarColors.put(username, color);
                myAvatar.setStyle("-fx-fill: " + color + ";");
                currentUserLabel.setText(displayName);
                myBio = bio;
            } else {
                pendingProfileBio.put(username, bio);
                avatarColors.put(username, color);
                contactsList.refresh();
            }
        });

        socketClient.connect(user.getUsername());
        socketClient.requestContacts();
    }

    private void showUserProfile(String username, String lastSeen, boolean isOnline) {
        javafx.stage.Stage profileStage = new javafx.stage.Stage();
        profileStage.setTitle("Профіль — " + username);

        String color = getAvatarColor(username);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 30;");
        root.setAlignment(Pos.TOP_CENTER);

        javafx.scene.layout.StackPane avatarPane = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(50);
        circle.setStyle("-fx-fill: " + color + ";");
        Label initial = new Label(getInitial(username));
        initial.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");
        avatarPane.getChildren().addAll(circle, initial);

        javafx.scene.layout.HBox statusBox = new javafx.scene.layout.HBox(8);
        statusBox.setAlignment(Pos.CENTER);
        javafx.scene.shape.Circle statusDot = new javafx.scene.shape.Circle(6);
        statusDot.setStyle("-fx-fill: " + (isOnline ? "#4ecca3" : "#888") + ";");
        Label statusLabel = new Label(isOnline ? "В мережі" : "Не в мережі");
        statusLabel.setStyle("-fx-text-fill: " + (isOnline ? "#4ecca3" : "#888") + "; -fx-font-size: 13px;");
        statusBox.getChildren().addAll(statusDot, statusLabel);

        Label nameLabel = new Label(username);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        javafx.scene.layout.VBox infoBox = new javafx.scene.layout.VBox(10);
        infoBox.setStyle(
                "-fx-background-color: #16213e;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 15 20;"
        );
        infoBox.setMaxWidth(300);

        Label lastSeenTitle = new Label("Остання активність");
        lastSeenTitle.setStyle("-fx-text-fill: #4ecca3; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lastSeenLabel = new Label(isOnline ? "Зараз онлайн" : lastSeen);
        lastSeenLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(lastSeenTitle, lastSeenLabel);

        String bio = pendingProfileBio.getOrDefault(username, "");
        if (!bio.isEmpty()) {
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #0f3460;");
            Label bioTitle = new Label("ОПИС");
            bioTitle.setStyle("-fx-text-fill: #4ecca3; -fx-font-size: 11px; -fx-font-weight: bold;");
            Label bioLabel = new Label(bio);
            bioLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            bioLabel.setWrapText(true);
            bioLabel.setMaxWidth(260);
            infoBox.getChildren().addAll(sep, bioTitle, bioLabel);
        }

        Button msgBtn = new Button("Написати повідомлення");
        msgBtn.setStyle(
                "-fx-background-color: #e94560;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;"
        );
        msgBtn.setOnAction(e -> profileStage.close());

        root.getChildren().addAll(avatarPane, nameLabel, statusBox, infoBox, msgBtn);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 350, 400);
        scene.getStylesheets().add(
                ChatController.class.getClassLoader().getResource("style.css").toExternalForm()
        );
        profileStage.setScene(scene);
        profileStage.show();
    }

    private void loadChat(String chatKey) {
        messagesList.getItems().clear();
        List<String> history = chatHistory.getOrDefault(chatKey, new ArrayList<>());
        messagesList.getItems().addAll(history);
        messagesList.scrollTo(messagesList.getItems().size() - 1);
    }

    @FXML
    private void handleSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty() || selectedContact == null) return;
        socketClient.sendPrivateMessage(selectedContact, text);
        messageField.clear();
    }

    public String getMyColor() { return getAvatarColor(currentUser.getUsername()); }
    public void updateMyColor(String color) { avatarColors.put(currentUser.getUsername(), color); myAvatar.setStyle("-fx-fill: " + color + ";"); }
    public void updateMyName(String name) { currentUserLabel.setText(name); }
    public String getMyBio() { return myBio; }
    public void updateMyBio(String bio) { this.myBio = bio; }

    @FXML
    private void handleOpenProfile() throws Exception {
        FXMLLoader loader = new FXMLLoader(ChatController.class.getClassLoader().getResource("view/Profile.fxml"));
        Scene scene = new Scene(loader.load(), 400, 600);
        scene.getStylesheets().add(ChatController.class.getClassLoader().getResource("style.css").toExternalForm());
        ProfileController ctrl = loader.getController();
        ctrl.setData(currentUser, this);
        Stage profileStage = new Stage();
        profileStage.setTitle("Профіль");
        profileStage.setScene(scene);
        profileStage.show();
    }

    @FXML
    private void handleEmojiPanel() {
        if (emojiPopup != null && emojiPopup.isShowing()) { emojiPopup.hide(); return; }

        String[][] emojis = {
                {"😊", "😂", "❤", "👍", "🔥", "😍", "🎉", "😢"},
                {"😎", "🤔", "😅", "🙏", "💪", "👋", "🤝", "✅"},
                {"😡", "😱", "🤣", "💀", "👀", "🥰", "😴", "🤯"},
                {"🎵", "🎮", "🍕", "🐱", "🐶", "🌟", "💯", "🚀"}
        };

        javafx.scene.layout.VBox panel = new javafx.scene.layout.VBox(5);
        panel.setStyle("-fx-background-color: #16213e; -fx-padding: 10; -fx-border-color: #0f3460; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 5);");

        for (String[] row : emojis) {
            javafx.scene.layout.HBox rowBox = new javafx.scene.layout.HBox(2);
            for (String emoji : row) {
                Button btn = new Button(emoji);
                btn.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 20));
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4; -fx-border-width: 0;");
                btn.setOnAction(e -> { messageField.appendText(emoji); messageField.requestFocus(); emojiPopup.hide(); });
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #0f3460; -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 5; -fx-border-width: 0;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4; -fx-border-width: 0;"));
                rowBox.getChildren().add(btn);
            }
            panel.getChildren().add(rowBox);
        }

        emojiPopup = new javafx.stage.Popup();
        emojiPopup.getContent().add(panel);
        emojiPopup.setAutoHide(true);

        javafx.geometry.Bounds bounds = messageField.localToScreen(messageField.getBoundsInLocal());
        emojiPopup.show(messageField.getScene().getWindow(), bounds.getMinX(), bounds.getMinY() - 220);
    }

    public void saveProfileToServer(String displayName, String color, String bio) {
        socketClient.saveProfile(displayName, color, bio);
    }
}