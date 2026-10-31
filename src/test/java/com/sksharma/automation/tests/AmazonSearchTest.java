package com.sksharma.automation.tests;

import com.sksharma.automation.dataproviders.AmazonSearchDataProvider;
import com.sksharma.automation.pages.AmazonHomePage;
import com.sksharma.automation.pages.AmazonSearchResultsPage;
import com.sksharma.automation.reporting.TestLogger;
import com.sksharma.automation.utils.SearchTextNormalizer;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

        String decodedUrl = URLDecoder.decode(resultsPage.getCurrentUrl(),
                StandardCharsets.UTF_8);
        Assert.assertTrue(decodedUrl.toLowerCase().contains("k="
                        + searchTerm.toLowerCase()),
                "Expected the results URL to retain search term '" + searchTerm
                        + "', but found: " + resultsPage.getCurrentUrl());

        Assert.assertTrue(resultsPage.hasResults(),
                "Expected Amazon search results for: " + searchTerm);

        String firstResultTitle = resultsPage.getFirstResultTitle();
        Assert.assertNotNull(firstResultTitle,
                "The first result title should not be null for search term: "
                        + searchTerm);
        Assert.assertFalse(firstResultTitle.isBlank(),
                "The first result title should not be blank for search term: "
                        + searchTerm);

        java.util.Set<String> titleTerms =
                SearchTextNormalizer.meaningfulTerms(firstResultTitle);
        String primaryProductTerm =
                SearchTextNormalizer.primaryProductTerm(searchTerm);
        Assert.assertTrue(titleTerms.contains(primaryProductTerm),
                "Expected the first result title to identify the query's primary "
                        + "product term '" + primaryProductTerm + "' for query '"
                        + searchTerm
                        + "'; actual title: '" + firstResultTitle + "'. This is "
                        + "lexical evidence only and does not establish semantic relevance. "
                        + "Normalized product term: " + primaryProductTerm
                        + "; title terms: "
                        + SearchTextNormalizer.meaningfulTerms(firstResultTitle));

        TestLogger.pass("Amazon India returned a titled result with query-term "
                + "evidence for: " + searchTerm);
    }

    @Test
    public void shouldRetainSearchStateAfterRefresh() {
        AmazonHomePage homePage = new AmazonHomePage(driver);
        homePage.open();
        homePage.waitForSearchBoxReady();
        AmazonSearchResultsPage resultsPage = homePage.searchFor("laptop");
        resultsPage.waitForResultsPageReady();
        String searchUrl = resultsPage.getCurrentUrl();

        resultsPage.refresh();
        resultsPage.waitForResultsPageReady();

        Assert.assertEquals(resultsPage.getCurrentUrl(), searchUrl,
                "Refreshing search results should preserve the search URL.");
        Assert.assertTrue(resultsPage.hasResults(),
                "Search results should remain visible after refresh.");
    }

    @Test
    public void shouldReturnToHomepageWithBrowserBack() {
        AmazonHomePage homePage = new AmazonHomePage(driver);
        homePage.open();
        homePage.waitForSearchBoxReady();
        AmazonSearchResultsPage resultsPage = homePage.searchFor("headphones");
        resultsPage.waitForResultsPageReady();

        resultsPage.goBack();
        homePage.waitForSearchBoxReady();

        Assert.assertTrue(homePage.isSearchBoxDisplayed(),
                "Browser back should return to a page with the search input.");
    }
}
