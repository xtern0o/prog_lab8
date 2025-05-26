package org.example.client.gui.applications;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainView extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.setTitle("Коллекция");
        stage.setMinHeight(550);
        stage.setMinWidth(800);

        stage.show();
    }

    public static Scene createScene() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainView.class.getResource("/gui/MainView.fxml"));
        Parent root = loader.load();
        return new Scene(root);
    }
}
