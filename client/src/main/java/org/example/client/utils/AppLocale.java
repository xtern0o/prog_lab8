package org.example.client.utils;

import javafx.beans.property.SimpleObjectProperty;
import lombok.AllArgsConstructor;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.scene.control.*;

@AllArgsConstructor
public class AppLocale {
    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
            new Locale("ru"),
            new Locale("bg"),
            new Locale("cs"),
            new Locale("es", "GT")
    );

    // Свойство для наблюдения за изменениями локали
    private static final SimpleObjectProperty<Locale> currentLocaleProperty =
            new SimpleObjectProperty<>(SUPPORTED_LOCALES.get(0)); // Default: Русский
    private static ResourceBundle bundle = loadBundle(getCurrentLocale());

    public static Locale getCurrentLocale() {
        return currentLocaleProperty.get();
    }

    private static ResourceBundle loadBundle(Locale locale) {
        return ResourceBundle.getBundle("messages", locale);
    }

    public static void setLocale(Locale locale) {
        System.out.println("Попытка установить локаль " + locale.toString());
        if (!SUPPORTED_LOCALES.contains(locale)) {
            System.err.println("Локаль " + locale + " не поддерживается, используем дефолтную");
            throw new IllegalArgumentException("Unsupported locale: " + locale);
        }
        currentLocaleProperty.set(locale);
        System.out.println("ставим " + locale);
        bundle = loadBundle(locale);
        System.out.println("Поставили");
    }

    public static SimpleObjectProperty<Locale> currentLocaleProperty() {
        return currentLocaleProperty;
    }

    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getString(String key, Object... args) {
        try {
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return null;
        }
    }
}
