package org.example.client.command.client_commands;

import org.example.client.cli.ConsoleOutput;
import org.example.client.command.ClientCommand;
import org.example.client.managers.ClientCommandManager;

public class HelpClientCommand extends ClientCommand {
    private final ConsoleOutput consoleOutput;
    private final ClientCommandManager clientCommandManager;

    public HelpClientCommand(ConsoleOutput consoleOutput, ClientCommandManager clientCommandManager) {
        super("help_client", "Справка о доступных клиентских командах");
        this.consoleOutput = consoleOutput;
        this.clientCommandManager = clientCommandManager;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException();

        consoleOutput.println("* Справка по клиентским командам");
        clientCommandManager.getCommands()
                .values()
                .forEach(command -> consoleOutput.println(command.toString()));
    }
}
