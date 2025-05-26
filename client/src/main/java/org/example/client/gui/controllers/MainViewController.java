package org.example.client.gui.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainViewController implements Initializable {
    @FXML private Label usernameLabel;
    @FXML private Label statusCodeBar;
    @FXML private Label statusMessage;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;

    @FXML private TableView<Ticket> tableView;
    @FXML private TableColumn<Ticket, Integer> idColumn;
    @FXML private TableColumn<Ticket, String> nameColumn;
    @FXML private TableColumn<Ticket, Float> coord_xColumn;
    @FXML private TableColumn<Ticket, Double> coord_yColumn;
    @FXML private TableColumn<Ticket, String> creation_dateColumn;
    @FXML private TableColumn<Ticket, Double> priceColumn;
    @FXML private TableColumn<Ticket, Float> discountColumn;
    @FXML private TableColumn<Ticket, Boolean> refundableColumn;
    @FXML private TableColumn<Ticket, TicketType> typeColumn;
    @FXML private TableColumn<Ticket, Long> person_heightColumn;
    @FXML private TableColumn<Ticket, Country> person_nationalityColumn;
    @FXML private TableColumn<Ticket, String> owner_loginColumn;

    private volatile ObservableList<Ticket> ticketsObserveCollection = FXCollections.observableArrayList();

    private Client client = ClientSingleton.getClient();

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

        initTableColumns();

        synchronizeCollection();
    }

    /**
     * Обновляет коллекцию в соответствии с новыми значениями
     * @param tickets
     */
    private void updateTableData(Collection<Ticket> tickets) {
        Platform.runLater(() -> {
            ticketsObserveCollection.clear();
            ticketsObserveCollection.addAll(tickets);

            tableView.refresh();
            statusBarNotify("OK", "Получена актуальная коллекция с сервера. Всего элементов: " + tickets.size());

        });
    }

    private void initTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        refundableColumn.setCellValueFactory(new PropertyValueFactory<>("refundable"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        owner_loginColumn.setCellValueFactory(new PropertyValueFactory<>("ownerLogin"));

        // Для числовых типов с использованием соответствующих свойств и asObject()

        // Для price (double)
        priceColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());

        // Для discount (Float)
        discountColumn.setCellValueFactory(
                cellData -> new SimpleFloatProperty(
                        cellData.getValue().getDiscount()
                ).asObject());

        coord_xColumn.setCellValueFactory(
                cellData -> new SimpleFloatProperty(
                        cellData.getValue().getCoordinates().getX()
                ).asObject()
        );
        coord_yColumn.setCellValueFactory(
                cellData -> new SimpleDoubleProperty(
                        cellData.getValue().getCoordinates().getY()
                ).asObject()
        );
        creation_dateColumn.setCellValueFactory(
                cellData -> {
                    ZonedDateTime date = cellData.getValue().getCreationDate();
                    if (date != null) {
                        return new SimpleStringProperty(date.format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                        ));
                    }
                    return new SimpleStringProperty("");
                }
        );
        person_heightColumn.setCellValueFactory(
                cellData -> new SimpleLongProperty(
                        cellData.getValue().getPerson().getHeight()
                ).asObject()
        );
        person_nationalityColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getPerson().getNationality()
                )
        );

        tableView.setItems(ticketsObserveCollection);

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

    /**
     * Метод отправляющий запрос на сервер и получающий текущую коллекцию
     * С помощью команды show
     */
    @FXML
    private void synchronizeCollection() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("show", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Collection<Ticket> newCollection = response.getCollection();
                updateTableData(newCollection);
            }
        }).start();
    }

    @FXML
    private void getInfo() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("info", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            "Информация",
                            "Информация о коллекции на сервере",
                            response.getMessage()
                    );
                    statusBarNotify(response.getResponseStatus().toString(), "Ответ получен");

                });
            }
            else {
                Platform.runLater(() -> {
                    statusBarNotify(response.getResponseStatus().toString(), response.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void logout() {
        AuthManager.setCurrentUser(null);
        this.authCallback.run();
    }

    @FXML
    private void showHistory() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("history", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            "История",
                            "5 последних команд",
                            response.getMessage()
                    );
                    statusBarNotify(response.getResponseStatus().toString(), "Ответ получен");

                });
            }
            else {
                Platform.runLater(() -> {
                    statusBarNotify(response.getResponseStatus().toString(), response.getMessage());
                });
            }
        }).start();

    }

    @FXML
    private void getHead() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("head", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            "Первый элемент",
                            "Первый эемент коллекции",
                            response.getMessage()
                    );
                    statusBarNotify(response.getResponseStatus().toString(), "Ответ получен");

                });
            }
            else {
                Platform.runLater(() -> {
                    statusBarNotify(response.getResponseStatus().toString(), response.getMessage());
                });
            }
        }).start();
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

    @FXML
    private void removeHeadCommand() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("remove_head", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            "Удаление элемента",
                            "Результат выполнения удаления",
                            response.getMessage()
                    );
                    synchronizeCollection();
                    statusBarNotify(response.getResponseStatus().toString(), "Ответ получен");


                });
            }
            else {
                Platform.runLater(() -> {
                    statusBarNotify(response.getResponseStatus().toString(), response.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void clearMyItemsCommand() {
        if (DialogHandler.confirmationDialog("Очистка коллекции", "Вы уверены, что хотите удалить все свои элементы в коллекции?")) {
            new Thread(() -> {
                RequestCommand requestCommand = new RequestCommand("clear", AuthManager.getCurrentUser());
                Response response = client.send(requestCommand);
                if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                    synchronizeCollection();
                    Platform.runLater(() -> {
                        DialogHandler.successAlert("Успешно", "Очистка коллекции", response.getMessage());
                    });
                }
            }).start();
        }
    }

    @FXML
    private void deleteByIdCommand() {
        try {
            int id = DialogHandler.integerInputDialog("Удаление", "Удаление по id", "Введите id элемента");
            new Thread(() -> {
                RequestCommand requestCommand = new RequestCommand(
                        "remove_by_id",
                        new ArrayList<>(List.of(String.valueOf(id))),
                        AuthManager.getCurrentUser()
                );
                Response response = client.send(requestCommand);
                Platform.runLater(() -> {
                    statusBarNotify(response.getResponseStatus().toString(), response.getMessage());
                });
            }).start();
        } catch (NullPointerException ignored) {
        }

    }

}
