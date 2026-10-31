package com.sksharma.automation.applications.decathlon.pages;

import com.sksharma.automation.applications.decathlon.elements.DecathlonSearchElements;
import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.pages.BasePage;
import org.openqa.selenium.WebDriver;

public final class DecathlonHomePage extends BasePage {
    public DecathlonHomePage(WebDriver driver) {
        super(driver);
    }

    public DecathlonHomePage open() {
        methods.navigateTo(ConfigReader.getProperty("base.url"));
        methods.waitForVisibility(DecathlonSearchElements.SEARCH_INPUT);
        return this;
    }

    public DecathlonSearchPage searchFor(String query) {
        DecathlonSearchPage results = new DecathlonSearchPage(driver);
        results.search(query);
        return results;
    }
}
