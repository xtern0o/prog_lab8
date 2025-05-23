package org.example.server.utils;

import org.example.server.managers.CommandManager;

import java.util.Objects;

/**
 * Deprecated not used
 */
public class CommandManagerSingleton {
    public static CommandManager commandManager;

    static {
        commandManager = new CommandManager();
    }

    public static CommandManager getCommandManager() {
        if (Objects.isNull(commandManager)) commandManager = new CommandManager();
        return commandManager;
    }
}
