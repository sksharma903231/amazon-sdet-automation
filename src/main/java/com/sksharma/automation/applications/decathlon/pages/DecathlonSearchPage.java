package com.sksharma.automation.applications.decathlon.pages;

import com.sksharma.automation.applications.decathlon.elements.DecathlonSearchElements;
import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.pages.BasePage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class DecathlonSearchPage extends BasePage {
    private final WebDriverWait wait;

    public DecathlonSearchPage(WebDriver driver) {
        super(driver);
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getIntProperty("wait.timeout.seconds")));
    }

    public void search(String query) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(
                DecathlonSearchElements.SEARCH_INPUT));
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        input.sendKeys(query);
        wait.until(d -> query.equals(input.getAttribute("value")));
        input.sendKeys(Keys.ENTER);
        wait.until(d -> d.getCurrentUrl().contains("/search") && !productResults().isEmpty());
    }

    /** Reads fresh listing data in one script so SPA rerenders cannot stale element handles. */
    public List<ProductResult> productResults() {
        Object result = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("""
                return [...document.querySelectorAll("a[href*='/p/']")]
                  .filter(a=>a.offsetWidth||a.offsetHeight||a.getClientRects().length)
                  .map(a=>({href:a.href,text:(a.innerText||'').trim(),
                    imageAlt:(a.querySelector('img')?.alt||'').trim()}))
                  .filter(x=>x.text && x.href.includes('/p/'));
                """);
        if (!(result instanceof List<?> rows)) return List.of();
        return rows.stream().filter(Map.class::isInstance).map(Map.class::cast)
                .map(row -> new ProductResult(String.valueOf(row.get("href")),
                        String.valueOf(row.get("text")), String.valueOf(row.get("imageAlt"))))
                .toList();
    }

    public void openBrandFilter() {
        wait.until(d -> d.findElements(DecathlonSearchElements.FILTER_BUTTONS).stream()
                .anyMatch(button -> button.isDisplayed() && button.isEnabled()
                        && button.getText().trim().equalsIgnoreCase("Brand")));
        driver.findElements(DecathlonSearchElements.FILTER_BUTTONS).stream()
                .filter(WebElement::isDisplayed).filter(WebElement::isEnabled)
                .filter(button -> button.getText().trim().equalsIgnoreCase("Brand"))
                .findFirst().orElseThrow().click();
    }

    public String selectBrand(String brand) {
        WebElement choice = wait.until(d -> d.findElements(DecathlonSearchElements.BRAND_CHOICES).stream()
                .filter(WebElement::isDisplayed).filter(WebElement::isEnabled)
                .filter(input -> brandLabel(input).toLowerCase().contains(brand.toLowerCase()))
                .findFirst().orElse(null));
        String label = brandLabel(choice);
        choice.click();
        return label;
    }

    private String brandLabel(WebElement input) {
        Object value = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const e=arguments[0], l=(e.labels&&e.labels[0])||e.closest('label'); "
                        + "return (l?.innerText||e.parentElement?.innerText||e.value||'').trim();", input);
        return String.valueOf(value);
    }

    public void waitForProductListingChange(String previousFirstHref) {
        wait.until(d -> productResults().isEmpty()
                || !previousFirstHref.equals(productResults().get(0).href()));
        wait.until(d -> !productResults().isEmpty());
    }

    public void openProduct(String href) {
        WebElement link = wait.until(d -> d.findElements(DecathlonSearchElements.PRODUCT_LINKS)
                .stream().filter(WebElement::isDisplayed)
                .filter(element -> href.equals(element.getAttribute("href")))
                .findFirst().orElse(null));
        link.click();
        wait.until(d -> d.getCurrentUrl().contains("/p/"));
    }

    public record ProductResult(String href, String text, String imageAlt) { }
}
