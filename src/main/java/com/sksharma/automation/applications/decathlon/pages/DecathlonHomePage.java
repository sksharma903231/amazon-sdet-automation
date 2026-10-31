package com.sksharma.automation.applications.decathlon.pages;

import com.sksharma.automation.applications.decathlon.elements.DecathlonSearchElements;
import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.pages.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class DecathlonHomePage extends BasePage {
    public DecathlonHomePage(WebDriver driver) {
        super(driver);
    }

    public DecathlonHomePage open() {
        methods.navigateTo(ConfigReader.getProperty("base.url"));
        methods.waitForVisibility(DecathlonSearchElements.SEARCH_INPUT);
        return this;
    }

    public DecathlonSearchPage searchFor(String query) {
        DecathlonSearchPage results = new DecathlonSearchPage(driver);
        results.search(query);
        return results;
    }

    public DecathlonSearchPage openMenRunningShoesCategory() {
        methods.click(DecathlonSearchElements.MEN_CATEGORY_NAV);
        WebElement category = new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getIntProperty("wait.timeout.seconds")))
                .until(ExpectedConditions.elementToBeClickable(
                        DecathlonSearchElements.MEN_RUNNING_SHOES_LINK));
        category.click();
        new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getIntProperty("wait.timeout.seconds")))
                .until(d -> d.findElement(org.openqa.selenium.By.tagName("body")).getText()
                        .contains("Men Running Shoes"));
        DecathlonSearchPage listing = new DecathlonSearchPage(driver);
        listing.waitForProducts(product -> !product.href().isBlank());
        return listing;
    }
}
