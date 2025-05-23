package org.example.server.utils;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.exceptions.NoSuchCommand;
import org.example.server.command.Command;
import org.example.server.command.NoAuthCommand;
import org.example.server.managers.CommandManager;

import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;

/**
 * Middleware
 * Класс для обработки запросов с командами
 * @author maxkarn
 */
public class RequestCommandHandler implements Callable<ConnectionPool> {
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
    public ConnectionPool call() {
        try {
            Command command = commandManager.getCommand(requestCommand.getCommandName());
            if (command == null) throw new NoSuchCommand(requestCommand.getCommandName());

            if (!(command instanceof NoAuthCommand)) {
                if (!requestCommand.getUser().validate()) {
                    return new ConnectionPool(
                            new Response(ResponseStatus.VALIDATION_ERROR, "Failed user validation"),
                            objectOutputStream
                    );
                }
                if (!DatabaseSingleton.getDatabaseManager().checkUserData(requestCommand.getUser())) {
                    return new ConnectionPool(
                            new Response(ResponseStatus.LOGIN_UNLUCK, "Неверные данные пользователя"),
                            objectOutputStream
                    );
                }
            }

            return new ConnectionPool(
                    commandManager.execute(requestCommand),
                    objectOutputStream
            );

        } catch (NoSuchCommand noSuchCommand) {
            return new ConnectionPool(
                    new Response(ResponseStatus.NO_SUCH_COMMAND, "Команда \"" + requestCommand.getCommandName() + "\" не найдена"),
                    objectOutputStream
            );
        } catch (IllegalArgumentException illegalArgumentException) {
            return new ConnectionPool(
                    new Response(ResponseStatus.ARGS_ERROR, "Неверное использование аргументов. " + illegalArgumentException.getMessage()),
                    objectOutputStream
            );
        }
    }
}
