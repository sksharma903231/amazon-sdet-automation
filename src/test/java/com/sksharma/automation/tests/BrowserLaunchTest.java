package com.sksharma.automation.tests;
import org.testng.annotations.Listeners;
import com.sksharma.automation.listeners.ExtentReportListener;
import com.sksharma.automation.pages.AmazonHomePage;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.apache.logging.log4j.Logger;
import com.sksharma.automation.utils.LoggerUtils;



@Listeners(ExtentReportListener.class)
public class BrowserLaunchTest extends BaseTest {
    private static final Logger logger =
            LoggerUtils.getLogger(BrowserLaunchTest.class);

    @Test
    public void shouldOpenAmazonIndia() {
        logger.info("Checking that Amazon India opened successfully.");
        AmazonHomePage amazonHomePage =new AmazonHomePage(driver);
        String currentUrl = amazonHomePage.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith("https://www.amazon.in"),
                "The browser should open Amazon India. Actual URL: "+ currentUrl);
    }

}