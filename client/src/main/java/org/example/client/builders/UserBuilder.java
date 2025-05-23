package org.example.client.builders;

import org.example.client.utils.InputReader;
import org.example.common.dtp.User;
import org.example.common.utils.Printable;

import java.util.function.Predicate;

public class UserBuilder extends Builder<User> {
    public UserBuilder(Printable consoleOutput, InputReader consoleInput) {
        super(consoleOutput, consoleInput);
    }

    public User build() {
        Predicate<String> loginValidate = (login) -> (login.length() > 3);
        Predicate<String> passwordValidate = (password) -> (password != null && password.length() >= 6);

        String loginValidateErrorMessage = "Длина логина должна быть больше 3 символов";
        String passwordValidateErrorMessage = "Паоль не должен быть пустым; Длина пароля должна быть хотя бы 6 символов";

        String login = askString("login", "введите логин", loginValidate, loginValidateErrorMessage);
        String password = askString("password", "введите пароль", passwordValidate, passwordValidateErrorMessage);

        return new User(login, password);
    }
}
