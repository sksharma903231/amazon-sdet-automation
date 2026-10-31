package com.sksharma.automation.elements;

import org.openqa.selenium.By;

public final class AmazonHomeElements {

    public static final By SEARCH_BOX =
            By.xpath("//*[@id='twotabsearchtextbox']");

    private AmazonHomeElements() {
        // Prevent object creation.
    }
}
