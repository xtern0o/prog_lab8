package org.example.server.managers;

import org.example.common.dtp.ResponseStatus;
import org.example.server.utils.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Утилитарный класс для управления существующими задачами (futures)
 */
public class TaskManager {
    private static final Collection<Future<List<ConnectionPool>>> fixedThreadPoolFutures = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    public static synchronized void addNewFuture(Future<List<ConnectionPool>> future) {
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
                        List<ConnectionPool> listOfPools = future.get();
                        for (ConnectionPool pool : listOfPools) {
                            if (pool.response().getResponseStatus().equals(ResponseStatus.COLLECTION_UPDATE)) {
                                CollectionNotifier.notifyAllClients();
                            }
                            ConnectionManager.sendNewResponse(pool);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        logger.warn("Произошла ошибка при обработке future's: {}", e.getMessage());
                    }
                });
        fixedThreadPoolFutures.removeIf(Future::isDone);
    }

}
