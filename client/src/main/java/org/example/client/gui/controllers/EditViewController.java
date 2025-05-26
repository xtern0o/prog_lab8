package org.example.client.gui.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.example.client.managers.AuthManager;
import org.example.client.managers.Client;
import org.example.client.utils.ClientSingleton;
import org.example.client.utils.DialogHandler;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Country;
import org.example.common.entity.Ticket;
import org.example.common.entity.TicketType;

import javax.imageio.plugins.jpeg.JPEGImageReadParam;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EditViewController implements Initializable {
    enum EditMode {
        ADD,
        UPDATE
    }

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private Slider discountSlider;
    @FXML private Label discountValLabel;
    @FXML private TextField pHeightField;
    @FXML private TextField xField;
    @FXML private TextField yField;
    @FXML private CheckBox refundableField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> pNationField;
    @FXML private TextField idField;
    @FXML private TextField ownerField;
    @FXML private TextField creationDateField;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    @FXML private ImageView smiley;
    // я так заебался это писать

    Client client = ClientSingleton.getClient();

    @Getter
    @Setter
    private Runnable mainCallback;

    @Getter
    private Ticket ticket;

    @Getter
    @Setter
    private String operationType;

    @Getter
    private EditMode mode;

    HashMap<Control, Boolean> validControl = new HashMap<>();

    public void setTicket(Ticket t) {
        this.ticket = t;
        fillFields();

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        typeCombo.getItems().clear();
        for (TicketType type : TicketType.values()) {
            typeCombo.getItems().add(type.name());
        }
        typeCombo.getItems().add("");
        pNationField.getItems().clear();
        for (Country country : Country.values()) {
            pNationField.getItems().add(country.name());
        }
        pNationField.getItems().add("");

        discountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            discountValLabel.setText(String.format("%.2f", newVal.floatValue()));
            ticket.setDiscount(newVal.floatValue());
        });
        discountValLabel.setText(String.format("%.2f", discountSlider.getValue()));

        validControl.put(nameField, true);
        validControl.put(priceField, true);
        validControl.put(pHeightField, true);
        validControl.put(xField, true);
        validControl.put(yField, true);
        validControl.put(refundableField, true);
        validControl.put(typeCombo, true);
        validControl.put(pNationField, true);

        fillFields();

    }
    @FXML
    private void cancel() {
        mainCallback.run();
    }

    @FXML
    private void save() {
        if (!ticket.validate()) {
            DialogHandler.errorAlert("Ошибка", "Ошибка валидации", "Одно или несколько полей невалидны");
            return;
        }
        new Thread(() -> {
            if (mode.equals(EditMode.UPDATE)) {
                RequestCommand requestCommand = new RequestCommand(
                        "update",
                        new ArrayList<>(List.of(String.valueOf(ticket.getId()))),
                        ticket,
                        AuthManager.getCurrentUser()
                );
                Response response = client.send(requestCommand);
                if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                    Platform.runLater(() -> {
                        DialogHandler.successAlert("Успех", "Обновление объекта", response.getMessage());
                        System.out.println(ticket);
                        mainCallback.run();
                    });
                } else {
                    Platform.runLater(() -> {
                        DialogHandler.errorAlert("Неудача", "Произошла ошибка при обновлении: " + response.getResponseStatus().toString(), response.getMessage());
                    });
                }
            }
            else {
                RequestCommand requestCommand = new RequestCommand(
                        "add",
                        ticket,
                        AuthManager.getCurrentUser()
                );
                Response response = client.send(requestCommand);
                if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                    Platform.runLater(() -> {
                        DialogHandler.successAlert("Успех", "Создание объекта", response.getMessage());
                        System.out.println(ticket);
                        mainCallback.run();
                    });
                } else {
                    Platform.runLater(() -> {
                        DialogHandler.errorAlert("Неудача", "Произошла ошибка при создании: " + response.getResponseStatus().toString(), response.getMessage());
                    });
                }
            }
        }).start();

    }

    private void fillFields() {
        if (ticket != null) {
            mode = EditMode.UPDATE;

            nameField.setText(ticket.getName());
            priceField.setText(String.valueOf(ticket.getPrice()));
            discountSlider.setValue(ticket.getDiscount());
            pHeightField.setText(String.valueOf(ticket.getPerson().getHeight()));
            xField.setText(String.valueOf(ticket.getCoordinates().getX()));
            yField.setText(String.valueOf(ticket.getCoordinates().getY()));
            refundableField.setSelected(ticket.isRefundable());
            if (ticket.getType() != null) {
                typeCombo.setValue(ticket.getType().name());
            }
            if (ticket.getPerson() != null && ticket.getPerson().getNationality() != null) {
                pNationField.setValue(ticket.getPerson().getNationality().name());
            }
            idField.setText(String.valueOf(ticket.getId()));
            ownerField.setText(ticket.getOwnerLogin());
            creationDateField.setText(ticket.getCreationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            if (!ticket.getOwnerLogin().equals(AuthManager.getCurrentUser().login())) {
                nameField.setDisable(true);
                priceField.setDisable(true);
                discountSlider.setDisable(true);
                pHeightField.setDisable(true);
                xField.setDisable(true);
                yField.setDisable(true);
                refundableField.setDisable(true);
                typeCombo.setDisable(true);
                pNationField.setDisable(true);
                saveButton.setDisable(true);
            }
        }
        else {
            mode = EditMode.ADD;
            this.ticket = new Ticket();
            ticket.setOwnerLogin(AuthManager.getCurrentUser().login());
            creationDateField.setText(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            ownerField.setText(AuthManager.getCurrentUser().login());
            validControl.put(nameField, false);
            validControl.put(priceField, false);
            validControl.put(pHeightField, false);
            validControl.put(xField, false);
            validControl.put(yField, false);

        }
    }

    private void evilActivate(Control control) {
        smiley.setImage(new Image(getClass().getResource("/gui/image/smiley_bad.png").toExternalForm()));
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px");
        validControl.put(control, false);

    }

    private void okActivate(Control control) {
        validControl.put(control, true);
        if (ticket.validate() && validControl.values().stream().allMatch(x -> x)) smiley.setImage(new Image(getClass().getResource("/gui/image/smiley_ok.png").toExternalForm()));
        control.setStyle("");
    }

    public void nameChanged() {
        String name = nameField.getText();
        ticket.setName(name);
        if (name == null || name.isBlank()) evilActivate(nameField);
        else {
            okActivate(nameField);

        }
    }

    public void priceChanged() {
        try {
            double price = Double.parseDouble(priceField.getText());
            ticket.setPrice(price);
            if (price <= 0) evilActivate(priceField);
            else {
                okActivate(priceField);
            }
        } catch (NumberFormatException e) {
            evilActivate(priceField);
        }
    }

    public void pHeightChanged() {
        try {
            long height = Long.parseLong(pHeightField.getText());
            ticket.getPerson().setHeight(height);
            if (height <= 0) evilActivate(pHeightField);
            else {
                okActivate(pHeightField);
            }
        } catch (NumberFormatException e) {
            evilActivate(pHeightField);
        }
    }

    public void refundableChanged() {
        ticket.setRefundable(refundableField.isSelected());
    }

    public void typeChanged() {
        if (typeCombo.getValue().isEmpty()) {
            ticket.setType(null);
            return;
        }
        ticket.setType(TicketType.valueOf(typeCombo.getValue()));
    }

    public void pNationChanged() {
        if (pNationField.getValue().isEmpty()) {
            ticket.getPerson().setNationality(null);
            return;
        }

        ticket.getPerson().setNationality(Country.valueOf(pNationField.getValue()));
    }

    public void xChanged() {
        try {
            float x = Float.parseFloat(xField.getText());
            ticket.getCoordinates().setX(x);
            okActivate(xField);
        } catch (NumberFormatException e) {
            evilActivate(xField);
        }
    }

    public void yChanged() {
        try {
            int y = Integer.parseInt(yField.getText());
            ticket.getCoordinates().setY(y);
            if (y <= -417) evilActivate(yField);
            else {
                okActivate(yField);
            }
        } catch (NumberFormatException e) {
            evilActivate(yField);
        }
    }


}
