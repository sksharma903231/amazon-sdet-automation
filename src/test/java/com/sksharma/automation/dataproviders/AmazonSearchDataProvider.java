package com.sksharma.automation.dataproviders;

import org.testng.annotations.DataProvider;

public class AmazonSearchDataProvider {

    @DataProvider(name = "amazonSearchTerms")
    public static Object[][] searchTerms() {
        return new Object[][]{
                {"laptop"},
                {"headphones"}
        };
    }
}
