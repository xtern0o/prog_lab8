package org.example.server.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class MultiThreadServer implements Runnable {
    private final int port;
    private ServerSocketChannel serverSocketChannel;
    private final CommandManager commandManager;

    private SocketChannel clientChannel;

    private volatile boolean isRunning = false;

    public static final Logger logger = LoggerFactory.getLogger(MultiThreadServer.class);

    public MultiThreadServer(int port, CommandManager commandManager) {
        this.port = port;
        this.commandManager = commandManager;
    }

    @Override
    public void run() {
        try {
            openServerSocket();
        } catch (IOException ioException) {
            logger.error("Ошибка при открытии сервера", ioException);
            return;
        }

        this.isRunning = true;
        logger.info("Сервер прослушивает порт: " + port);

        while (isRunning) {
            TaskManager.getReadyResults();

            this.clientChannel = connectToClient();
            if (clientChannel != null) {
                new Thread(
                        new ConnectionManager(clientChannel, commandManager)
                ).start();
            }
        }

        shutdown();
    }

    private void openServerSocket() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        logger.info("Сокеты открыты!");
    }

    private void shutdown() {
        try {
            isRunning = false;
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }
            logger.info("Сервер остановлен");
        } catch (IOException e) {
            logger.error("Ошибка при остановке сервера (shutdown)", e);
        }
    }

    public void stop() {
        this.isRunning = false;
        shutdown();
    }

    private SocketChannel connectToClient() {
        try {
            serverSocketChannel.socket().setSoTimeout(100);
            clientChannel = serverSocketChannel.socket().accept().getChannel();
            logger.info("Соединение установлено: {}", clientChannel.getRemoteAddress());
            return clientChannel;
        } catch (IOException ignored) {
            return null;
        }
    }
}