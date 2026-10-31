package com.sksharma.automation.tests;

import com.sksharma.automation.dataproviders.AmazonSearchDataProvider;
import com.sksharma.automation.pages.AmazonHomePage;
import com.sksharma.automation.pages.AmazonSearchResultsPage;
import com.sksharma.automation.reporting.TestLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AmazonSearchTest extends BaseTest {

    @Test(dataProvider = "amazonSearchTerms",
            dataProviderClass = AmazonSearchDataProvider.class)
    public void shouldDisplaySearchResultsForSearchTerm(String searchTerm) {
        TestLogger.info("Starting Amazon India search for: " + searchTerm);

        AmazonHomePage homePage = new AmazonHomePage(driver);
        homePage.open();
        homePage.waitForSearchBoxReady();

        TestLogger.info("Searching Amazon India for: " + searchTerm);
        AmazonSearchResultsPage resultsPage = homePage.searchFor(searchTerm);
        resultsPage.waitForResultsPageReady();

        Assert.assertTrue(resultsPage.hasResults(),
                "Expected Amazon search results for: " + searchTerm);

        String firstResultTitle = resultsPage.getFirstResultTitle();
        Assert.assertNotNull(firstResultTitle,
                "The first result title should not be null for search term: "
                        + searchTerm);
        Assert.assertFalse(firstResultTitle.isBlank(),
                "The first result title should not be blank for search term: "
                        + searchTerm);

        TestLogger.pass("Amazon India returned a titled result for: " + searchTerm);
    }
}
