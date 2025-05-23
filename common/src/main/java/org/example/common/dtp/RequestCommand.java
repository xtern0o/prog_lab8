package org.example.common.dtp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.example.common.entity.Ticket;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Класс запроса с клиента на сервер
 */
@AllArgsConstructor
@Getter
public class RequestCommand implements Serializable {
    /**
     * Название команды
     */
    private final String commandName;

    /**
     * Аргументы в строчном формате
     */
    private final ArrayList<String> args;

    /**
     * Объект Ticket (например при передаче add или update)
     */
    private final Ticket ticketObject;

    private final User user;

    public RequestCommand(String commandName, ArrayList<String> args, @NonNull User user) {
        this(commandName, args, null, user);
    }

    public RequestCommand(String commandName, @NonNull User user) {
        this(commandName, null, null, user);
    }

    public RequestCommand(String commandName, Ticket ticketObject, @NonNull User user) {
        this(commandName, null, ticketObject, user);
    }
    
    public boolean isEmpty() {
        return commandName.isBlank() && args.isEmpty() && ticketObject == null;
    }
}
