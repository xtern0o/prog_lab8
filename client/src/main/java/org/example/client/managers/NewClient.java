package org.example.client.managers;

import lombok.Getter;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.common.utils.Printable;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Простой асинхронный клиент
 */
public class NewClient implements Closeable {
    @Getter private final int port;
    @Getter private final String host;
    private final int maxReconnectionAttempts;
    private final int reconnectionDelay;
    private final Printable consoleOutput;
    private boolean exitIfUnsuccessfulConnection;

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int currentReconnectionAttempt;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread listenerThread;

    public static final int TIMEOUT_MS = 5000;

    public NewClient(
            String host,
            int port,
            int maxReconnectionAttempts,
            int reconnectionDelay,
            Printable consoleOutput,
            boolean exitIfUnsuccessfulConnection
    ) {
        this.host = host;
        this.port = port;
        this.maxReconnectionAttempts = maxReconnectionAttempts;
        this.reconnectionDelay = reconnectionDelay;
        this.consoleOutput = consoleOutput;
        this.exitIfUnsuccessfulConnection = exitIfUnsuccessfulConnection;
    }

    /**
     * Подключение к серверу
     */
    public boolean connectToServer() {
        try {
            closeConnection();

            consoleOutput.println("Попытка подключения к серверу: " + host + ":" + port);

            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();

            inputStream = new ObjectInputStream(socket.getInputStream());

            consoleOutput.println("Подключение к серверу успешно: " + host + ":" + port);
            currentReconnectionAttempt = 0;

            // Запускаем поток для прослушивания
            startListenerThread();

            return true;

        } catch (IOException e) {
            consoleOutput.printError("Ошибка подключения: " + e.getMessage());
            return handleConnectionFailure();
        }
    }

    /**
     * Запускает поток для прослушивания сообщений от сервера
     */
    private void startListenerThread() {
        if (listenerThread != null && listenerThread.isAlive()) {
            running.set(false);
            listenerThread.interrupt();
        }

        running.set(true);
        listenerThread = new Thread(this::listenForResponses);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Поток прослушивания сообщений от сервера
     */
    private void listenForResponses() {
        while (running.get() && socket != null && !socket.isClosed()) {
            try {
                Object response = inputStream.readObject();

                if (response instanceof Response) {
                    Response resp = (Response) response;

                    if (resp.getResponseStatus() == ResponseStatus.COLLECTION_UPDATE) {
                        handleCollectionUpdate(resp);
                    }
                }
            } catch (IOException e) {
                consoleOutput.printError("Ошибка чтения: " + e.getMessage());
                handleReconnect();
                break;
            } catch (ClassNotFoundException e) {
                consoleOutput.printError("Ошибка десериализации: " + e.getMessage());
            }
        }
    }

    /**
     * Обработка обновления коллекции
     */
    protected void handleCollectionUpdate(Response response) {
        Collection<Ticket> collection = response.getCollection();
        if (collection != null) {
            consoleOutput.println("Получено обновление коллекции: " + collection.size() + " элементов");
            // Здесь можно добавить код для обновления UI
        }
    }

    /**
     * Обработка ошибки подключения
     */
    private boolean handleConnectionFailure() {
        currentReconnectionAttempt++;

        if (currentReconnectionAttempt <= maxReconnectionAttempts) {
            consoleOutput.println("Попытка переподключения " + currentReconnectionAttempt + " из " + maxReconnectionAttempts);

            try {
                Thread.sleep(reconnectionDelay);
                return connectToServer();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            if (exitIfUnsuccessfulConnection) {
                consoleOutput.println("Не удалось подключиться после " + maxReconnectionAttempts + " попыток. Завершение работы.");
                System.exit(-1);
            }

            consoleOutput.printError("Не удалось подключиться после " + maxReconnectionAttempts + " попыток");
            return false;
        }
    }

    /**
     * Переподключение при потере соединения
     */
    private void handleReconnect() {
        new Thread(() -> {
            consoleOutput.println("Соединение потеряно. Попытка переподключения...");
            connectToServer();
        }).start();
    }

    /**
     * Проверка подключения
     */
    private boolean ensureConnected() {
        if (isConnected()) return true;

        consoleOutput.println("Соединение разорвано. Попытка переподключения...");
        return connectToServer();
    }

    /**
     * Отправка запроса на сервер
     */
    public Response send(RequestCommand requestCommand) {
        if (requestCommand == null || requestCommand.isEmpty()) {
            return new Response(ResponseStatus.COMMAND_ERROR, "Пустой запрос");
        }

        if (!ensureConnected()) {
            return new Response(ResponseStatus.SERVER_ERROR, "Не удалось подключиться к серверу");
        }

        try {

            synchronized (outputStream) {
                outputStream.writeObject(requestCommand);
                outputStream.flush();
                outputStream.reset();
            }
            consoleOutput.println("Запрос отправлен");

            // Ждем ответ
            Response response;
            synchronized (inputStream) {
                response = (Response) inputStream.readObject();
                while (response.getResponseStatus().equals(ResponseStatus.COLLECTION_UPDATE)) {
                    handleCollectionUpdate(response);
                    response = (Response) inputStream.readObject();
                }
            }

            consoleOutput.println("Ответ получен!!");
            return response;

        } catch (IOException e) {
            consoleOutput.printError("Ошибка связи с сервером: " + e.getMessage());

            if (connectToServer()) {
                consoleOutput.println("Повторная отправка запроса после переподключения...");
                return send(requestCommand);
            }

            return new Response(ResponseStatus.SERVER_ERROR, "Ошибка связи: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            consoleOutput.printError("Ошибка десериализации: " + e.getMessage());
            return new Response(ResponseStatus.SERVER_ERROR, "Ошибка десериализации ответа");
        }
    }

    /**
     * Проверка статуса соединения
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() &&
                outputStream != null && inputStream != null;
    }

    /**
     * Закрытие соединения
     */
    private void closeConnection() {
        running.set(false);

        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }

        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }

            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            consoleOutput.printError("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    /**
     * Закрытие клиента
     */
    @Override
    public void close() {
        closeConnection();
        consoleOutput.println("Клиент закрыт");
    }
}