package org.example.server.command.commands;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.server.command.Command;
import org.example.server.command.NoAuthCommand;
import org.example.server.managers.DatabaseManager;
import org.example.server.utils.DatabaseSingleton;

import java.sql.SQLException;

public class RegisterCommand extends Command implements NoAuthCommand {
    public RegisterCommand() {
        super("register", "Регистрация нового пользователя");
    }

    @Override
    public Response execute(RequestCommand requestCommand) {
        try {
            DatabaseManager databaseManager = DatabaseSingleton.getDatabaseManager();

            if (databaseManager.getUserByLogin(requestCommand.getUser().login()) != null) {
                return new Response(ResponseStatus.COMMAND_ERROR, "Пользователь с таким именем уже есть");
            }

            databaseManager.addUser(requestCommand.getUser());

            return new Response(ResponseStatus.OK, "Регистрация прошла успешно");
        } catch (SQLException sqlException) {
            return new Response(ResponseStatus.COMMAND_ERROR, "Ошибка создания пользователя: " + sqlException.getMessage());
        }

    }
}
