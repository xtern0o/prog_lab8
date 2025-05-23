package org.example.client.gui.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.client.Main;
import org.example.client.managers.AuthManager;
import org.example.client.managers.Client;
import org.example.client.utils.ClientConfig;
import org.example.client.utils.ClientSingleton;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.dtp.User;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class AuthViewController implements Initializable {
    @FXML private TabPane mainTab;
    @FXML private Tab loginTab;
    @FXML private Tab regTab;
    @FXML private Label statusBar;
    @FXML private AnchorPane mainAnchor;

    @FXML private TextField loginInput;
    @FXML private PasswordField passwordInput;
    @FXML private Button authButton;
    @FXML private Hyperlink registerLink;

    @FXML private TextField regLoginInput;
    @FXML private PasswordField regPasswordInput;
    @FXML private PasswordField regRepeatpasswordInput;
    @FXML private Button regButton;

    //    private final Client client = new Client(ClientConfig.getInstance().getHost(), ClientConfig.getInstance().getPort(), 100, 10, Main.consoleOutput, false);
    private final Client client = ClientSingleton.getClient();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusBar.setText("");
        mainTab.getSelectionModel().select(loginTab);
    }

    @FXML
    private void loginButtonPressed(ActionEvent actionEvent) {
        if (!User.validateLogin(loginInput.getText())) {
            statusBarNotify("Логин должен быть > 3 символов");
            return;
        }
        if (!User.validatePassword(passwordInput.getText())) {
            statusBarNotify("Пароль должен быть как >= 6 символов");
            return;
        }
        authButton.setText("Подождите");
        authButton.setDisable(true);
        loginInput.setDisable(true);
        passwordInput.setDisable(true);

        User user = new User(loginInput.getText(), passwordInput.getText());
        RequestCommand requestCommand = new RequestCommand("ping", user);

        new Thread(() -> {
            Response response = client.send(requestCommand);

            Platform.runLater(() -> {
                authButton.setText("Авторизоваться");
                authButton.setDisable(false);
                loginInput.setDisable(false);
                passwordInput.setDisable(false);
                if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                    AuthManager.setCurrentUser(user);
                    statusBarNotify("Вы успешно авторизованы как \"" + user.login() + "\"");
                }
                else if (response.getResponseStatus().equals(ResponseStatus.LOGIN_UNLUCK)) {
                    statusBarNotify("Неверный логин или пароль");
                }
                else {
                    statusBarNotify(response.getResponseStatus() + ": " + response.getMessage());
                }
            });
        }).start();
    }

    @FXML
    private void registerButtonPressed(ActionEvent actionEvent) {
        if (!User.validateLogin(regLoginInput.getText())) {
            statusBarNotify("Логин должен быть > 3 символов");
            return;
        }
        if (!User.validatePassword(regPasswordInput.getText())) {
            statusBarNotify("Пароль должен быть как >= 6 символов");
            return;
        }
        if (!passwordAreSame()) {
            statusBarNotify("Пароли должны совпадать");
            return;
        }
        regButton.setText("Подождите...");
        regButton.setDisable(true);
        regLoginInput.setDisable(true);
        regPasswordInput.setDisable(true);
        regRepeatpasswordInput.setDisable(true);

        User user = new User(regLoginInput.getText(), regPasswordInput.getText());
        RequestCommand requestCommand = new RequestCommand("register", user);

        new Thread(() -> {
            Response response = client.send(requestCommand);

            Platform.runLater(() -> {
                regButton.setText("Регистрация");
                regButton.setDisable(false);
                regLoginInput.setDisable(false);
                regPasswordInput.setDisable(false);
                regRepeatpasswordInput.setDisable(false);
                if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                    AuthManager.setCurrentUser(user);
                    statusBarNotify("Регистрация прошла успешно! Вы авторизованы как " + user.login());
                }
                else {
                    statusBarNotify(response.getResponseStatus() + ": " + response.getMessage());
                }
            });
        }).start();



    }

    @FXML
    private void gotoRegister(ActionEvent actionEvent) {
        mainTab.getSelectionModel().select(regTab);
    }

    @FXML
    private void gotoLogin(ActionEvent actionEvent) {
        mainTab.getSelectionModel().select(loginTab);
    }

    private boolean passwordAreSame() {
        return regPasswordInput.getText().equals(regRepeatpasswordInput.getText());
    }

    private void statusBarNotify(String s) {
        statusBar.setText(s);

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(3),
                actionEvent -> statusBar.setText("")
        ));
        timeline.setCycleCount(1);
        timeline.play();
    }

}
