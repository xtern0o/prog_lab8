package org.example.server.command.commands;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.common.exceptions.ValidationError;
import org.example.server.command.Command;
import org.example.server.managers.CollectionManager;
import org.example.server.managers.DatabaseManager;
import org.example.server.utils.DatabaseSingleton;

import javax.xml.crypto.Data;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

// TODO: сделать update
public class UpdateCommand extends Command {
    public UpdateCommand() {
        super("update", "обновить элемент с введенным id");
    }

    @Override
    public Response execute(RequestCommand requestCommand) {
        if (requestCommand.getArgs().size() != 1) throw new IllegalArgumentException();

        try {
            PriorityBlockingQueue<Ticket> objectsFromUser = CollectionManager.getCollection()
                    .stream()
                    .filter(ticket -> Objects.equals(ticket.getOwnerLogin(), requestCommand.getUser().login()))
                    .collect(Collectors.toCollection(
                            PriorityBlockingQueue::new
                    ));

            int id = Integer.parseInt(requestCommand.getArgs().get(0));
            if (objectsFromUser.stream().noneMatch(ticket -> ticket.getId() == id)) {
                return new Response(ResponseStatus.ARGS_ERROR, String.format("Объекта с id=%d не существует или он не принадлежит Вам", id));
            }

            if (requestCommand.getTicketObject() == null) {
                return new Response(ResponseStatus.OBJECT_REQUIRED, "Для выполнения команды нужно создать элемент коллекции");
            }

            if (objectsFromUser.stream().anyMatch(ticket -> ticket.getId() == id)) {

                Ticket newTicket = requestCommand.getTicketObject();
                newTicket.setId(id);

                if (!newTicket.validate()) {
                    return new Response(ResponseStatus.VALIDATION_ERROR, "Объект некорректен");
                }
                DatabaseManager databaseManager = DatabaseSingleton.getDatabaseManager();
                int status = databaseManager.updateTicket(newTicket);
                if (status == 1) {
                    CollectionManager.setCollection(databaseManager.loadCollection());
                    return new Response(ResponseStatus.OK, String.format("Объект с id=%d был учпешно изменен", id));
                }
                if (status == 0) {
                    return new Response(ResponseStatus.OK, String.format("Объект с id=%d не найден или не принадлежит вам", id));
                }
                return new Response(ResponseStatus.COMMAND_ERROR, "Произошла ошибка при попытке обновления");
            }
            else {
                return new Response(ResponseStatus.ARGS_ERROR, String.format("Объекта с id=%d не существует или он не принадлежит вам", id));
            }
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Id - целое число");
        }
    }
}
