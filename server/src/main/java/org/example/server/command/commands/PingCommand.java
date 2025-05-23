package org.example.server.command.commands;

import org.example.common.dtp.RequestCommand;
import org.example.common.dtp.Response;
import org.example.common.dtp.ResponseStatus;
import org.example.server.command.Command;

public class PingCommand extends Command {
    public PingCommand() {
        super("ping", "проверка корректности подключения");
    }

    @Override
    public Response execute(RequestCommand requestCommand) {
        return new Response(ResponseStatus.OK, "hello");
    }
}
