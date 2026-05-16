package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.DataBaseManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.model.User;

import java.sql.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() throws Exception {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Заповніть всі поля!");
            return;
        }

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM users WHERE username = ? AND password = ?")) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User loggedUser = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
                FXMLLoader chatLoader = new FXMLLoader(
                        LoginController.class.getClassLoader().getResource("view/Chat.fxml")
                );
                Scene chatScene = new Scene(chatLoader.load(), 900, 600);
                chatScene.getStylesheets().add(
                        LoginController.class.getClassLoader().getResource("style.css").toExternalForm()
                );
                ChatController chatController = chatLoader.getController();
                chatController.setCurrentUser(loggedUser);
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(chatScene);
                stage.setTitle("Messenger — " + username);
            } else {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText("Невірний логін або пароль!");
            }
        } catch (SQLException e) {
            errorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Заповніть всі поля!");
            return;
        }

        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password) VALUES (?, ?)")) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();

            errorLabel.setStyle("-fx-text-fill: #4ecca3;");
            errorLabel.setText("Реєстрація успішна! Тепер увійдіть.");

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                errorLabel.setStyle("-fx-text-fill: #e94560;");
                errorLabel.setText("Такий користувач вже існує!");
            } else {
                errorLabel.setText("Помилка: " + e.getMessage());
            }
        }
    }
}