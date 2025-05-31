package org.example.client.utils;

import lombok.Getter;
import org.example.client.builders.TicketBuilder;
import org.example.client.gui.controllers.MainViewController;
import org.example.client.managers.AuthManager;
import org.example.client.managers.Client;
import org.example.client.managers.ClientCommandManager;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.common.exceptions.NoSuchCommand;
import org.example.common.utils.Printable;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;

/**
 * Класс для выполнения скриптов
 */
public class ScriptExecutor {
    @Getter
    private String res = "";
    private Printable consoleOutput = new Printable() {
        @Override
        public void print(String s) {
            res += s;
        }

        @Override
        public void println(String s) {
            res += (s + "\n");
        }

        @Override
        public void printError(String s) {
            res += ("[Error]: " + s + "\n");
        }
    };
    private InputReader consoleInput;

    Client client;

    private final List<File> scriptStack = new ArrayList<>();


    public ScriptExecutor() {
        client = ClientSingleton.getClient();
    }

    public void run(File file) {
        String userCommand;
        scriptStack.add(file);

        try (Scanner scriptScanner = new Scanner(file)) {
            if (!scriptScanner.hasNext()) throw new NoSuchElementException();

            consoleInput = new InputReader() {
                @Override
                public String readLine() {
                    return scriptScanner.nextLine();
                }
            };

            while (scriptScanner.hasNextLine()) {
                userCommand = scriptScanner.nextLine().trim();
                if (userCommand.isBlank()) continue;
                String[] queryParts = userCommand.split(" ");
                // launch command
                Response response = client.send(
                        new RequestCommand(
                                queryParts[0],
                                new ArrayList<>(Arrays.asList(Arrays.copyOfRange(queryParts, 1, queryParts.length))),
                                AuthManager.getCurrentUser()
                        )
                );

                if (response == null) {
                    consoleOutput.println("Запрос пустой");
                    continue;
                }

                switch (response.getResponseStatus()) {
                    case OK -> {
                        consoleOutput.println(response.getMessage());
                        if (response.getCollection() != null) {
                            for (Ticket t : response.getCollection()) {
                                consoleOutput.println(t.toString());
                            }
                        }
                    }
                    case COMMAND_ERROR -> {
                        consoleOutput.printError("Ошибка выполнения команды: " + response.getMessage());

                    }
                    case ARGS_ERROR -> {
                        consoleOutput.printError("Некорректное использование аргументов команды. " + response.getMessage());
                    }
                    case NO_SUCH_COMMAND, SERVER_ERROR -> {
                        consoleOutput.printError(response.getMessage());
                    }
                    case EXECUTE_SCRIPT -> {
                        handleExecuteScript(response);
                    }
                    case OBJECT_REQUIRED -> {
                        buildObject(queryParts);
                    }
                    default -> {}
                }

                dumpData();

            }

            consoleOutput.println("* Завершение исполнения файла " + file.getName());


        } catch (FileNotFoundException fileNotFoundException) {
            DialogHandler.errorAlert(AppLocale.getString("FileNotFound"), AppLocale.getString("FileXNotFound", file.getName()), "");
            dumpData();
        } catch (NoSuchElementException noSuchElementException) {
            DialogHandler.errorAlert("Error", "EmptyFileError", "");
            dumpData();

        }
    }

    private void handleExecuteScript(Response response) {
        File newFile = new File(response.getMessage());
        try {
            if (!newFile.exists()) throw new FileNotFoundException();
            consoleOutput.println(String.format("* Исполнение файла \"%s\"", newFile.getName()));
            if (scriptStack.contains(newFile)) {
                consoleOutput.printError("Нельзя запускать рекурсию");
                dumpData();
                return;
            }
            run(newFile);

        } catch (FileNotFoundException fileNotFoundException){
            consoleOutput.printError("Файл " + newFile + " не найден");
            dumpData();
        }
    }

    private void buildObject(String[] queryParts) {
        Ticket ticket = new TicketBuilder(consoleOutput, consoleInput).build();
        Response responseOnBuild = client.send(
                new RequestCommand(
                        queryParts[0],
                        new ArrayList<>(Arrays.asList(Arrays.copyOfRange(queryParts, 1, queryParts.length))),
                        ticket,
                        AuthManager.getCurrentUser()
                )
        );
        if (responseOnBuild.getResponseStatus() != ResponseStatus.OK) {
            consoleOutput.printError("При создании объекта произошла ошибка. " + responseOnBuild.getMessage());
        }
        else {
            consoleOutput.println(responseOnBuild.getMessage());
        }

    }

    private void dumpData() {
        return;
    }
}
