package org.example.server.utils;

import org.example.common.dtp.Response;

import java.io.ObjectOutputStream;

/**
 * Рекорд для хранения ответа и потока, по котоорому его отсылать для того, чтобы не путать пользователей
 * @param response ответ сервера
 * @param objectOutputStream поток для отправки ответа
 */
public record ConnectionPool(Response response, ObjectOutputStream objectOutputStream) {}
