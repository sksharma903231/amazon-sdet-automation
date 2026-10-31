
package com.sksharma.automation.tests;

import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.pages.AmazonHomePage;
import com.sksharma.automation.reporting.TestLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BrowserLaunchTest extends BaseTest {

    @Test
    public void shouldOpenAmazonIndia() {

        TestLogger.info("Starting Amazon India homepage validation.");

        AmazonHomePage amazonHomePage = new AmazonHomePage(driver);
        amazonHomePage.open();
        amazonHomePage.waitForSearchBoxReady();

        String currentUrl = amazonHomePage.getCurrentUrl();
        String baseUrl = ConfigReader.getProperty("base.url");

        TestLogger.info("Actual browser URL: " + currentUrl);

        Assert.assertTrue(
                currentUrl.startsWith(baseUrl),
                "Expected current URL to start with configured base URL "
                        + baseUrl + ", but found: " + currentUrl
        );

        String pageTitle = amazonHomePage.getPageTitle();
        Assert.assertNotNull(pageTitle,
                "Amazon India page title should not be null.");
        Assert.assertFalse(pageTitle.isBlank(),
                "Amazon India page title should not be blank.");

        TestLogger.pass(
                "Amazon India homepage and primary search box are ready."
        );
    }
}
