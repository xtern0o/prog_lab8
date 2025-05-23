package org.example.client.command.client_commands;

import org.example.client.command.ClientCommand;

public class ExitCommand extends ClientCommand {
    public ExitCommand() {
        super("exit", "Выход из программы");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException();

        System.exit(0);
    }
}
