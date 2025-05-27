package org.example.server.managers;

import lombok.Getter;

import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Реестр активных пользователей
 */
public class ClientRegistry {
    @Getter
    private static final Map<SocketChannel, ObjectOutputStream> activeClients = new ConcurrentHashMap<>();

    public static void registerClient(SocketChannel channel, ObjectOutputStream outputStream) {
        activeClients.put(channel, outputStream);
        CollectionManager.logger.info("Клиент зарегистрирован: {}", channel);
    }

    public static void unregisterClient(SocketChannel channel) {
        activeClients.remove(channel);
        CollectionManager.logger.info("Клиент удален из реестра: {}", channel);
    }

}
