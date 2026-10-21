package com.sksharma.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class BrowserLaunchTest extends BaseTest{
    @Test
    public void shouldOpemExamplewebsite(){

        String actualTitle = driver.getTitle();
        Assert.assertEquals(actualTitle,"Example Domain");

    }
    @Test
    public void shouldHaveCorrectPageTitle() {


        Assert.assertEquals(
                driver.getTitle(),
                "Example Domain"
        );
    }
}