package org.example.server;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.server.cli.ConsoleOutput;
import org.example.server.command.Command;
import org.example.server.command.commands.*;
import org.example.server.managers.*;
import org.example.server.utils.DatabaseSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static Dotenv dotenv = Dotenv
            .configure()
            .directory("./server/")
            .load();

    static int port;

    static CommandManager commandManager = new CommandManager();
    static ConsoleOutput consoleOutput = new ConsoleOutput();

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (!validateArgs(args)) return;

        DatabaseManager databaseManager = DatabaseSingleton.getDatabaseManager();
        CollectionManager.setCollection(databaseManager.loadCollection());

        ArrayList<Command> commands = new ArrayList<Command>(Arrays.asList(
                new HelpCommand(commandManager),
                new HistoryCommand(commandManager),
                new AddCommand(),
                new ShowCommand(),
                new InfoCommand(),
                new ClearCommand(),
                new UpdateCommand(),
                new RemoveByIdCommand(),
                new HeadCommand(),
                new RemoveHeadCommand(),
                new FilterStartsWithNameCommand(),
                new PrintUniqueDiscountCommand(),
                new PrintFieldDescendingPersonCommand(),
                new ExecuteScriptCommand(),
                new PingCommand(),
                new RegisterCommand()
        )
        );
        commandManager.addCommands(commands);

        MultiThreadServer server = new MultiThreadServer(port, commandManager);
        RuntimeManager runtimeManager = new RuntimeManager(consoleOutput, server);

        try {
            runtimeManager.run();
        } catch (RuntimeException runtimeException) {
            logger.error("Ошибка выполнения программы: {}", runtimeException.getMessage());
        }

    }

    public static boolean validateArgs(String[] args) {
        if (args.length != 1) {
            logger.error("Неверное количество аргументов при запуске");
            consoleOutput.println("* Корректный запуск программы: java -jar <путь до программы> <порт прослушивания>");
            return false;
        }
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            logger.error("Некорректный порт");
            return false;
        }
        return true;
    }

}