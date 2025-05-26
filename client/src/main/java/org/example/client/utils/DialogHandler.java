package org.example.client.utils;


import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Хэндлер со статическими методами генерации частоиспользуемых диалогов и алертов
 */
public class DialogHandler {
    public static void commandResponseAlert(String title, String headerText, String text) {
        Alert historyAlert = new Alert(Alert.AlertType.INFORMATION);
        historyAlert.setTitle(title);
        historyAlert.setHeaderText(headerText);
        Stage historyStage = (Stage) historyAlert.getDialogPane().getScene().getWindow();
        historyStage.setMinWidth(500);
        historyStage.setMinHeight(300);
        TextArea textArea = new TextArea(text);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setFont(Font.font("Fira Code", 14));

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setPrefWidth(1000);
        expContent.add(textArea, 0, 0);
        historyAlert.getDialogPane().setContent(expContent);
        historyAlert.setResizable(true);
        historyAlert.showAndWait();
    }

    /**
     * Показывает диалоговое окно с вопросом и кнопками "Да" и "Нет".
     * @param title Заголовок окна
     * @param text Вопрос пользователю
     * @return true если выбрано "Да", false если "Нет" или закрыто окно
     */
    public static boolean confirmationDialog(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);

        // Переименовываем кнопки на "Да" и "Нет"
        ButtonType yesButton = new ButtonType("Да");
        ButtonType noButton = new ButtonType("Нет");
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }

    public static void successAlert(String title, String headerText, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(text);
        alert.setHeaderText(headerText);
        alert.showAndWait();
    }

    public static Integer integerInputDialog(String title, String header, String promptText) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        TextField inputField = new TextField();
        inputField.setPromptText(promptText);
        inputField.setFont(Font.font("Fira Code", 14));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label(promptText), 0, 0);
        grid.add(inputField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            okButton.setDisable(!newVal.matches("-?\\d+"));
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    return Integer.parseInt(inputField.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Integer> result = dialog.showAndWait();
        return result.orElse(null);
    }
}
