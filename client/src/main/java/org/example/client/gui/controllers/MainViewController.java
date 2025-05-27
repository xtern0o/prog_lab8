package org.example.client.gui.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.example.client.managers.AuthManager;
import org.example.client.managers.Client;
import org.example.client.utils.ClientSingleton;
import org.example.client.utils.DialogHandler;
import org.example.client.utils.RectCoords;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Country;
import org.example.common.entity.Ticket;
import org.example.common.entity.TicketType;
import org.w3c.dom.css.Rect;

import java.net.URL;
import java.sql.SQLOutput;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    @FXML private Canvas canvas;

    @Getter
    private ConcurrentHashMap<String, Color> userToColor = new ConcurrentHashMap<>();
    private final static long MIN_OBJECT_HEIGHT = 20;
    private final static long MAX_OBJECT_HEIGHT = 200;
    private volatile ConcurrentHashMap<RectCoords, Ticket> canvasCoordsMap = new ConcurrentHashMap<>();
    private final Map<RectCoords, Double> rectAlphaMap = new ConcurrentHashMap<>();
    private Timeline appearTimeline;

    @Getter
    @Setter
    private Image bgImage;

    private volatile ObservableList<Ticket> ticketsObserveCollection = FXCollections.observableArrayList();

    private Client client = ClientSingleton.getClient();

    private volatile Integer highlightedTicketId = null;
    private volatile Timeline highlightTimeline;

    @Getter
    @Setter
    private Runnable authCallback;

    @Getter
    @Setter
    private Consumer<Ticket> editCallback;

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

        bgImage = new Image(Objects.requireNonNull(getClass().getResource("/gui/image/world.jpeg")).toExternalForm());

        System.out.println(bgImage.isError());
        System.out.println(bgImage.getUrl());
        System.out.println(bgImage.getException().toString());

        initTableColumns();

        synchronizeCollection();

        System.out.println(bgImage.getUrl());

        canvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    double x = mouseEvent.getX();
                    double y = mouseEvent.getY();
                    for (RectCoords coordKeys : canvasCoordsMap.keySet()) {
                        if (coordKeys.isInside(x, y)) {
                            doubleClickEdit(canvasCoordsMap.get(coordKeys));
                        }
                    }
                }
            }
        });
    }

    private static void drawGrid(GraphicsContext gc, double width, double height, double cellSize) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);

        for (double x = 0; x <= width; x += cellSize) {
            gc.strokeLine(x, 0, x, height);
        }

        for (double y = 0; y <= height; y += cellSize) {
            gc.strokeLine(0, y, width, y);
        }
    }

    private void redrawCanvas() {
        updateUsersColors();

        Platform.runLater(() -> {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            drawGrid(gc, canvas.getWidth(), canvas.getHeight(), 100);

            if (ticketsObserveCollection.isEmpty()) return;

            canvasCoordsMap.clear();
            Map<RectCoords, Ticket> newRects = new HashMap<>();

            for (Ticket ticket : ticketsObserveCollection) {
                float canvas_x = Math.abs(ticket.getCoordinates().getX() % (float) canvas.getWidth());
                int canvas_y = Math.abs(ticket.getCoordinates().getY() % (int) canvas.getHeight());

                long height = ticket.getPerson().getHeight();
                int width = 20;

                if (height > MAX_OBJECT_HEIGHT) height = MAX_OBJECT_HEIGHT;
                if (height < MIN_OBJECT_HEIGHT) height = MIN_OBJECT_HEIGHT;

                RectCoords coords = new RectCoords(
                        canvas_x, canvas_y,
                        canvas_x + width, canvas_y + height
                );

                newRects.put(coords, ticket);
                canvasCoordsMap.put(coords, ticket);

                rectAlphaMap.put(coords, 0.0);
            }

            // 2. Удалить исчезнувшие прямоугольники из alphaMap (чтобы не росла Map)
            rectAlphaMap.keySet().removeIf(key -> !canvasCoordsMap.containsKey(key));

            // 3. Запускаем анимацию
            if (appearTimeline != null) appearTimeline.stop();

            appearTimeline = new Timeline();
            appearTimeline.setCycleCount(Timeline.INDEFINITE);

            final double durationMs = 1000.0;
            final double step = 1.0 / (durationMs / 16);
            appearTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(16), e -> {
                boolean anyAnimating = false;
                for (RectCoords coords : canvasCoordsMap.keySet()) {
                    double alpha = rectAlphaMap.getOrDefault(coords, 1.0);
                    if (alpha < 1.0) {
                        alpha = Math.min(1.0, alpha + step);
                        rectAlphaMap.put(coords, alpha);
                        anyAnimating = true;
                    }
                }
                drawAllRects(gc);
                if (!anyAnimating) {
                    appearTimeline.stop();
                }
            }));

            appearTimeline.playFromStart();
        });
    }

    private void drawAllRects(GraphicsContext gc) {
        gc.setFill(Color.DARKGREY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (Map.Entry<RectCoords, Ticket> entry : canvasCoordsMap.entrySet()) {
            RectCoords coords = entry.getKey();
            Ticket ticket = entry.getValue();

            Color ticketColor = userToColor.get(ticket.getOwnerLogin());
            if (ticketColor == null) ticketColor = Color.RED;
            double alpha = rectAlphaMap.getOrDefault(coords, 1.0);

            double x = coords.x1();
            double y = coords.y1();
            double w = coords.x2() - coords.x1();
            double h = coords.y2() - coords.y1();

            double scale = alpha;
            double currW = w * scale;
            double currH = h * scale;

            double currX = x;
            double currY = y;

            Color colorWithAlpha = new Color(
                    ticketColor.getRed(),
                    ticketColor.getGreen(),
                    ticketColor.getBlue(),
                    alpha
            );
            gc.setFill(colorWithAlpha);
            gc.fillRect(currX, currY, currW, currH);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(currX, currY, currW, currH);
        }
    }

    /**
     * Обновляет коллекцию в соответствии с новыми значениями
     * @param newCollection
     */
    private void updateTableData(Collection<Ticket> newCollection) {
        Platform.runLater(() -> {
            boolean redraw = false;
            if (isCollectionChanged(newCollection, ticketsObserveCollection)) redraw = true;

            ticketsObserveCollection.clear();
            ticketsObserveCollection.addAll(newCollection);

            tableView.refresh();
            if (redraw) redrawCanvas();
            statusBarNotify("OK", "Получена актуальная коллекция с сервера. Всего элементов: " + newCollection.size());
        });
    }

    private void initTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        refundableColumn.setCellValueFactory(new PropertyValueFactory<>("refundable"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        owner_loginColumn.setCellValueFactory(new PropertyValueFactory<>("ownerLogin"));


        priceColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());

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

        tableView.setRowFactory(tableView -> {
            TableRow<Ticket> row = new TableRow<Ticket>() {
                @Override
                protected void updateItem(Ticket ticket, boolean empty) {
                    super.updateItem(ticket, empty);

                    if (empty || ticket == null) {
                        setStyle("");
                    } else if (ticket.getId() != null && ticket.getId().equals(highlightedTicketId)) {
                        setStyle(
                                "-fx-background-color: #d6ccc2;"
                        );
                    } else {
                        setStyle("");
                    }
                }
            };

            row.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2 && ! row.isEmpty()) {
                    doubleClickEdit(row.getItem());
                }
            });
            return row;
        });

    }

    private void doubleClickEdit(Ticket ticket) {
        editCallback.accept(ticket);
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

    private static boolean isCollectionChanged(Collection<Ticket> newCollection, ObservableList<Ticket> oldCollection) {
        if (newCollection.size() != oldCollection.size()) return true;
        List<Integer> newIds = newCollection.stream().map(Ticket::getId).sorted().toList();
        List<Integer> oldIds = oldCollection.stream().map(Ticket::getId).sorted().toList();
        return !newIds.equals(oldIds);
    }

    private void updateUsersColors() {
        userToColor.clear();
        Set<String> users = ticketsObserveCollection.stream().map(Ticket::getOwnerLogin).collect(Collectors.toSet());
        System.out.println("users: " + users.toString());
        int n = users.size();
        int i = 0;
        for (String user : users) {
            double hue = (double) i / n * 360;
            Color color = Color.hsb(hue, 1.0, 1.0);
            userToColor.put(user, color);
            i++;
            System.out.println(hue);
        }
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
                if (response.getCollection() != null) {
                    Ticket head = response.getCollection().stream().findFirst().get();
                    highlightRow(head.getId());
                }

                Platform.runLater(() -> {
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

    private void highlightRow(Integer ticketId) {
        if (highlightTimeline != null) {
            highlightTimeline.stop();
        }

        Platform.runLater(() -> {
            highlightedTicketId = ticketId;
            tableView.refresh();
            for (Ticket ticket : ticketsObserveCollection) {
                if (ticket.getId().equals(ticketId)) {
                    tableView.scrollTo(ticket);
                    break;
                }
            }

            highlightTimeline = new Timeline(new KeyFrame(
                    Duration.seconds(3),
                    event -> {
                        highlightedTicketId = null;
                        tableView.refresh();
                    }
            ));
            highlightTimeline.setCycleCount(1);
            highlightTimeline.play();
        });
    }

    @FXML
    private void printUniqueDiscountCommand() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("print_unique_discount", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            "Уникальные discount",
                            "Уникальные discount",
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
        editCallback.accept(null);
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
                synchronizeCollection();
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            "Удаление элемента",
                            "Результат выполнения удаления",
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
                synchronizeCollection();
                Platform.runLater(() -> {
                    DialogHandler.successAlert("Удаление по id", "id = " + id, response.getMessage());
                    statusBarNotify(response.getResponseStatus().toString(), response.getMessage());
                });
            }).start();
        } catch (NullPointerException ignored) {
        }

    }

}
