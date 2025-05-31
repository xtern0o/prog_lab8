package org.example.client.gui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import org.example.client.managers.AuthManager;
import org.example.client.managers.Client;
import org.example.client.utils.AppLocale;
import org.example.client.utils.ClientSingleton;
import org.example.client.utils.DialogHandler;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Coordinates;
import org.example.common.entity.Country;
import org.example.common.entity.Person;
import org.example.common.entity.Ticket;
import org.example.common.entity.TicketType;

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Predicate;

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

    private final Predicate<String> isNameValid = name -> name != null && !name.isBlank();

    private final Predicate<String> isPriceValid = priceStr -> {
        try {
            double price = Double.parseDouble(priceStr);
            return price > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    };

    private final Predicate<String> isHeightValid = heightStr -> {
        try {
            long height = Long.parseLong(heightStr);
            return height > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    };

    private final Predicate<String> isXValid = xStr -> {
        try {
            Float.parseFloat(xStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    };

    private final Predicate<String> isYValid = yStr -> {
        try {
            int y = Integer.parseInt(yStr);
            return y > -471; // Исправлено на > -471
        } catch (NumberFormatException e) {
            return false;
        }
    };

    public void setTicket(Ticket t) {
        this.ticket = t;
        fillFields();
        validateAllFields();
        updateSmiley();
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
            if (ticket != null) {
                ticket.setDiscount(newVal.floatValue());
            }
        });
        discountValLabel.setText(String.format("%.2f", discountSlider.getValue()));

        validControl.put(nameField, false);
        validControl.put(priceField, false);
        validControl.put(pHeightField, false);
        validControl.put(xField, false);
        validControl.put(yField, false);
        validControl.put(refundableField, true);
        validControl.put(typeCombo, true);
        validControl.put(pNationField, true);

        setLocalization();

        fillFields();
    }

    private void setLocalization() {
        saveButton.setText(AppLocale.getString("Save"));
        cancelButton.setText(AppLocale.getString("Cancel"));
    }

    @FXML
    private void cancel() {
        mainCallback.run();
    }

    @FXML
    private void save() {
        if (!validateAllFields()) {
            DialogHandler.errorAlert(AppLocale.getString("Error"), AppLocale.getString("ValidationError"), "Одно или несколько полей невалидны");
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
                        DialogHandler.errorAlert(AppLocale.getString("Success"), AppLocale.getString("ErrorWhileUpdate", response.getResponseStatus().toString()), response.getMessage());
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
                        DialogHandler.successAlert(AppLocale.getString("Success"), AppLocale.getString("ObjectCreation"), response.getMessage());
                        System.out.println(ticket);
                        mainCallback.run();
                    });
                } else {
                    Platform.runLater(() -> {
                        DialogHandler.errorAlert(AppLocale.getString("Unluck"), AppLocale.getString("ErrorWhileCreating", response.getResponseStatus().toString()), response.getMessage());
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

            if (ticket.getPerson() != null) {
                pHeightField.setText(String.valueOf(ticket.getPerson().getHeight()));
                if (ticket.getPerson().getNationality() != null) {
                    pNationField.setValue(ticket.getPerson().getNationality().name());
                }
            }

            if (ticket.getCoordinates() != null) {
                xField.setText(String.valueOf(ticket.getCoordinates().getX()));
                yField.setText(String.valueOf(ticket.getCoordinates().getY()));
            }

            refundableField.setSelected(ticket.isRefundable());

            if (ticket.getType() != null) {
                typeCombo.setValue(ticket.getType().name());
            }

            idField.setText(String.valueOf(ticket.getId()));
            ownerField.setText(ticket.getOwnerLogin());
            creationDateField.setText(ticket.getCreationDate().format(DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(AppLocale.getCurrentLocale())));

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
            ticket.setCreationDate(ZonedDateTime.now());
            ticket.setCoordinates(new Coordinates());
            ticket.setPerson(new Person());
            ticket.setRefundable(false);
            ticket.setDiscount((float) discountSlider.getValue());

            creationDateField.setText(ZonedDateTime.now().format(DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(AppLocale.getCurrentLocale())));
            ownerField.setText(AuthManager.getCurrentUser().login());

            nameField.setText("");
            priceField.setText("");
            pHeightField.setText("");
            xField.setText("");
            yField.setText("");
            refundableField.setSelected(false);
            typeCombo.setValue("");
            pNationField.setValue("");

            resetValidation();
        }

        updateSmiley();
    }

    private void resetValidation() {
        validControl.put(nameField, false);
        validControl.put(priceField, false);
        validControl.put(pHeightField, false);
        validControl.put(xField, false);
        validControl.put(yField, false);

        nameField.setStyle("-fx-border-color: red; -fx-border-width: 2px");
        priceField.setStyle("-fx-border-color: red; -fx-border-width: 2px");
        pHeightField.setStyle("-fx-border-color: red; -fx-border-width: 2px");
        xField.setStyle("-fx-border-color: red; -fx-border-width: 2px");
        yField.setStyle("-fx-border-color: red; -fx-border-width: 2px");
    }

    private boolean validateAllFields() {
        validateField(nameField, isNameValid);
        validateField(priceField, isPriceValid);
        validateField(pHeightField, isHeightValid);
        validateField(xField, isXValid);
        validateField(yField, isYValid);

        boolean allValid = true;
        for (Boolean valid : validControl.values()) {
            if (!valid) {
                allValid = false;
                break;
            }
        }

        updateSmiley();
        return allValid;
    }

    private void validateField(TextField field, Predicate<String> validationPredicate) {
        String value = field.getText();
        boolean isValid = validationPredicate.test(value);

        validControl.put(field, isValid);
        field.setStyle(isValid ? "" : "-fx-border-color: red; -fx-border-width: 2px");
    }

    private void updateSmiley() {
        boolean allValid = validControl.values().stream().allMatch(valid -> valid);

        String imagePath = allValid ? "/gui/image/smiley_ok.png" : "/gui/image/smiley_bad.png";
        smiley.setImage(new Image(getClass().getResource(imagePath).toExternalForm()));
    }

    private void evilActivate(Control control) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px");
        validControl.put(control, false);
        updateSmiley();
    }

    private void okActivate(Control control) {
        control.setStyle("");
        validControl.put(control, true);
        updateSmiley();
    }

    public void nameChanged() {
        String name = nameField.getText();
        if (ticket != null) {
            ticket.setName(name);
        }

        validateField(nameField, isNameValid);
        updateSmiley();
    }

    public void priceChanged() {
        String priceStr = priceField.getText();
        if (isPriceValid.test(priceStr) && ticket != null) {
            ticket.setPrice(Double.parseDouble(priceStr));
        }

        validateField(priceField, isPriceValid);
        updateSmiley();
    }

    public void pHeightChanged() {
        String heightStr = pHeightField.getText();
        if (isHeightValid.test(heightStr) && ticket != null && ticket.getPerson() != null) {
            ticket.getPerson().setHeight(Long.parseLong(heightStr));
        }

        validateField(pHeightField, isHeightValid);
        updateSmiley();
    }

    public void refundableChanged() {
        if (ticket != null) {
            ticket.setRefundable(refundableField.isSelected());
        }
    }

    public void typeChanged() {
        if (ticket != null) {
            if (typeCombo.getValue() == null || typeCombo.getValue().isEmpty()) {
                ticket.setType(null);
            } else {
                ticket.setType(TicketType.valueOf(typeCombo.getValue()));
            }
        }
    }

    public void pNationChanged() {
        if (ticket != null && ticket.getPerson() != null) {
            if (pNationField.getValue() == null || pNationField.getValue().isEmpty()) {
                ticket.getPerson().setNationality(null);
            } else {
                ticket.getPerson().setNationality(Country.valueOf(pNationField.getValue()));
            }
        }
    }

    public void xChanged() {
        String xStr = xField.getText();
        if (isXValid.test(xStr) && ticket != null && ticket.getCoordinates() != null) {
            ticket.getCoordinates().setX(Float.parseFloat(xStr));
        }

        validateField(xField, isXValid);
        updateSmiley();
    }

    public void yChanged() {
        String yStr = yField.getText();
        if (isYValid.test(yStr) && ticket != null && ticket.getCoordinates() != null) {
            ticket.getCoordinates().setY(Integer.parseInt(yStr));
        }

        validateField(yField, isYValid);
        updateSmiley();
    }
}