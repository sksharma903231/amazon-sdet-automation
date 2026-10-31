package com.sksharma.automation.pages;

import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.elements.AmazonHomeElements;
import org.openqa.selenium.WebDriver;

public class AmazonHomePage extends BasePage {

    public AmazonHomePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        methods.navigateTo(ConfigReader.getProperty("base.url"));
    }

    public void waitForSearchBoxReady() {
        methods.waitForVisibility(AmazonHomeElements.SEARCH_BOX);
        methods.waitForClickability(AmazonHomeElements.SEARCH_BOX);
    }

    public boolean isSearchBoxDisplayed() {
        return methods.isDisplayed(AmazonHomeElements.SEARCH_BOX);
    }

    public boolean isSearchBoxEnabled() {
        return methods.isEnabled(AmazonHomeElements.SEARCH_BOX);
    }

    public AmazonSearchResultsPage searchFor(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            throw new IllegalArgumentException("Search term must not be null or blank");
        }
        methods.type(AmazonHomeElements.SEARCH_BOX, searchTerm);
        methods.submit(AmazonHomeElements.SEARCH_BOX);
        return new AmazonSearchResultsPage(driver);
    }

    public String getPageTitle() {
        return methods.getPageTitle();
    }

    public String getCurrentUrl() {
        return methods.getCurrentUrl();
    }
}
