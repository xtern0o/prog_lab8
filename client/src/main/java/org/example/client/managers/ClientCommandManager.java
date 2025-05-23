package org.example.client.managers;

import lombok.Getter;
import org.example.client.command.ClientCommand;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.exceptions.NoSuchCommand;

import java.util.Collection;
import java.util.HashMap;

/**
 * Менеджер для client-side команд
 */
@Getter
public class ClientCommandManager {
    private final HashMap<String, ClientCommand> commands = new HashMap<>();

    /**
     * Добавляет команды в коллекцию команд
     * @param command объект команды
     */
    public void addCommand(ClientCommand command) {
        this.commands.put(command.getName(), command);
    }

    /**
     * Добавляет коллекцию команд в колеекцию команд
     * @param commandCollection коллекция из добавляемых команд
     */
    public void addCommands(Collection<ClientCommand> commandCollection) {
        for (ClientCommand command : commandCollection) {
            addCommand(command);
        }
    }

    /**
     * Производит выполнение команды
     * @param name имя команды
     * @param args аргументы необходимые для выполнения команды
     */
    public void execute(String name, String[] args) throws NoSuchCommand {
        if (!commands.containsKey(name)) throw new NoSuchCommand(name);

        ClientCommand clientCommand = commands.get(name);
        clientCommand.execute(args);
    }
}
