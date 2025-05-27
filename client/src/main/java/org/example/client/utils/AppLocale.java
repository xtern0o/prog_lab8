package org.example.client.utils;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@AllArgsConstructor
public class AppLocale {
    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
            new Locale("ru"),
            new Locale("bg"),
            new Locale("cs"),
            new Locale("es", "GT")
    );
    private static Locale currentLocale = SUPPORTED_LOCALES.get(0); // Default: Русский
    private static ResourceBundle bundle = loadBundle(currentLocale);

    private static ResourceBundle loadBundle(Locale locale) {
        return ResourceBundle.getBundle("messages", locale);
    }

    public static void setLocale(Locale locale) {
        if (!SUPPORTED_LOCALES.contains(locale)) {
            throw new IllegalArgumentException("Unsupported locale: " + locale);
        }
        currentLocale = locale;
        bundle = loadBundle(locale);
    }

    public static Locale getLocale() {
        return currentLocale;
    }

    public static String getString(String key) {
        return bundle.getString(key);
    }
}
