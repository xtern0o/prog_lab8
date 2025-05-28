package org.example.client.managers;

import org.example.client.builders.TicketBuilder;
import org.example.client.cli.ConsoleInput;
import org.example.client.cli.ConsoleOutput;
import org.example.client.utils.ClientSingleton;
import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.common.exceptions.NoSuchCommand;
import org.example.common.utils.Printable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Класс, отвечающий за MainLoop и соединение действий пользователя и клиента
 */
public class RuntimeManager implements Runnable {
    private final Printable consoleOutput;
    private final ConsoleInput consoleInput;
    private Client client = ClientSingleton.getClient();
    private final RunnableScriptsManager runnableScriptsManager;
    private final ClientCommandManager clientCommandManager;

    public RuntimeManager(Printable consoleOutput, ConsoleInput consoleInput, RunnableScriptsManager runnableScriptsManager, ClientCommandManager clientCommandManager) {
        this.consoleOutput = consoleOutput;
        this.consoleInput = consoleInput;
        this.client = ClientSingleton.getClient();
        this.runnableScriptsManager = runnableScriptsManager;
        this.clientCommandManager = clientCommandManager;
    }

    public void executeCommand(String command) {

    }

    /**
     * Запуск клиента
     */
    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consoleOutput.println("Завершение работы программы. До свидания!!");
        }));

        consoleOutput.println(
                "> \"help\" для справки по доступным командам\n" +
                "> \"help_client\" для справки о клиентских командах (управление авторизацией)"
        );

        while (true) {
            try {
                consoleOutput.print(String.format("[%s@%s:%s] $ ", AuthManager.getCurrentUser() == null ? "<NOAUTH>" : AuthManager.getCurrentUser().login(), client.getHost(), client.getPort()));

                String queryString = consoleInput.readLine().trim();

                if (queryString.isBlank()) continue;

                String[] queryParts = queryString.split(" ");

                if (processClientCommand(queryParts, clientCommandManager)) continue;

                if (Objects.isNull(AuthManager.getCurrentUser())) {
                    consoleOutput.printError("Неавторизованные пользователи не могут выполнять команды для взаимодействия с коллекцией");
                    consoleOutput.println("> \"login\", если у вас уже есть аккаунт");
                    consoleOutput.println("> \"register\", чтобы создать новый");
                    continue;
                }

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
                this.printResponse(response);

                if (Objects.requireNonNull(response.getResponseStatus()) == ResponseStatus.OBJECT_REQUIRED) {
                    buildObject(queryParts);
                }
            } catch (NoSuchElementException noSuchElementException) {
                consoleOutput.println("Конец ввода");
                return;
            } catch (Exception exception) {
                consoleOutput.printError(exception.getClass().getName() + ": " + exception.getMessage());
            }
        }
    }

    /**
     * Парсинг ответа сервера в понятном для пользователя формате
     * @param response ответ сервера
     */
    public void printResponse(Response response) {
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
            default -> {}
        }
    }

    /**
     * Метод для выполнения пользовательского скрипта из файла
     * @param response ответ сервера
     */
    public void handleExecuteScript(Response response) {
        try {
            File scriptFile = new File(response.getMessage());
            if (!scriptFile.exists()) throw new FileNotFoundException(String.format("Исполняемый файл \"%s\" не найден", scriptFile.getName()));

            consoleOutput.println(String.format("* Исполнение файла \"%s\"", scriptFile.getName()));

            if (RunnableScriptsManager.checkIfLaunchedInStack(scriptFile)) {
                consoleOutput.printError(String.format("Исполняемый файл %s был вызван более одного раза в рамках одного скрипта. Исправьте код.", scriptFile.getName()));
                return;
            }

            ConsoleInput.setFileMode(true);
            RunnableScriptsManager.addFile(scriptFile);

            for (String line = runnableScriptsManager.readLine(); line != null; line = runnableScriptsManager.readLine()) {
                String queryString = line.trim();

                if (queryString.isBlank()) continue;

                String[] queryParts = queryString.split(" ");

                Response response1 = client.send(
                        new RequestCommand(
                                queryParts[0],
                                new ArrayList<>(Arrays.asList(Arrays.copyOfRange(queryParts, 1, queryParts.length))),
                                AuthManager.getCurrentUser()
                        )
                );

                if (response1 == null) {
                    consoleOutput.println("Запрос пустой");
                    continue;
                }
                this.printResponse(response1);

                switch (response1.getResponseStatus()) {
                    case OBJECT_REQUIRED -> {
                        buildObject(queryParts);
                        System.out.println(Arrays.toString(queryParts));
                    }
                    default -> {}
                }
            }

            consoleOutput.println("* Завершение исполнения файла " + scriptFile.getName());

            RunnableScriptsManager.removeFile(scriptFile);

            ConsoleInput.setFileMode(false);

        } catch (FileNotFoundException fileNotFoundException) {
            consoleOutput.printError(String.format("Исполняемый файл \"%s\" не найден", response.getMessage()));
        } catch (Exception exception) {
            RunnableScriptsManager.clear();
            throw new RuntimeException(exception);
        }
    }

    /**
     * Создание объекта при запросе со стороны сервера
     * @param queryParts части пользовательской команды, которая послужила инициатором необходимости отправки
     *                   нового объекта на сервер
     */
    public void buildObject(String[] queryParts) {
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

    /**
     * Метод для выполнения команд на стороне клиента
     * @param queryParts части команды
     * @return true если команда выполнена false если нет
     */
    public static boolean processClientCommand(String[] queryParts, ClientCommandManager clientCommandManager) {
        String qCommandName = queryParts[0];
        if (qCommandName.isBlank()) return false;
        String[] qCommandArgs = Arrays.copyOfRange(queryParts, 1, queryParts.length);

        try {
            clientCommandManager.execute(qCommandName, qCommandArgs);
            return true;
        } catch (NoSuchCommand noSuchCommand) {
            return false;
        }
    }
}
