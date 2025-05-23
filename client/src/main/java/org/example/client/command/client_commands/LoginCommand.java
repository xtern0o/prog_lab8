package org.example.client.command.client_commands;

import org.example.client.builders.UserBuilder;
import org.example.client.cli.ConsoleInput;
import org.example.client.cli.ConsoleOutput;
import org.example.client.command.ClientCommand;
import org.example.client.managers.AuthManager;
import org.example.client.managers.Client;
import org.example.client.utils.ClientSingleton;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.dtp.User;

import java.util.Objects;

public class LoginCommand extends ClientCommand {
    private final ConsoleInput consoleInput;
    private final ConsoleOutput consoleOutput;
    private final Client client = ClientSingleton.getClient();

    public LoginCommand(ConsoleInput consoleInput, ConsoleOutput consoleOutput) {
        super("login", "авторизация");

        this.consoleInput = consoleInput;
        this.consoleOutput = consoleOutput;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException();

        if (!Objects.isNull(AuthManager.getCurrentUser())) {
            consoleOutput.println("Вы уже авторизованы как \"" + AuthManager.getCurrentUser().login() + "\"");
            return;
        }

        consoleOutput.println("Авторизация");

        User user = new UserBuilder(consoleOutput, consoleInput).build();

        RequestCommand requestCommand = new RequestCommand("ping", user);
        Response response = client.send(requestCommand);
        if (response.getResponseStatus().equals(ResponseStatus.OK)) {
            AuthManager.setCurrentUser(user);
            consoleOutput.println("Вы успешно авторизованы как \"" + user.login() + "\"");
        }
        else if (response.getResponseStatus().equals(ResponseStatus.LOGIN_UNLUCK)) {
            consoleOutput.printError("Неверный логин или пароль");
        }
        else {
            consoleOutput.printError(response.getResponseStatus() + ": " + response.getMessage());
        }

    }
}
