package com.sksharma.automation.tests;

import com.sksharma.automation.pages.AmazonHomePage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AmazonHomePageTest extends BaseTest {

    @Test
    public void shouldShowSearchInput() {
        AmazonHomePage homePage = openHomePage();
        Assert.assertTrue(homePage.isSearchBoxDisplayed(),
                "Amazon homepage search input should be visible.");
    }

    @Test
    public void shouldEnableSearchInput() {
        AmazonHomePage homePage = openHomePage();
        Assert.assertTrue(homePage.isSearchBoxEnabled(),
                "Amazon homepage search input should be enabled.");
    }

    private AmazonHomePage openHomePage() {
        AmazonHomePage homePage = new AmazonHomePage(driver);
        homePage.open();
        homePage.waitForSearchBoxReady();
        return homePage;
    }
}
