package org.example.client.gui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import org.example.client.gui.filters.TableFilterManager;
import org.example.client.managers.AuthManager;
import org.example.client.utils.DialogHandler;
import org.example.common.entity.Country;
import org.example.common.entity.Ticket;
import org.example.common.entity.TicketType;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ResourceBundle;

public class FilterDialogController implements Initializable {

    @FXML private TextField nameStartsWith;
    @FXML private CheckBox nameCaseSensitive;
    @FXML private TextField ownerLoginEquals;
    @FXML private CheckBox showOnlyMine;

    @FXML private TextField idEquals;
    @FXML private TextField priceFrom;
    @FXML private TextField priceTo;
    @FXML private TextField discountFrom;
    @FXML private TextField discountTo;
    @FXML private TextField coordXFrom;
    @FXML private TextField coordXTo;
    @FXML private TextField coordYFrom;
    @FXML private TextField coordYTo;
    @FXML private TextField heightFrom;
    @FXML private TextField heightTo;

    @FXML private DatePicker creationDateFrom;
    @FXML private DatePicker creationDateTo;
    @FXML private ComboBox<TicketType> ticketTypeComboBox;
    @FXML private ComboBox<Boolean> refundableComboBox;
    @FXML private ComboBox<Country> nationalityComboBox;

    @FXML private Button resetButton;
    @FXML private Button applyButton;
    @FXML private Button cancelButton;

    @Setter
    private MainViewController mainViewController;

    @Setter
    private Stage dialogStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ticketTypeComboBox.setItems(FXCollections.observableArrayList(TicketType.values()));
        ticketTypeComboBox.getItems().add(0, null);

        nationalityComboBox.setItems(FXCollections.observableArrayList(Country.values()));
        nationalityComboBox.getItems().add(0, null);

        setupTextFieldValidation();

        nameStartsWith.textProperty().bindBidirectional(TableFilterManager.nameStartsWithProperty());
        ownerLoginEquals.textProperty().bindBidirectional(TableFilterManager.ownerLoginProperty());
        if (ownerLoginEquals.getText() != null) {
            if (ownerLoginEquals.getText().equals(AuthManager.getCurrentUser().login())) {
                showOnlyMine.setSelected(true);
                ownerLoginEquals.setDisable(true);
            }
        }

        if (TableFilterManager.getPriceFilterActive().get()) {
            if (TableFilterManager.getPriceMin() != null) priceFrom.setText(TableFilterManager.getPriceMin().toString());
            if (TableFilterManager.getPriceMax() != null) priceTo.setText(TableFilterManager.getPriceMax().toString());
        }
        if (TableFilterManager.getDiscountFilterActive().get()) {
            if (TableFilterManager.getDiscountMin() != null) discountFrom.setText(TableFilterManager.getDiscountMin().toString());
            if (TableFilterManager.getDiscountMax() != null) discountTo.setText(TableFilterManager.getDiscountMax().toString());
        }
        if (TableFilterManager.getCoordFilterActive().get()) {
            if (TableFilterManager.getCoordXMin() != null) coordXFrom.setText(TableFilterManager.getCoordXMin().toString());
            if (TableFilterManager.getCoordXMax() != null) coordXTo.setText(TableFilterManager.getCoordXMax().toString());
            if (TableFilterManager.getCoordYMin() != null) coordYFrom.setText(TableFilterManager.getCoordYMin().toString());
            if (TableFilterManager.getCoordYMax() != null) coordYTo.setText(TableFilterManager.getCoordYMax().toString());
        }
        if (TableFilterManager.getHeightFilterActive().get()) {
            if (TableFilterManager.getHeightMin() != null) heightFrom.setText(TableFilterManager.getHeightMin().toString());
            if (TableFilterManager.getHeightMax() != null) heightTo.setText(TableFilterManager.getHeightMax().toString());
        }
        if (TableFilterManager.getDateFilterActive().get()) {
            if (TableFilterManager.getDateFrom() != null) creationDateFrom.setValue(TableFilterManager.getDateFrom().toLocalDate());
            if (TableFilterManager.getDateTo() != null) creationDateTo.setValue(TableFilterManager.getDateTo().toLocalDate().minusDays(1));
        }
        if (TableFilterManager.getTypeFilterActive().get()) {
            ticketTypeComboBox.setValue(TableFilterManager.getTicketType());
        }
        if (TableFilterManager.getNationalityFilterActive().get()) {
            nationalityComboBox.setValue(TableFilterManager.getNationality());
        }
        if (TableFilterManager.getIdFilterActive().get()) {
            idEquals.setText(TableFilterManager.getIdEquals().toString());
        }

    }

    private void setupTextFieldValidation() {
        setupNumericValidation(idEquals, true);
        setupNumericValidation(priceFrom, false);
        setupNumericValidation(priceTo, false);
        setupNumericValidation(discountFrom, false);
        setupNumericValidation(discountTo, false);
        setupNumericValidation(coordXFrom, false);
        setupNumericValidation(coordXTo, false);
        setupNumericValidation(coordYFrom, false);
        setupNumericValidation(coordYTo, false);
        setupNumericValidation(heightFrom, true);
        setupNumericValidation(heightTo, true);
    }

    private void setupNumericValidation(TextField field, boolean integerOnly) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                return;
            }

            if (integerOnly) {
                if (!newValue.matches("\\d*")) {
                    field.setText(newValue.replaceAll("[^\\d]", ""));
                }
            } else {
                if (!newValue.matches("\\d*\\.?\\d*")) {
                    field.setText(oldValue);
                }
            }
        });
    }

    @FXML
    private void applyFilters() {
        if (mainViewController.getFilteredTickets() != null) {
            updateFilterManager();

            mainViewController.getFilteredTickets().setPredicate(null);
            mainViewController.getFilteredTickets().setPredicate(TableFilterManager.createPredicate());

            DialogHandler.successAlert("Установка фильтров", "Фильтр установлен", "Фильтры были успешно применены");

//            dialogStage.close();
        }
    }

    /**
     * Обновляет все фильтры в TableFilterManager текущими значениями из UI
     */
    private void updateFilterManager() {
        String startsWith = parseStringOrNull(nameStartsWith.getText());
        if (startsWith != null) {
            TableFilterManager.setNameFilterActive(!nameStartsWith.getText().isEmpty());
            System.out.println("NOT NULL NAME");
        } else {
            TableFilterManager.nameStartsWithProperty().set("");
            TableFilterManager.setNameFilterActive(false);
            System.out.println("NULL NAME");
        }
        if (showOnlyMine.isSelected()) {
            TableFilterManager.setOwnerLogin(AuthManager.getCurrentUser().login());
        } else {
            if (parseStringOrNull(ownerLoginEquals.getText()) != null) {
                TableFilterManager.setOwnerLogin(ownerLoginEquals.getText());
            } else {
                TableFilterManager.setOwnerLogin(null);
            }
        }

        Integer idValue = parseIntegerOrNull(idEquals.getText());
        TableFilterManager.setIdEquals(idValue);

        Double priceMinValue = parseDoubleOrNull(priceFrom.getText());
        Double priceMaxValue = parseDoubleOrNull(priceTo.getText());
        TableFilterManager.setPriceRange(priceMinValue, priceMaxValue);

        Float discountMinValue = parseFloatOrNull(discountFrom.getText());
        Float discountMaxValue = parseFloatOrNull(discountTo.getText());
        TableFilterManager.setDiscountRange(discountMinValue, discountMaxValue);

        Float coordXMinValue = parseFloatOrNull(coordXFrom.getText());
        Float coordXMaxValue = parseFloatOrNull(coordXTo.getText());
        TableFilterManager.setCoordXRange(coordXMinValue, coordXMaxValue);

        Double coordYMinValue = parseDoubleOrNull(coordYFrom.getText());
        Double coordYMaxValue = parseDoubleOrNull(coordYTo.getText());
        TableFilterManager.setCoordYRange(coordYMinValue, coordYMaxValue);

        Long heightMinValue = parseLongOrNull(heightFrom.getText());
        Long heightMaxValue = parseLongOrNull(heightTo.getText());
        TableFilterManager.setHeightRange(heightMinValue, heightMaxValue);

        ZonedDateTime dateFromValue = null;
        if (creationDateFrom.getValue() != null) {
            dateFromValue = creationDateFrom.getValue().atStartOfDay(ZoneId.systemDefault());
        }

        ZonedDateTime dateToValue = null;
        if (creationDateTo.getValue() != null) {
            dateToValue = creationDateTo.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault());
        }

        TableFilterManager.setDateRange(dateFromValue, dateToValue);

        TableFilterManager.setTicketType(ticketTypeComboBox.getValue());
        TableFilterManager.setNationality(nationalityComboBox.getValue());


    }

    private String parseStringOrNull(String text) {
        if (text == null || text.isEmpty()) return null;
        return text;
    }

    private Integer parseIntegerOrNull(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double parseDoubleOrNull(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Float parseFloatOrNull(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseLongOrNull(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @FXML
    private void showMineAction() {
        if (showOnlyMine.isSelected()) {
            ownerLoginEquals.setDisable(true);
            TableFilterManager.setOwnerLogin(AuthManager.getCurrentUser().login());
            return;
        }
        ownerLoginEquals.setDisable(false);
    }

    @FXML
    private void resetFilters() {
        nameStartsWith.clear();
        nameCaseSensitive.setSelected(false);
        ownerLoginEquals.clear();
        showOnlyMine.setSelected(false);

        idEquals.clear();
        priceFrom.clear();
        priceTo.clear();
        discountFrom.clear();
        discountTo.clear();
        coordXFrom.clear();
        coordXTo.clear();
        coordYFrom.clear();
        coordYTo.clear();
        heightFrom.clear();
        heightTo.clear();

        idEquals.clear();

        creationDateFrom.setValue(null);
        creationDateTo.setValue(null);
        ticketTypeComboBox.setValue(null);
        nationalityComboBox.setValue(null);

        TableFilterManager.resetAllFilters();

        if (mainViewController.getFilteredTickets() != null) {
            mainViewController.getFilteredTickets().setPredicate(ticket -> true);
            DialogHandler.successAlert("Сброс фильтров", "Фильтры сброшены", "Фильтры были успешно сброшены");
        }

    }

    @FXML
    private void closeDialog() {
        dialogStage.close();
    }
}