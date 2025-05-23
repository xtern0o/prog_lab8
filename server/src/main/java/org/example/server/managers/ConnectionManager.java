package org.example.server.managers;

import org.example.common.dtp.ObjectSerializer;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.server.command.Command;
import org.example.server.utils.ConnectionPool;
import org.example.server.utils.RequestCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Класс ля запуска Runnable-таски по обработке соединений
 * Многопоточное чтение (Thread)
 * Многопоточная обработка (FixedThreadPool)
 */
public class ConnectionManager implements Runnable {
    private static final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);
    private final SocketChannel clientChannel;
    private final CommandManager commandManager;

    private RequestCommand requestCommand;

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    public ConnectionManager(SocketChannel clientChannel, CommandManager commandManager) {
        this.clientChannel = clientChannel;
        this.commandManager = commandManager;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream clientReader = new ObjectInputStream(clientChannel.socket().getInputStream());
            ObjectOutputStream clientWriter = new ObjectOutputStream(clientChannel.socket().getOutputStream());

            while (true) {
                requestCommand = (RequestCommand) clientReader.readObject();
                logger.info(
                        "GOT REQUEST: CommandName: {}; Args: {}, User: {}",
                        requestCommand.getCommandName(), requestCommand.getArgs(), requestCommand.getUser()
                );
                TaskManager.addNewFuture(
                        fixedThreadPool.submit(
                                new RequestCommandHandler(
                                        requestCommand,
                                        clientWriter,
                                        commandManager
                                )
                        )
                );
            }


        } catch (EOFException eofException) {
            logger.info("Клиент закрыл соединение");
        } catch (IOException e) {
            logger.warn("Неудача при десериализации: {}", e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.warn("Не удалось корректо десериализовать: {}", e.getMessage());
        }
    }


    public static void sendNewResponse(ConnectionPool connectionPool) {
        new Thread(() -> {
            try {
                connectionPool.objectOutputStream().writeObject(connectionPool.response());
                connectionPool.objectOutputStream().flush();
                logger.info(
                        "SENT RESPONSE [{}]",
                        connectionPool.response().getResponseStatus()
                );
            } catch (IOException ioException) {
                logger.warn("Не удалось отправить ответ клиенту: " + ioException.getMessage());
            }


        }).start();
    }
}
