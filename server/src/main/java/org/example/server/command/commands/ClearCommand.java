package org.example.server.command.commands;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.server.command.Command;
import org.example.server.managers.CollectionManager;
import org.example.server.managers.DatabaseManager;
import org.example.server.utils.DatabaseSingleton;

/**
 * Класс команды clear
 */
public class ClearCommand extends Command {
    public ClearCommand() {
        super("clear", "удплить все элементы коллекции принадлежащие пользователю");
    }


    @Override
    public Response execute(RequestCommand requestCommand) {
        if (requestCommand.getArgs() != null) {
            if (!requestCommand.getArgs().isEmpty()) throw new IllegalArgumentException();
        }
        DatabaseManager databaseManager = DatabaseSingleton.getDatabaseManager();
        int deletedRows = databaseManager.deleteObjectsByUser(requestCommand.getUser());
        if (deletedRows == -1) {
            return new Response(ResponseStatus.COMMAND_ERROR, "Ошибка при удалении объектов");

        }
        CollectionManager.setCollection(databaseManager.loadCollection());
        return new Response(ResponseStatus.OK, "Удалено объектов: " + deletedRows);

    }
}