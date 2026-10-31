package com.sksharma.automation.tests;

import com.sksharma.automation.pages.AmazonHomePage;
import com.sksharma.automation.pages.AmazonSearchResultsPage;
import com.sksharma.automation.reporting.TestLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AmazonSearchTest extends BaseTest {

    @Test
    public void shouldDisplaySearchResultsForLaptop() {
        TestLogger.info("Starting Amazon India laptop search.");

        AmazonHomePage homePage = new AmazonHomePage(driver);
        homePage.open();
        homePage.waitForSearchBoxReady();

        TestLogger.info("Searching Amazon India for laptop.");
        AmazonSearchResultsPage resultsPage = homePage.searchFor("laptop");
        resultsPage.waitForResultsPageReady();

        Assert.assertTrue(resultsPage.hasResults(),
                "Expected Amazon search results for laptop.");

        String firstResultTitle = resultsPage.getFirstResultTitle();
        Assert.assertNotNull(firstResultTitle,
                "The first Amazon search result title should not be null.");
        Assert.assertFalse(firstResultTitle.isBlank(),
                "The first Amazon search result title should not be blank.");

        TestLogger.pass("Amazon India returned a titled result for laptop.");
    }
}
