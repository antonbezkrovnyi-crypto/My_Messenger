package org.example;

import javafx.fxml.FXMLLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Messager extends Application {

    @Override
    public void start(Stage stage) throws Exception{

        DataBaseManager.initialize();
        SoundManager.init();

        var url = Messager.class.getClassLoader().getResource("view/Login.fxml");
        System.out.println("FXML URL: " + url);

        FXMLLoader loader = new FXMLLoader(url);
        Scene scene = new Scene(loader.load(), 900, 600);
        scene.getStylesheets().add(
                Messager.class.getClassLoader().getResource("style.css").toExternalForm()
        );
        stage.setTitle("Messenger");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}