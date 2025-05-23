package org.example.server.command.commands;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.server.command.Command;
import org.example.server.managers.CollectionManager;
import org.example.server.managers.DatabaseManager;
import org.example.server.utils.DatabaseSingleton;

public class RemoveByIdCommand extends Command {
    public RemoveByIdCommand() {
        super("remove_by_id", "удаляет элемент из коллекции по его id");
    }

    @Override
    public Response execute(RequestCommand requestCommand) {
        if (requestCommand.getArgs().size() != 1) throw new IllegalArgumentException();

        try {
            int id = Integer.parseInt(requestCommand.getArgs().get(0));

            DatabaseManager databaseManager = DatabaseSingleton.getDatabaseManager();

            int res = databaseManager.deleteObjectByIdFromUser(requestCommand.getUser(), id);
            if (res == 1) {
                CollectionManager.setCollection(databaseManager.loadCollection());
                return new Response(ResponseStatus.OK, String.format("Элемент с id=%d удален успешно", id));
            } else if (res == 0) {
                return new Response(ResponseStatus.OK, String.format("Элемент с id=%d не найден или не принадлежит Вам", id));
            }
            return new Response(ResponseStatus.COMMAND_ERROR, "Ошибка во время удаления");
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Команда принимает целочисленный аргумент");
        }

    }
}
