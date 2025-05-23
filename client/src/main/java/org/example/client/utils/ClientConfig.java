package org.example.client.utils;

import lombok.Getter;
import org.example.common.exceptions.ValidationError;

public class ClientConfig {
    private static ClientConfig instance;
    @Getter
    private String host;
    @Getter
    private int port;

    private ClientConfig() {
        this.host = "localhost";
        this.port = 8000;
    }

    public static synchronized ClientConfig getInstance() {
        if (instance == null) {
            instance = new ClientConfig();
        }
        return instance;
    }

    public static void initialize(String[] args) throws ValidationError {
        ClientConfig config = getInstance();
        if (args.length != 2) throw new ValidationError("2 аргумента: хост порт");
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            if (port < 0 || port > 65535) {
                throw new ValidationError("Некорректное значение порта: 0 < port < 65535");
            }
            config.host = host;
            config.port = port;
        } catch (NumberFormatException numberFormatException) {
            throw new ValidationError("Порт - число!!");
        }

    }
}
