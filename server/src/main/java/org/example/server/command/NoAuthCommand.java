package org.example.server.command;

/**
 * Этим интерфейсом помечаются команды, выполнение которых не требует авторизации.
 * В частности создано для того, чтобы избежать хардкода команды register
 */
public interface NoAuthCommand {
}
