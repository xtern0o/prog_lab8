package org.example.server.managers;

import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.common.entity.Ticket;
import org.example.server.utils.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;

public class CollectionNotifier {
    private static final Logger logger = LoggerFactory.getLogger(CollectionNotifier.class);

    public static void notifyAllClients() {
        Collection<Ticket> currentCollection = new PriorityQueue<>(CollectionManager.getCollection());

        Response updateResponse = new Response(
                ResponseStatus.COLLECTION_UPDATE,
                "Обновление коллекции",
                currentCollection
        );

        Map<SocketChannel, ObjectOutputStream> clients = ClientRegistry.getActiveClients();
        logger.info("Отправка обновления коллекции {} клиентам", clients.size());

        for (Map.Entry<SocketChannel, ObjectOutputStream> entry : clients.entrySet()) {
            ObjectOutputStream objectOutputStream = entry.getValue();

            ConnectionManager.sendNewResponse(new ConnectionPool(updateResponse, objectOutputStream));
        }
    }
}
