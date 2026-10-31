package com.sksharma.automation.driver;

import com.sksharma.automation.config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;


public class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    public static WebDriver createDriver() {

        if (DRIVER.get() != null) {
            throw new IllegalStateException("A WebDriver is already created for this thread");
        }

        String browser = ConfigReader.getProperty("browser");

        if (browser == null || browser.isBlank()) {
            throw new IllegalArgumentException(
                    "Browser is not configured in config.properties"
            );
        }

        WebDriver driver;
        if (browser.equalsIgnoreCase("chrome")) {
            ChromeOptions options = new ChromeOptions();
            if (Boolean.parseBoolean(ConfigReader.getProperty("headless"))) {
                options.addArguments("--headless=new", "--no-sandbox",
                        "--disable-dev-shm-usage");
            }
            driver = new ChromeDriver(options);
        } else {
            throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        DRIVER.set(driver);
        return driver;
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("No WebDriver has been created for this thread");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            DRIVER.remove();
        }
    }
}
