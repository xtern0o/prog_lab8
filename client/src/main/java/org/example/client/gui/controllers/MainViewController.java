package org.example.client.gui.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.example.client.cli.ConsoleInput;
import org.example.client.cli.ConsoleOutput;
import org.example.client.gui.filters.TableFilterManager;
import org.example.client.managers.*;
import org.example.client.utils.*;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Country;
import org.example.common.entity.Ticket;
import org.example.common.entity.TicketType;
import org.example.common.utils.Printable;
import org.w3c.dom.css.Rect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.SQLOutput;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MainViewController implements Initializable {
    @Setter
    @Getter
    private Stage stage;

    @FXML public MenuItem deleteById;
    @FXML public MenuItem clearMyItems;
    @FXML public MenuItem removeHead;
    @FXML MenuButton deleteMenu;
    @FXML MenuItem updateColelctionButton;
    @FXML MenuItem serverInfoButton;
    @FXML MenuItem logoutButton;
    @FXML private Label usernameLabel;
    @FXML private Label statusCodeBar;
    @FXML private Label statusMessage;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private Label currentUser;
    @FXML private Label nameFilter;
    @FXML private Menu paramsMenu;
    @FXML private Menu commandsMenu;
    @FXML private Menu fileMenu;
    @FXML private Button addElementButton;


    @FXML private RadioMenuItem ru;
    @FXML private RadioMenuItem cz;
    @FXML private RadioMenuItem bg;
    @FXML private RadioMenuItem esgt;

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
    @FXML private Button filterButton;

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
    @Getter
    private FilteredList<Ticket> filteredTickets;


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

        initializeLocalization();

        initTableColumns();

        synchronizeCollection();

        AppLocale.currentLocaleProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(this::updateUILocalization);
        });

        filteredTickets.addListener(new ListChangeListener<Ticket>() {
            @Override
            public void onChanged(Change<? extends Ticket> c) {
                redrawCanvas();
            }
        });

        updateUILocalization();


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

        canvas.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double x = event.getX();
                double y = event.getY();
                for (RectCoords coords : canvasCoordsMap.keySet()) {
                    if (coords.isInside(x, y)) {
                        statusBarNotify("CHOSEN", "Выбран элемент с id=" + canvasCoordsMap.get(coords).getId());
                    }
                }
            }
        });

        client.addCollectionUpdateListener((newCollection) -> {
            Platform.runLater(this::synchronizeCollection);
        });

    }

    /**
     * Открывает диалоговое окно с расширенными фильтрами
     */
    @FXML
    public void gotoFilters() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/gui/FilterDialog.fxml"));
            AnchorPane page = (AnchorPane) loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(AppLocale.getString("FilterTitle", "Расширенная фильтрация"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(stage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            dialogStage.setResizable(true);

            FilterDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setMainViewController(this);

            dialogStage.showAndWait();

            redrawCanvas();

        } catch (IOException e) {
            e.printStackTrace();
            DialogHandler.errorAlert(
                    AppLocale.getString("ErrorTitle", "Ошибка"),
                    AppLocale.getString("FilterDialogError", "Ошибка при открытии диалога фильтрации"),
                    e.getMessage()
            );
        }
    }

    private void initializeLocalization() {
        Locale currentLocale = AppLocale.getCurrentLocale();
        switch (currentLocale.getLanguage()) {
            case "ru" -> ru.setSelected(true);
            case "cs" -> cz.setSelected(true);
            case "bg" -> bg.setSelected(true);
            case "es" -> esgt.setSelected(true);
        }
    }

    private void updateUILocalization() {
        fileMenu.setText(AppLocale.getString("FileMenu"));
        updateColelctionButton.setText(AppLocale.getString("UpdateCollection"));
        serverInfoButton.setText(AppLocale.getString("ServerInfo"));
        logoutButton.setText(AppLocale.getString("Logout"));

        commandsMenu.setText(AppLocale.getString("CommandsMenu"));
        paramsMenu.setText(AppLocale.getString("ParamsMenu"));

        updateColelctionButton.setText(AppLocale.getString("UpdateCollection"));
        serverInfoButton.setText(AppLocale.getString("ServerInfo"));
        logoutButton.setText(AppLocale.getString("Logout"));

        currentUser.setText(AppLocale.getString("CurrentUser"));

        ru.setText(AppLocale.getString("LangRu"));
        bg.setText(AppLocale.getString("LangBg"));
        cz.setText(AppLocale.getString("LangCz"));
        esgt.setText(AppLocale.getString("LangEsGt"));

        addElementButton.setText(AppLocale.getString("AddElement"));
        deleteMenu.setText(AppLocale.getString("DeleteMenu"));

        deleteById.setText(AppLocale.getString("DeleteById"));
        clearMyItems.setText(AppLocale.getString("ClearMyItems"));
        removeHead.setText(AppLocale.getString("RemoveHead"));

        filterButton.setText(AppLocale.getString("FilterDialogTitle"));

//        updateTableData(ticketsObserveCollection);

        tableView.refresh();

    }

    @FXML
    private void setRuLang() {
        try {
            AppLocale.setLocale(new Locale("ru"));
        } catch (Exception e) {
            System.err.println("Ошибка при установке русского языка: " + e.getMessage());
        }
    }

    @FXML
    private void setCzLang() {
        try {
            AppLocale.setLocale(new Locale("cs"));
        } catch (Exception e) {
            System.err.println("Ошибка при установке чешского языка: " + e.getMessage());
        }
    }

    @FXML
    private void setBgLang() {
        try {
            AppLocale.setLocale(new Locale("bg"));
        } catch (Exception e) {
            System.err.println("Ошибка при установке болгарского языка: " + e.getMessage());
        }
    }

    @FXML
    private void setEsgtLang() {
        try {
            AppLocale.setLocale(new Locale("es", "GT"));
        } catch (Exception e) {
            System.err.println("Ошибка при установке испанского языка: " + e.getMessage());
        }
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

//            if (filteredTickets.isEmpty()) return;

            canvasCoordsMap.clear();
            Map<RectCoords, Ticket> newRects = new HashMap<>();

            for (Ticket ticket : filteredTickets) {
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

            rectAlphaMap.keySet().removeIf(key -> !canvasCoordsMap.containsKey(key));

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

        drawGrid(gc, canvas.getWidth(), canvas.getHeight(), 100);

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
            boolean collectionChanged = isCollectionChanged(newCollection, ticketsObserveCollection);

            if (collectionChanged) {
                ticketsObserveCollection.clear();
                ticketsObserveCollection.addAll(newCollection);

                tableView.refresh();
                redrawCanvas();
                statusBarNotify("OK", AppLocale.getString("RefreshSuccess", newCollection.size()));
            }
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
                                DateTimeFormatter
                                        .ofLocalizedDate(FormatStyle.LONG)
                                        .withLocale(AppLocale.getCurrentLocale())
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

        filteredTickets = new FilteredList<>(ticketsObserveCollection, TableFilterManager.createPredicate());
        SortedList<Ticket> sortedTickets = new SortedList<>(filteredTickets);
        sortedTickets.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedTickets);

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
        statusLabel.setText(AppLocale.getString("Status"));
        messageLabel.setText(AppLocale.getString("Message"));

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


    private static boolean isCollectionChanged(Collection<Ticket> newCollection,
                                               ObservableList<Ticket> oldCollection) {
        if (newCollection.size() != oldCollection.size()) {
            return true;
        }
        Set<Ticket> newSet = new HashSet<>(newCollection);

        for (Ticket oldTicket : oldCollection) {
            if (!newSet.contains(oldTicket)) {
                return true;
            }
        }

        return false;
    }

    private void updateUsersColors() {
        userToColor.clear();
        Set<String> users = ticketsObserveCollection.stream().map(Ticket::getOwnerLogin).collect(Collectors.toSet());
        int n = users.size();
        int i = 0;
        for (String user : users) {
            double hue = (double) i / n * 360;
            Color color = Color.hsb(hue, 0.9, 0.9);
            userToColor.put(user, color);
            i++;
        }
    }

    @FXML
    public void getInfo() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("info", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            AppLocale.getString("Info"),
                            AppLocale.getString("CollectionInfo"),
                            response.getMessage()
                    );
                    statusBarNotify(response.getResponseStatus().toString(), AppLocale.getString("GotAnswer"));

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
    public void logout() {
        AuthManager.setCurrentUser(null);
        this.authCallback.run();
    }

    @FXML
    public void showHistory() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("history", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            AppLocale.getString("History"),
                            AppLocale.getString("5LoastComm"),
                            response.getMessage()
                    );
                    statusBarNotify(response.getResponseStatus().toString(), AppLocale.getString("GotAnswer"));

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
    public void getHead() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("head", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                if (response.getCollection() != null) {
                    Ticket head = response.getCollection().stream().findFirst().get();
                    highlightRow(head.getId());
                }

                Platform.runLater(() -> {
                    statusBarNotify(response.getResponseStatus().toString(), AppLocale.getString("GotAnswer"));
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
                    Duration.seconds(1),
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
    public void printUniqueDiscountCommand() {
        new Thread(() -> {
            RequestCommand requestCommand = new RequestCommand("print_unique_discount", AuthManager.getCurrentUser());
            Response response = client.send(requestCommand);
            if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                Platform.runLater(() -> {
                    DialogHandler.commandResponseAlert(
                            AppLocale.getString("UniqueDiscount"),
                            AppLocale.getString("UniqueDiscount"),
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
    public void executeScriptCommand() {
        Platform.runLater(() -> {
            ScriptExecutor scriptExecutor = new ScriptExecutor();
            File file = DialogHandler.selectFile(stage, "SelectFile");
            scriptExecutor.run(file);
            DialogHandler.commandResponseAlert("Execution", "Result of execution " + file, scriptExecutor.getRes());
            synchronizeCollection();
        });
    }


    @FXML
    public void addElement() {
        editCallback.accept(null);
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
                            AppLocale.getString("ElementDeletion"),
                            AppLocale.getString("DeletionResult"),
                            response.getMessage()
                    );
                    statusBarNotify(response.getResponseStatus().toString(), AppLocale.getString("GotAnswer"));

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
        if (DialogHandler.confirmationDialog(AppLocale.getString("CollectionClear"), AppLocale.getString("SureWantToCleanQ"))) {
            new Thread(() -> {
                RequestCommand requestCommand = new RequestCommand("clear", AuthManager.getCurrentUser());
                Response response = client.send(requestCommand);
                if (response.getResponseStatus().equals(ResponseStatus.OK)) {
                    synchronizeCollection();
                    Platform.runLater(() -> {
                        DialogHandler.successAlert(AppLocale.getString("Success"), AppLocale.getString("CollectionClear"), response.getMessage());
                    });
                }
            }).start();
        }
    }

    @FXML
    private void deleteByIdCommand() {
        try {
            int id = DialogHandler.integerInputDialog(AppLocale.getString("Success"), AppLocale.getString("DeletionById"), AppLocale.getString("EnterElementId"));
            new Thread(() -> {
                RequestCommand requestCommand = new RequestCommand(
                        "remove_by_id",
                        new ArrayList<>(List.of(String.valueOf(id))),
                        AuthManager.getCurrentUser()
                );
                Response response = client.send(requestCommand);
                synchronizeCollection();
                Platform.runLater(() -> {
                    DialogHandler.successAlert(AppLocale.getString("DeletionById"), "id = " + id, response.getMessage());
                    statusBarNotify(response.getResponseStatus().toString(), response.getMessage());
                });
            }).start();
        } catch (NullPointerException ignored) {
        }

    }

}
