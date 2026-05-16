package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.model.User;

public class ProfileController {

    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitial;
    @FXML private HBox colorPicker;
    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextArea bioField;
    @FXML private Label statusLabel;

    private User currentUser;
    private String selectedColor;
    private ChatController chatController;

    public void setData(User user, ChatController chatController) {
        this.currentUser = user;
        this.chatController = chatController;
        this.selectedColor = chatController.getMyColor();

        nameField.setText(user.getUsername());
        usernameField.setText(user.getUsername());
        avatarInitial.setText(String.valueOf(user.getUsername().charAt(0)).toUpperCase());
        avatarCircle.setStyle("-fx-fill: " + selectedColor + ";");
        bioField.setText(chatController.getMyBio());
    }

    @FXML
    private void handleChangeColor() {
        colorPicker.setVisible(!colorPicker.isVisible());
        colorPicker.setManaged(!colorPicker.isManaged());
    }

    @FXML
    private void selectColor(MouseEvent event) {
        Circle clicked = (Circle) event.getSource();
        String style = clicked.getStyle();
        String color = style.replace("-fx-fill: ", "")
                .replace("; -fx-cursor: hand;", "").trim();
        selectedColor = color;
        avatarCircle.setStyle("-fx-fill: " + color + ";");
        colorPicker.setVisible(false);
        colorPicker.setManaged(false);
    }

    @FXML
    private void handleSave() {
        String newName = nameField.getText().trim();
        String bio = bioField.getText().trim();

        if (newName.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #e94560;");
            statusLabel.setText("Ім'я не може бути порожнім!");
            return;
        }

        chatController.updateMyColor(selectedColor);
        chatController.updateMyName(newName);
        chatController.updateMyBio(bio);

        chatController.saveProfileToServer(newName, selectedColor, bio);

        statusLabel.setStyle("-fx-text-fill: #4ecca3;");
        statusLabel.setText("Збережено успішно!");
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}