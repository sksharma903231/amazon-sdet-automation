
package com.sksharma.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException(
                        "config.properties was not found."
                );
            }

            properties.load(input);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not load configuration.", e
            );
        }
    }

    public static String getProperty(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Configuration key must not be null or blank."
            );
        }

        String normalizedKey = key.trim();
        String value = System.getProperty(normalizedKey);
        if (value != null) {
            if (value.isBlank()) {
                throw new IllegalArgumentException(
                        "System property override for configuration key '"
                                + normalizedKey + "' must not be blank."
                );
            }
            return value.trim();
        }

        value = properties.getProperty(normalizedKey);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing or empty configuration property in config.properties: "
                            + normalizedKey
            );
        }

        return value.trim();
    }

    public static int getIntProperty(String key) {
        String value = getProperty(key);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) throw new NumberFormatException("must be positive");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Configuration property '" + key + "' must be a positive integer: " + value, e);
        }
    }

    public static boolean getBooleanProperty(String key) {
        String value = getProperty(key);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException(
                "Configuration property '" + key + "' must be true or false: " + value);
    }

}
