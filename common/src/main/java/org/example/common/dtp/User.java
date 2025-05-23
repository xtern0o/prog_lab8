package org.example.common.dtp;

import org.example.common.utils.Validatable;

import java.io.Serializable;

public record User(String login, String password) implements Serializable, Validatable {
    @Override
    public String toString() {
        return String.format("User(%s : %s)", login, password);
    }

    @Override
    public boolean validate() {
        return validateLogin(login) && validatePassword(password);
    }

    public static boolean validatePassword(String password) {
        return password.length() >= 6;
    }

    public static boolean validateLogin(String login) {
        return login.length() > 3;
    }
}
