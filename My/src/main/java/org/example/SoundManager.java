package org.example;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundManager {

    private static MediaPlayer messageSound;

    public static void init() {
        try {
            var url = SoundManager.class.getClassLoader().getResource("sounds/message.mp3");
            if (url != null) {
                messageSound = new MediaPlayer(new Media(url.toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Не вдалось завантажити звук: " + e.getMessage());
        }
    }

    public static void playMessage() {
        try {
            if (messageSound != null) {
                messageSound.stop();
                messageSound.play();
            }
        } catch (Exception e) {
            System.err.println("Помилка відтворення: " + e.getMessage());
        }
    }
}