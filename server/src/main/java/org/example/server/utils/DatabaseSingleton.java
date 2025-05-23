package org.example.server.utils;

import org.example.server.managers.DatabaseManager;

import java.util.Objects;

/**
 * Класс-синглтон для работы с единственным менеджером базы данных
 */
public class DatabaseSingleton {
    private static DatabaseManager databaseManager;

    static {
        databaseManager = new DatabaseManager();
    }

    public static DatabaseManager getDatabaseManager() {
        if (Objects.isNull(databaseManager)) databaseManager = new DatabaseManager();
        return databaseManager;
    }
}
