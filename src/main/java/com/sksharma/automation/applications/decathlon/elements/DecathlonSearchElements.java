package com.sksharma.automation.applications.decathlon.elements;

import org.openqa.selenium.By;

/** Locators verified against the Decathlon India search DOM. */
public final class DecathlonSearchElements {
    private DecathlonSearchElements() { }

    public static final By SEARCH_INPUT = By.cssSelector("input[type='search']");
    public static final By PRODUCT_LINKS = By.cssSelector("a[href*='/p/']");
    public static final By FILTER_BUTTONS = By.cssSelector("button,[role='button']");
    public static final By BRAND_CHOICES = By.cssSelector(
            "input[type='checkbox'],input[type='radio']");
    public static final By SORT_OPTIONS = By.cssSelector("button,[role='option'],[role='menuitem'],[role='menuitemradio']");
    public static final By MEN_CATEGORY_NAV = By.cssSelector("[data-test-id='navigation-category-Men']");
    public static final By MEN_RUNNING_SHOES_LINK =
            By.cssSelector("a[href*='/c/men-running-shoes']");
    public static final By PRODUCT_HEADING = By.cssSelector("h1");
    public static final By PRODUCT_CONTAINER = By.cssSelector("article.pdp-container");
}
