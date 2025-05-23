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

public class AddCommand extends Command {
    public AddCommand() {
        super("add", "add {element} - добавить новый элемент в коллекцию");
    }

    @Override
    public Response execute(RequestCommand requestCommand) {
        if (requestCommand.getArgs() != null) {
            if (!requestCommand.getArgs().isEmpty()) throw new IllegalArgumentException();
        }

        if (requestCommand.getTicketObject() == null) {
            return new Response(ResponseStatus.OBJECT_REQUIRED, "Для выполнения команды нужно создать элемент коллекции");
        } else {
            DatabaseManager databaseManager = DatabaseSingleton.getDatabaseManager();

            Ticket newTicket = requestCommand.getTicketObject();

            if (!newTicket.validate()) {
                return new Response(ResponseStatus.VALIDATION_ERROR, "Новый объект не прошел валидацию: \n" + newTicket.toString() );
            }

            int newTicketId = databaseManager.addTicket(newTicket);

            if (newTicketId == -1) {
                return new Response(ResponseStatus.COMMAND_ERROR, "Ошибка при добавлении в БД");
            }

            newTicket.setId(newTicketId);
            CollectionManager.addElement(newTicket);

            return new Response(ResponseStatus.OK, "Объект успешно добавлен в коллекцию");
        }
    }
}
