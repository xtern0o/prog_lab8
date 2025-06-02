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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Многопоточный клиент, где чтение происходит в отдельном потоке
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

    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    private volatile boolean running = false;
    private Thread readerThread;

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
     * Метод для соединения с сервером
     * @return true если подключение успешно, false в противном случае
     */
    public boolean connectToServer() {
        try {
            closeConnection();

            consoleOutput.println("Попытка подключения к серверу: " + host + ":" + port);

            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.configureBlocking(true);
            socketChannel.socket().setSoTimeout(TIMEOUT_MS);

            outputStream = new ObjectOutputStream(socketChannel.socket().getOutputStream());
            outputStream.flush();

            inputStream = new ObjectInputStream(socketChannel.socket().getInputStream());

            running = true;
            readerThread = new Thread(this::readerLoop, "Client-Reader-Thread");
            readerThread.setDaemon(true);
            readerThread.start();

            consoleOutput.println("Подключение к серверу успешно установлено: " + host + ":" + port);
            currentReconnectionAttempt = 0;
            return true;

        } catch (SocketTimeoutException e) {
            consoleOutput.printError("Время ожидания подключения истекло: " + e.getMessage());
            return handleConnectionFailure();

        } catch (IOException e) {
            consoleOutput.printError("Ошибка ввода/вывода при подключении: " + e.getMessage());
            return handleConnectionFailure();

        } catch (UnresolvedAddressException e) {
            consoleOutput.printError("Неверный адрес сервера: " + e.getMessage());
            return false;

        } catch (Exception e) {
            consoleOutput.printError("Неожиданная ошибка при подключении: " + e.getMessage());
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
                consoleOutput.println("Завершение работы");
                System.exit(-1);
            }

            currentReconnectionAttempt = 0;
            consoleOutput.printError("Не удалось подключиться к серверу после " + maxReconnectionAttempts + " попыток");
            return false;
        }
    }

    /**
     * Цикл чтения в отдельном потоке
     */
    private void readerLoop() {
        consoleOutput.println("Поток чтения запущен");
        try {
            while (running && inputStream != null) {
                try {
                    Object response = inputStream.readObject();

                    if (response instanceof Response respObj) {
                        consoleOutput.println("Получен ответ: " + respObj.getResponseStatus());


                        if (respObj.getResponseStatus() == ResponseStatus.COLLECTION_UPDATE) {
                            handleCollectionUpdate(respObj);
                        } else {
                            responseQueue.offer(respObj);
                        }
                    } else {
                        consoleOutput.printError("Неверный тип ответа: " +
                                (response != null ? response.getClass().getName() : "null"));
                    }
                } catch (ClassNotFoundException e) {
                    consoleOutput.printError("Ошибка десериализации: " + e.getMessage());
                } catch (IOException e) {
                    if (running) {
                        consoleOutput.printError("Ошибка чтения из сокета: " + e.getMessage());

                        // Костыль с большой буквы для обработки отключения сервера
                        if (e.getMessage() == null) {
                            if (connectToServer()) {
                                consoleOutput.println("Переподключение после ошибки успешно");
                                continue;
                            } else {
                                consoleOutput.printError("Не удалось подключиться");
                                continue;
                            }
                        }

                        if (ensureConnected()) {
                            consoleOutput.println("Переподключение успешно");
                            continue;
                        } else {
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            if (running) {
                consoleOutput.printError("Непредвиденная ошибка в потоке чтения: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            consoleOutput.println("Поток чтения завершен");
        }
    }

    /**
     * Проверяет подключение и при необходимости переподключается
     */
    private boolean ensureConnected() {
        if (isConnected()) return true;

        consoleOutput.println("Соединение потеряно. Попытка переподключения...");
        return connectToServer();
    }

    /**
     * Отправка запроса на сервер и ожидание ответа
     */
    public synchronized Response send(RequestCommand requestCommand) {
        if (requestCommand == null || requestCommand.isEmpty()) {
            return new Response(ResponseStatus.COMMAND_ERROR, "Пустой запрос");
        }

        if (!ensureConnected()) {
            return new Response(ResponseStatus.SERVER_ERROR, "Не удалось подключиться к серверу");
        }

        try {
            responseQueue.clear();

            consoleOutput.println("Отправка запроса...");
            synchronized (outputStream) {
                outputStream.reset();
                outputStream.writeObject(requestCommand);
                outputStream.flush();
            }
            consoleOutput.println("Запрос отправлен");

            Response response = responseQueue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (response == null) {
                consoleOutput.printError("Таймаут ожидания ответа");
                return new Response(ResponseStatus.SERVER_ERROR, "Превышено время ожидания ответа");
            }

            consoleOutput.println("Получен ответ на запрос");

            Thread.sleep(100);

            return response;

        } catch (IOException e) {
            consoleOutput.printError("Ошибка соединения с сервером: " + e.getMessage());

            if (connectToServer()) {
                consoleOutput.println("Повторная отправка запроса после переподключения...");
                return send(requestCommand);
            }

            return new Response(ResponseStatus.SERVER_ERROR, "Ошибка соединения: " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Response(ResponseStatus.SERVER_ERROR, "Ожидание ответа было прервано");

        } catch (Exception e) {
            consoleOutput.printError("Непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
            return new Response(ResponseStatus.SERVER_ERROR, "Непредвиденная ошибка: " + e.getMessage());
        }
    }

    /**
     * Закрывает текущее соединение и освобождает ресурсы
     */
    private void closeConnection() {
        running = false;

        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            readerThread = null;
        }

        try {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {}
                outputStream = null;
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
                inputStream = null;
            }

            if (socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (IOException ignored) {}
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
        consoleOutput.println("Клиент закрыт");
    }

    /**
     * Обработка обновления коллекции
     */
    private void handleCollectionUpdate(Response response) {
        consoleOutput.println("Получено обновление коллекции");
        for (Consumer<Collection<Ticket>> listener : collectionUpdateListeners) {
            try {
                listener.accept(response.getCollection());
            } catch (Exception e) {
                consoleOutput.printError("Ошибка отправки обновления слушателю: " + e.getMessage());
            }
        }
    }
}