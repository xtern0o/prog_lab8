package org.example.server.command.commands;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.server.command.Command;
import org.example.server.managers.CollectionManager;
import org.example.server.managers.DatabaseManager;
import org.example.server.utils.DatabaseSingleton;

import javax.xml.crypto.Data;
import java.util.Collection;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

public class RemoveHeadCommand extends Command {
    public RemoveHeadCommand() {
        super("remove_head", "выводит первый элемент коллекции и удаляет его");
    }


    @Override
    public Response execute(RequestCommand requestCommand) {
        if (requestCommand.getArgs() != null) {
            if (!requestCommand.getArgs().isEmpty()) throw new IllegalArgumentException();
        }
        if (CollectionManager.getCollectionSize() == 0) {
            return new Response(ResponseStatus.OK, "Коллекция пуста");
        }

        PriorityBlockingQueue<Ticket> objectsFromUser = CollectionManager.getCollection()
                .stream()
                .filter(ticket -> Objects.equals(ticket.getOwnerLogin(), requestCommand.getUser().login()))
                .collect(Collectors.toCollection(
                        PriorityBlockingQueue::new
                ));

        if (objectsFromUser.isEmpty()) {
            return new Response(ResponseStatus.OK, "У Вас нет билетиков");
        }

        Ticket deletedTicket = objectsFromUser.poll();

        DatabaseManager databaseManager = DatabaseSingleton.getDatabaseManager();
        int status = databaseManager.deleteObjectByIdFromUser(requestCommand.getUser(), deletedTicket.getId());

        if (status == -1) {
            return new Response(ResponseStatus.COMMAND_ERROR, "Ошибка при удалении");
        } else if (status == 0) {
            return new Response(ResponseStatus.OK, "Объект не найден");
        }

        CollectionManager.setCollection(databaseManager.loadCollection());

        return new Response(
                ResponseStatus.OK,
                "Эта запись была удалена:\n" + deletedTicket
        );
    }
}
