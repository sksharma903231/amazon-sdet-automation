package com.sksharma.automation.driver;

import com.sksharma.automation.config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;


public class DriverFactory {

    public static WebDriver createDriver() {

        String browser = ConfigReader.getProperty("browser");

        if(browser ==null || browser.isBlank()){
            throw new IllegalArgumentException(
                    "Broser is not configured in config.properties"
            );
        }
        if (browser.equalsIgnoreCase("chrome")){
            return new ChromeDriver();
        }
        throw new IllegalArgumentException(
                "Unsupported browser: "+browser
        );

    }
}
