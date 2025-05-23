package org.example.client.command.client_commands;

import org.example.client.cli.ConsoleOutput;
import org.example.client.command.ClientCommand;
import org.example.client.managers.AuthManager;

public class LogoutCommand extends ClientCommand {
    private final ConsoleOutput consoleOutput;

    public LogoutCommand(ConsoleOutput consoleOutput) {
        super("logout", "Выход из аккаунта");
        this.consoleOutput = consoleOutput;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException();

        if (AuthManager.getCurrentUser() == null) {
            consoleOutput.println("Вы итак неавторизованы");
            return;
        }
        consoleOutput.println(String.format("Вы вышли из аккаунта \"%s\"", AuthManager.getCurrentUser().login()));
        AuthManager.setCurrentUser(null);

    }
}
