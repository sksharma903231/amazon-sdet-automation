package com.sksharma.automation.factory;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public class MethodFactory {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

    private final WebDriver driver;

    public MethodFactory(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "WebDriver must not be null");
    }

    public WebElement findElement(By locator) {
        requireLocator(locator);
        return perform("find element", locator, () -> driver.findElement(locator));
    }

    public void waitForVisibility(By locator) {
        requireLocator(locator);
        perform("wait for visibility", locator, () ->
                waitForVisibleElement(locator));
    }

    public void waitForClickability(By locator) {
        requireLocator(locator);
        perform("wait for clickability", locator, () ->
                waitForClickableElement(locator));
    }

    public void click(By locator) {
        requireLocator(locator);
        perform("click", locator, () -> {
            waitForClickableElement(locator).click();
            return null;
        });
    }

    public void type(By locator, String text) {
        requireLocator(locator);
        if (text == null) {
            throw new IllegalArgumentException("Text to type must not be null");
        }
        perform("type text", locator, () -> {
            WebElement element = waitForClickableElement(locator);
            element.clear();
            element.sendKeys(text);
            return null;
        });
    }

    public void submit(By locator) {
        requireLocator(locator);
        perform("submit form", locator, () -> {
            waitForClickableElement(locator).submit();
            return null;
        });
    }

    public String getText(By locator) {
        requireLocator(locator);
        return perform("read text", locator, () ->
                waitForVisibleElement(locator).getText());
    }

    /**
     * Returns false when the locator is absent or its element is hidden.
     * Other WebDriver failures, including stale-element and session errors,
     * propagate with operation and locator context.
     */
    public boolean isDisplayed(By locator) {
        requireLocator(locator);
        return perform("check display state", locator, () -> {
            try {
                return driver.findElement(locator).isDisplayed();
            } catch (NoSuchElementException e) {
                return false;
            }
        });
    }

    public boolean isEnabled(By locator) {
        requireLocator(locator);
        return perform("check enabled state", locator, () ->
                driver.findElement(locator).isEnabled());
    }

    public void navigateTo(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }
        perform("navigate to URL " + url, null, () -> {
            driver.get(url);
            return null;
        });
    }

    public String getCurrentUrl() {
        return perform("read current URL", null, driver::getCurrentUrl);
    }

    public void refresh() {
        perform("refresh current page", null, () -> {
            driver.navigate().refresh();
            return null;
        });
    }

    public void navigateBack() {
        perform("navigate back", null, () -> {
            driver.navigate().back();
            return null;
        });
    }

    public String getPageTitle() {
        return perform("read page title", null, driver::getTitle);
    }

    private WebElement waitForClickableElement(By locator) {
        return new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    private WebElement waitForVisibleElement(By locator) {
        return new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private <T> T perform(String operation, By locator, Supplier<T> action) {
        try {
            return action.get();
        } catch (WebDriverException e) {
            e.addInfo("Operation", operation);
            if (locator != null) {
                e.addInfo("Locator", locator.toString());
            }
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalStateException(context(operation, locator), e);
        }
    }

    private String context(String operation, By locator) {
        return locator == null
                ? "Failed to " + operation
                : "Failed to " + operation + " for locator: " + locator;
    }

    private void requireLocator(By locator) {
        if (locator == null) {
            throw new IllegalArgumentException("Locator must not be null");
        }
    }
}
