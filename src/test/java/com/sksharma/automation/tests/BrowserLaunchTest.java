
package com.sksharma.automation.tests;

import com.sksharma.automation.pages.AmazonHomePage;
import com.sksharma.automation.reporting.TestLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BrowserLaunchTest extends BaseTest {

    @Test
    public void shouldOpenAmazonIndia() {

        TestLogger.info("Starting Amazon India homepage validation.");

        AmazonHomePage amazonHomePage = new AmazonHomePage(driver);

        String currentUrl = amazonHomePage.getCurrentUrl();

        TestLogger.info("Actual browser URL: " + currentUrl);

        boolean isCorrectUrl =
                currentUrl.startsWith("https://www.amazon.in");

        Assert.assertTrue(
                isCorrectUrl,
                "Expected Amazon India URL, but found: " + currentUrl
        );

        TestLogger.pass("Amazon India homepage URL verified successfully.");
    }
}
