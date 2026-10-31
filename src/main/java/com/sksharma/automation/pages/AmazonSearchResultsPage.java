package com.sksharma.automation.pages;

import com.sksharma.automation.elements.AmazonSearchResultsElements;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class AmazonSearchResultsPage extends BasePage {

    public AmazonSearchResultsPage(WebDriver driver) {
        super(driver);
    }

    public void waitForResultsPageReady() {
        try {
            methods.waitForVisibility(AmazonSearchResultsElements.RESULT_CARDS);
        } catch (TimeoutException resultsTimeout) {
            throw new TimeoutException(
                    "Timed out waiting for Amazon search result cards; the page state "
                            + "could not be confirmed because the no-results indicator "
                            + "has not been verified.",
                    resultsTimeout
            );
        }
    }

    public boolean hasResults() {
        return methods.isDisplayed(AmazonSearchResultsElements.RESULT_CARDS);
    }

    public String getFirstResultTitle() {
        return methods.getText(AmazonSearchResultsElements.FIRST_RESULT_TITLE);
    }
}
