package org.example.client.managers;

import lombok.Getter;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.common.utils.Printable;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Класс клиента, отвечающий за общение с сервером
 */
public class Client implements Closeable {
    @Getter private final int port;
    @Getter private final String host;
    private final int maxReconnectionAttempts;
    private final int reconnectionDelay;
    private final Printable consoleOutput;
    private boolean exitIfUnsuccessfulConnection;

    private SocketChannel socketChannel;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int currentReconnectionAttempt;

    /**
     * Список слушателей которых надо уведомлять об изменении коллекции
     */
    private final CopyOnWriteArrayList<Consumer<Collection<Ticket>>> collectionUpdateListeners = new CopyOnWriteArrayList<>();

    /**
     * Добавить слушателя
     * @param listener слушатель (функция с аргументом коллекции тикетов)
     */
    public void addCollectionUpdateListener(Consumer<Collection<Ticket>> listener) {
        collectionUpdateListeners.add(listener);
    }

    /**
     * Удалить слушателя
     * @param listener слушатель
     */
    public void removeCollectionUpdateListener(Consumer<Collection<Ticket>> listener) {
        collectionUpdateListeners.remove(listener);
    }


    public static final int TIMEOUT_MS = 5000;

    public Client(
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
     * Метод для соединения с сервером, использует обычные сокеты вместо каналов
     * @return true если подключение успешно, false в противном случае
     */
    public boolean connectToServer() {
        try {
            closeConnection();

            consoleOutput.println("Попытка подключения к серверу: " + host + ":" + port);

            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.socket().setSoTimeout(TIMEOUT_MS);

            outputStream = new ObjectOutputStream(socketChannel.socket().getOutputStream());
            outputStream.flush();

            inputStream = new ObjectInputStream(socketChannel.socket().getInputStream());

            consoleOutput.println("Подключение к серверу успешно установлено: " + host + ":" + port);
            currentReconnectionAttempt = 0;

            return true;

        } catch (SocketTimeoutException e) {
            consoleOutput.printError("Время ожидания подключения - В С Ё: " + e.getMessage());
            return handleConnectionFailure();

        } catch (IOException e) {
            consoleOutput.printError("Ошибка ввода/вывода при подключении: " + e.getMessage());
            return handleConnectionFailure();

        } catch (UnresolvedAddressException e) {
            consoleOutput.printError("Неверный адрес сервера: " + e.getMessage());
            return false;

        } catch (Exception e) {
            consoleOutput.printError("Ватафак это че: " + e.getMessage());
            e.printStackTrace();
            return handleConnectionFailure();
        }
    }

    /**
     * Обрабатывает ошибку подключения и пытается переподключиться
     */
    private boolean handleConnectionFailure() {
        currentReconnectionAttempt++;

        if (currentReconnectionAttempt <= maxReconnectionAttempts) {
            consoleOutput.println("Попытка переподключения " + currentReconnectionAttempt +
                    " из " + maxReconnectionAttempts);

            try {
                closeConnection();
                Thread.sleep(reconnectionDelay);
                return connectToServer();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                consoleOutput.printError("Прерывание во время ожидания переподключения");
                return false;
            }
        } else {
            if (exitIfUnsuccessfulConnection) {
                consoleOutput.println("Не удалось подключиться к серверу после " +
                        maxReconnectionAttempts + " попыток.");
                if (exitIfUnsuccessfulConnection) {
                    consoleOutput.println("Завершение работы");
                    System.exit(-1);
                }
            }

            currentReconnectionAttempt = 0;
            consoleOutput.printError("Анлак не получилось подключиться к серверу после " + maxReconnectionAttempts + " попыток");
            return false;
        }
    }

    /**
     * Проверяет подключение и при необходимости переподключается
     */
    private boolean ensureConnected() {
        if (isConnected()) return true;

        consoleOutput.println("Соединение - В С Ё? Попытка переподключения...");
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
            consoleOutput.println("Запрос отправляется...");

            outputStream.writeObject(requestCommand);
            outputStream.flush();
            consoleOutput.println("Запрос отправлен");

            Response finalResp = null;

            while (true) {
                Object response = inputStream.readObject();

                if (response instanceof Response) {
                    consoleOutput.println("Ответ получен!!");
                    Response respObj = (Response) response;
                    if (respObj.getResponseStatus() == ResponseStatus.COLLECTION_UPDATE) {
                        handleCollectionUpdate(respObj);
                    } else {
                        finalResp = respObj;
                        break;
                    }
                } else {
                    consoleOutput.printError("Неверный тип ответа: " +
                            (response != null ? response.getClass().getName() : "null"));
                    return new Response(ResponseStatus.SERVER_ERROR, "Неверный формат ответа");
                }
            }

            return finalResp;


        } catch (IOException e) {
            consoleOutput.printError("Ошибка соединения с сервером: " + e.getMessage());

            if (connectToServer()) {
                consoleOutput.println("Повторная отправка запроса после переподключения...");
                return send(requestCommand);
            }

            return new Response(ResponseStatus.SERVER_ERROR, "Анлак не подключается чет: " + e.getMessage());

        } catch (ClassNotFoundException e) {
            consoleOutput.printError("Ошибка десериализации: " + e.getMessage());
            return new Response(ResponseStatus.SERVER_ERROR, "Ошибка десериализации ответа");

        } catch (Exception e) {
            consoleOutput.printError("чзх: " + e.getMessage());
            e.printStackTrace();
            return new Response(ResponseStatus.SERVER_ERROR, "Непредвиденная ошибка: " + e.getMessage());
        }
    }

    /**
     * Закрывает текущее соединение и освобождает ресурсы
     */
    private void closeConnection() {
        try {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
                outputStream = null;
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
                inputStream = null;
            }

            if (socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (IOException e) {
                }
                socketChannel = null;
            }

        } catch (Exception e) {
            consoleOutput.printError("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    /**
     * Проверяем, активно ли соединение
     */
    public boolean isConnected() {
        return socketChannel != null && socketChannel.isConnected() &&
                !socketChannel.socket().isClosed() && outputStream != null &&
                inputStream != null;
    }

    /**
     * Закрываем клиент.
     */
    @Override
    public void close() {
        closeConnection();
        consoleOutput.println("Клиент - В С Ё");
    }

    private void handleCollectionUpdate(Response response) {
        for (Consumer<Collection<Ticket>> listener : collectionUpdateListeners) {
            try {
                listener.accept(response.getCollection());
            } catch (Exception e) {
                consoleOutput.printError("Ошибка отправки обновления слушателю");
            }
        }
    }
}