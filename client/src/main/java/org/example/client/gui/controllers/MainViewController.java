package org.example.client.gui.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.example.client.managers.AuthManager;

import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {
    @FXML private Label usernameLabel;
    @FXML private Label statusCodeBar;
    @FXML private Label statusMessage;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;

    @Getter
    @Setter
    private Runnable authCallback;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (AuthManager.getCurrentUser() == null) {
            throw new RuntimeException("Пользователь не авторизован");
        }
        usernameLabel.setText(AuthManager.getCurrentUser().login());

        statusCodeBar.setText("");
        statusMessage.setText("");
        statusLabel.setText("");
        messageLabel.setText("");
    }

    private void statusBarNotify(String code, String message) {
        statusCodeBar.setText(code);
        statusMessage.setText(message);
        statusLabel.setText("Статус:");
        messageLabel.setText("Сообщение:");

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(5),
                actionEvent -> {
                    statusMessage.setText("");
                    statusCodeBar.setText("");
                    statusLabel.setText("");
                    messageLabel.setText("");
                }
        ));
        timeline.setCycleCount(1);
        timeline.play();
    }

    @FXML
    private void synchronizeCollection() {
    }

    @FXML
    private void getInfo() {
    }

    @FXML
    private void logout() {
        AuthManager.setCurrentUser(null);
        this.authCallback.run();
    }

    @FXML
    private void showHistory() {
    }

    @FXML
    private void getHead() {
    }

    @FXML
    private void printUniqueDiscountCommand() {
    }

    @FXML
    private void printFieldDescendingPersonCommand() {
    }

    @FXML
    private void executeScriptCommand() {
    }

    @FXML
    private void setRuLang() {
    }

    @FXML
    private void setCzLang() {
    }

    @FXML
    private void setBgLang() {
    }

    @FXML
    private void setEsgvLang() {
    }

    @FXML
    private void addElement() {
    }

    @FXML
    private void filterStartsByNameFilter() {
    }

    @FXML
    private void processItemEdit() {
    }

}
