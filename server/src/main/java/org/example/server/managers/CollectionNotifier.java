package org.example.server.managers;

import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.server.utils.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class CollectionNotifier {
    private static final Logger logger = LoggerFactory.getLogger(CollectionNotifier.class);

    public static void notifyAllClients() {
        Map<SocketChannel, ObjectOutputStream> clients = ClientRegistry.getActiveClients();

        if (clients.isEmpty()) {
            logger.warn("Нет активных клиентов для уведомления");
            return;
        }

        logger.info("Отправка уведомления об обновлении коллекции {} клиентам", clients.size());

        Response updateResponse = new Response(
                ResponseStatus.COLLECTION_UPDATE,
                "Коллекция обновлена",
                CollectionManager.getCollection()
        );

        for (Map.Entry<SocketChannel, ObjectOutputStream> entry : clients.entrySet()) {
            SocketChannel channel = entry.getKey();
            ObjectOutputStream outputStream = entry.getValue();

            try {
                ConnectionManager.sendNewResponse(new ConnectionPool(updateResponse, outputStream));
                logger.debug("Уведомление отправлено клиенту: {}", channel);
            } catch (Exception e) {
                logger.error("Ошибка при отправке уведомления клиенту: {}", e.getMessage());
            }
        }
    }
}