package com.sksharma.automation.elements;

import org.openqa.selenium.By;

public final class AmazonSearchResultsElements {

    public static final By RESULT_CARDS =
            By.xpath(".//div[@data-component-type='s-search-result']");
    public static final By FIRST_RESULT_TITLE =
            By.xpath("(.//div[@data-component-type='s-search-result'])[1]//h2");

    private AmazonSearchResultsElements() {
        // Prevent object creation.
    }
}
