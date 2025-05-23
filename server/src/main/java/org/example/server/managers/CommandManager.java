package org.example.server.managers;

import lombok.Getter;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.exceptions.NoSuchCommand;
import org.example.server.command.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер для управления доступными командами
 * @author maxkarn
 */
@Getter
public class CommandManager {
    private final ConcurrentHashMap<String, Command> commands = new ConcurrentHashMap<>();
    private final ArrayList<Command> history = new ArrayList<>();

    /**
     * Добавляет команды в коллекцию команд
     * @param command объект команды
     */
    public void addCommand(Command command) {
        this.commands.put(command.getName(), command);
    }

    /**
     * Добавляет коллекцию команд в колеекцию команд
     * @param commandCollection коллекция из добавляемых команд
     */
    public void addCommands(Collection<Command> commandCollection) {
        for (Command command : commandCollection) {
            addCommand(command);
        }
    }

    /**
     * Сохраняет исполненную команду в истории
     * @param command команда, сохраняемая в истории
     */
    public void addToHistory(Command command) {
        history.add(command);
    }

    /**
     * Метод для выполнения команды
     * @param requestCommand сериализованный формат названия команд, аргументов и объекта коллекции
     * @return response
     * @throws NoSuchCommand если команда не найдена
     */
    public Response execute(RequestCommand requestCommand) throws NoSuchCommand {
        if (!commands.containsKey(requestCommand.getCommandName())) throw new NoSuchCommand(requestCommand.getCommandName());

        this.addToHistory(this.getCommands().get(requestCommand.getCommandName()));

        Command command = commands.get(requestCommand.getCommandName());
        return command.execute(requestCommand);
    }

    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }
}
