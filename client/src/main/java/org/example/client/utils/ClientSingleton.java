package org.example.client.utils;

import org.example.client.Main;
import org.example.client.cli.ConsoleOutput;
import org.example.client.managers.Client;

public class ClientSingleton {
    private static volatile Client client;

    public static synchronized Client getClient() {
        if (client == null) {
            client = new Client(
                    ClientConfig.getInstance().getHost(),
                    ClientConfig.getInstance().getPort(),
                    10,
                    200,
                    Main.consoleOutput,
                    false
            );
        }
        return client;
    }
}
