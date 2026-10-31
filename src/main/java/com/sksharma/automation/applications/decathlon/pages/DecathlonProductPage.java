package com.sksharma.automation.applications.decathlon.pages;

import com.sksharma.automation.applications.decathlon.elements.DecathlonSearchElements;
import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.pages.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class DecathlonProductPage extends BasePage {
    public DecathlonProductPage(WebDriver driver) {
        super(driver);
        new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getIntProperty("wait.timeout.seconds"))).until(
                ExpectedConditions.visibilityOfElementLocated(
                        DecathlonSearchElements.PRODUCT_HEADING));
    }

    public String title() {
        return methods.getText(DecathlonSearchElements.PRODUCT_HEADING);
    }

    public String productDetailsText() {
        return methods.getText(DecathlonSearchElements.PRODUCT_CONTAINER);
    }
}
