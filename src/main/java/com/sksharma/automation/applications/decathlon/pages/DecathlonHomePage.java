package com.sksharma.automation.applications.decathlon.pages;

import com.sksharma.automation.applications.decathlon.elements.DecathlonSearchElements;
import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.pages.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.StaleElementReferenceException;

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
        WebDriverWait wait = new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getIntProperty("wait.timeout.seconds")));
        // The home header is present before its category-card skeletons clear and the
        // client navigation becomes interactive. Wait for that visible ready state.
        wait.until(d -> !d.findElements(org.openqa.selenium.By.cssSelector(
                        "[data-test-id='shop-by-category-card:container']")).isEmpty()
                && d.findElements(org.openqa.selenium.By.cssSelector(
                        "[data-test-id='shop-by-category-card:container'] "
                                + "[data-test-id='banner-card-skeleton:container']"))
                .isEmpty());
        WebElement menNavigation = wait.until(ExpectedConditions.visibilityOfElementLocated(
                DecathlonSearchElements.MEN_CATEGORY_NAV));
        new Actions(driver).moveToElement(menNavigation).perform();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                DecathlonSearchElements.MEN_FOOTWEAR_GROUP));
        WebElement category = wait.until(ExpectedConditions.refreshed(
                ExpectedConditions.elementToBeClickable(
                        DecathlonSearchElements.MEN_RUNNING_SHOES_LINK)));
        category.click();
        wait.until(d -> {
            try {
                return d.findElement(org.openqa.selenium.By.tagName("body")).getText()
                        .contains("Men Running Shoes");
            } catch (StaleElementReferenceException stalePage) {
                return false;
            }
        });
        DecathlonSearchPage listing = new DecathlonSearchPage(driver);
        listing.waitForProducts(product -> !product.href().isBlank());
        return listing;
    }
}
