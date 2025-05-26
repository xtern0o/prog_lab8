package org.example.client.managers;

import org.example.common.dtp.User;


/**
 * Статик класс для хранения текущего пользователя клиента
 */
public class AuthManager {
    private volatile static User currentUser;

    /**
     * Получить объект текущего пользователя;
     * null, если не авторизован
     * @return
     */
    public synchronized static User getCurrentUser() {
        return AuthManager.currentUser;
    }

    /**
     * Установить текущего пользователя;
     * null, чтобы разлогиниться
     * @param newUser
     */
    public synchronized static void setCurrentUser(User newUser) {
        AuthManager.currentUser = newUser;
    }
}
