package org.example.client.gui.applications;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthView extends Application {
    @Override
    public void start(Stage stage) {
        try {
//            if (AuthManager.getCurrentUser() != null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/AuthView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.setTitle("Аутентификация");
            stage.setMinHeight(550);
            stage.setMinWidth(600);

            stage.show();

        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }
}
