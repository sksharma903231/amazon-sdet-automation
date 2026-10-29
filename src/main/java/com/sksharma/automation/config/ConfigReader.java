
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
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing or empty configuration property: " + key
            );
        }

        return value.trim();
    }

}
