package org.example.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.client.gui.controllers.AuthViewController;
import org.example.client.gui.controllers.EditViewController;
import org.example.client.gui.controllers.MainViewController;
import org.example.client.managers.AuthManager;
import org.example.common.entity.Ticket;

import java.io.IOException;

public class App extends Application {
    private Stage currentStage;
    @Override
    public void start(Stage stage) throws Exception {
        currentStage = stage;
        runAuth();
    }

    public void runAuth() {
        try {
            FXMLLoader authLoader = new FXMLLoader(getClass().getResource("/gui/AuthView.fxml"));
            Parent authRoot = authLoader.load();

            Scene scene = new Scene(authRoot);
            currentStage.setScene(scene);

            AuthViewController authViewController = authLoader.getController();
            authViewController.setCallback(this::runMain);

            currentStage.setTitle("Аутентификация");
            currentStage.setMinHeight(550);
            currentStage.setMinWidth(600);

            currentStage.show();
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    public void runMain() {
        try {
            FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/gui/MainView.fxml"));
            Parent mainRoot = mainLoader.load();
            MainViewController mainViewController = mainLoader.getController();
            mainViewController.setAuthCallback(this::runAuth);
            mainViewController.setEditCallback(this::runEdit);

            Scene mainScene = new Scene(mainRoot);
            currentStage.setScene(mainScene);

            currentStage.setTitle("Коллекция");
            currentStage.setMinHeight(550);
            currentStage.setMinWidth(800);

            currentStage.show();
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    public void runEdit(Ticket ticket) {
        try {
            FXMLLoader editLoader = new FXMLLoader(getClass().getResource("/gui/EditView.fxml"));
            Parent editRoot = editLoader.load();

            EditViewController editViewController = editLoader.getController();
            editViewController.setTicket(ticket);
            editViewController.setMainCallback(this::runMain);

            Scene editScene = new Scene(editRoot);
            currentStage.setScene(editScene);
            currentStage.setTitle("Объект билет");
            currentStage.setMinHeight(700);
            currentStage.setMinWidth(500);
            currentStage.show();


        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }
}
