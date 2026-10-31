package com.sksharma.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AmazonHomePage extends BasePage {

    private static final By SEARCH_BOX = By.id("twotabsearchtextbox");

    public AmazonHomePage(WebDriver driver) {
        super(driver);
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public void waitForSearchBoxReady() {
        waitForElementVisible(SEARCH_BOX);
        waitForElementClickable(SEARCH_BOX);
    }
}
