package org.example.server.managers;

import org.example.server.utils.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Утилитарный класс для управления существующими задачами (futures)
 */
public class TaskManager {
    private static final Collection<Future<ConnectionPool>> fixedThreadPoolFutures = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    public static synchronized void addNewFuture(Future<ConnectionPool> future) {
        fixedThreadPoolFutures.add(future);
    }

    /**
     * Получение резальтата готовых задач, а затем отправка ответа клиенту
     */
    public static synchronized void getReadyResults() {
        fixedThreadPoolFutures
                .stream()
                .filter(Future::isDone)
                .forEach(future -> {
                    try {
                        ConnectionManager.sendNewResponse(future.get());
                    } catch (ExecutionException | InterruptedException e) {
                        logger.warn("Произошла ошибка при обработке future's: {}", e.getMessage());
                    }
                });
        fixedThreadPoolFutures.removeIf(Future::isDone);
    }

}
