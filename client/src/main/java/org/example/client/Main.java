package org.example.client;

import javafx.application.Application;
import org.example.client.cli.ConsoleInput;
import org.example.client.cli.ConsoleOutput;
import org.example.client.command.ClientCommand;
import org.example.client.command.client_commands.*;
import org.example.client.gui.App;
import org.example.client.managers.*;
import org.example.client.utils.AppLocale;
import org.example.client.utils.ClientConfig;
import org.example.common.exceptions.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static String host;
    public static int port;
    public static ConsoleOutput consoleOutput = new ConsoleOutput();
    static ConsoleInput consoleInput = new ConsoleInput();
    static RunnableScriptsManager runnableScriptsManager = new RunnableScriptsManager();
    static ClientCommandManager clientCommandManager = new ClientCommandManager();

    public static void main(String[] args) {
        try {
            ClientConfig.initialize(args);
        } catch (ValidationError validationError) {
            consoleOutput.printError(validationError.getMessage());
        }

        ArrayList<ClientCommand> commands = new ArrayList<>(List.of(
                new ExitCommand(),
                new LoginCommand(consoleInput, consoleOutput),
                new RegisterCommand(consoleInput, consoleOutput),
                new LogoutCommand(consoleOutput),
                new HelpClientCommand(consoleOutput, clientCommandManager)
        ));

        clientCommandManager.addCommands(commands);

//        new RuntimeManager(consoleOutput, consoleInput, client, runnableScriptsManager, clientCommandManager).run();
        Application.launch(App.class, args);
    }
}
