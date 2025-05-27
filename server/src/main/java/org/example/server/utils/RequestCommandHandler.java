package org.example.server.utils;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.common.exceptions.NoSuchCommand;
import org.example.server.command.Command;
import org.example.server.command.NoAuthCommand;
import org.example.server.managers.CollectionManager;
import org.example.server.managers.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Middleware
 * Класс для обработки запросов с командами
 * @author maxkarn
 */
public class RequestCommandHandler implements Callable<List<ConnectionPool>> {
    private static final Logger logger = LoggerFactory.getLogger(RequestCommandHandler.class);
    private final CommandManager commandManager;
    private final RequestCommand requestCommand;
    private final ObjectOutputStream objectOutputStream;

    public RequestCommandHandler(RequestCommand requestCommand, ObjectOutputStream objectOutputStream, CommandManager commandManager) {
        this.requestCommand = requestCommand;
        this.objectOutputStream = objectOutputStream;
        this.commandManager = commandManager;
    }

    /**
     * Обработка запроса
     * @return
     */
    public List<ConnectionPool> call() {
        try {
            Command command = commandManager.getCommand(requestCommand.getCommandName());
            if (command == null) throw new NoSuchCommand(requestCommand.getCommandName());

            if (!(command instanceof NoAuthCommand)) {
                if (!requestCommand.getUser().validate()) {
                    return new ArrayList<>(List.of(new ConnectionPool(
                            new Response(ResponseStatus.VALIDATION_ERROR, "Failed user validation"),
                            objectOutputStream
                    )));
                }
                if (!DatabaseSingleton.getDatabaseManager().checkUserData(requestCommand.getUser())) {
                    return new ArrayList<>(List.of(new ConnectionPool(
                            new Response(ResponseStatus.LOGIN_UNLUCK, "Неверные данные пользователя"),
                            objectOutputStream
                    )));
                }
            }

            PriorityBlockingQueue<Ticket> oldCollection = CollectionManager.getCollection();
            Response response = commandManager.execute(requestCommand);
            PriorityBlockingQueue<Ticket> newCollection = CollectionManager.getCollection();

            logger.info("Old collection hash: {}, size: {}", oldCollection.hashCode(), oldCollection.size());
            logger.info("New collection hash: {}, size: {}", newCollection.hashCode(), newCollection.size());
            logger.info("Collections equal: {}", oldCollection.equals(newCollection));

            boolean contentChanged = oldCollection.size() != newCollection.size() ||
                    !oldCollection.containsAll(newCollection) ||
                    !newCollection.containsAll(oldCollection);
            logger.info("Content changed: {}", contentChanged);

            if (!contentChanged) {
                return new ArrayList<>(List.of(new ConnectionPool(
                        response,
                        objectOutputStream
                )));
            }

            if (oldCollection.equals(newCollection))

            return new ArrayList<>(List.of(new ConnectionPool(
                    response,
                    objectOutputStream
            ), new ConnectionPool(
                    new Response(ResponseStatus.COLLECTION_UPDATE, "UPDATE!!", CollectionManager.getCollection()),
                    objectOutputStream
            )));

        } catch (NoSuchCommand noSuchCommand) {
            return new ArrayList<>(List.of(new ConnectionPool(
                    new Response(ResponseStatus.NO_SUCH_COMMAND, "Команда \"" + requestCommand.getCommandName() + "\" не найдена"),
                    objectOutputStream
            )));
        } catch (IllegalArgumentException illegalArgumentException) {
            return new ArrayList<>(List.of(new ConnectionPool(
                    new Response(ResponseStatus.ARGS_ERROR, "Неверное использование аргументов. " + illegalArgumentException.getMessage()),
                    objectOutputStream
            )));
        }
    }
}
