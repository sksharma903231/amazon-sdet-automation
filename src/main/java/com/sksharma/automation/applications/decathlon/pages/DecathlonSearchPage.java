package com.sksharma.automation.applications.decathlon.pages;

import com.sksharma.automation.applications.decathlon.elements.DecathlonSearchElements;
import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.pages.BasePage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class DecathlonSearchPage extends BasePage {
    private final WebDriverWait wait;

    public DecathlonSearchPage(WebDriver driver) {
        super(driver);
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getIntProperty("wait.timeout.seconds")));
    }

    public void search(String query) {
        WebElement input = wait.until(driver -> {
            WebElement candidate = driver.findElement(DecathlonSearchElements.SEARCH_INPUT);
            if (!candidate.isDisplayed() || !candidate.isEnabled()) return null;
            boolean unobstructed = Boolean.TRUE.equals(
                    ((JavascriptExecutor) driver).executeScript("""
                            const target = arguments[0];
                            const rect = target.getBoundingClientRect();
                            const top = document.elementFromPoint(
                              rect.left + rect.width / 2,
                              rect.top + rect.height / 2);
                            return top === target || target.contains(top);
                            """, candidate));
            return unobstructed ? candidate : null;
        });
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        input.sendKeys(query);
        wait.until(d -> {
            WebElement currentInput = d.findElement(DecathlonSearchElements.SEARCH_INPUT);
            return query.equals(currentInput.getAttribute("value"));
        });
        wait.until(ExpectedConditions.elementToBeClickable(
                DecathlonSearchElements.SEARCH_INPUT)).sendKeys(Keys.ENTER);
        wait.until(d -> d.getCurrentUrl().contains("/search")
                && (!productResults().isEmpty()
                || d.findElement(org.openqa.selenium.By.tagName("body")).getText()
                .contains("Sorry, No Results Found")));
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
        openFilter("Brand");
    }

    public void openFilter(String filterName) {
        wait.until(d -> d.findElements(DecathlonSearchElements.FILTER_BUTTONS).stream()
                .anyMatch(button -> button.isDisplayed() && button.isEnabled()
                        && button.getText().trim().equalsIgnoreCase(filterName)));
        driver.findElements(DecathlonSearchElements.FILTER_BUTTONS).stream()
                .filter(WebElement::isDisplayed).filter(WebElement::isEnabled)
                .filter(button -> button.getText().trim().equalsIgnoreCase(filterName))
                .findFirst().orElseThrow().click();
    }

    public String selectBrand(String brand) {
        return selectFacet(brand);
    }

    public String selectFacet(String option) {
        WebElement choice = wait.until(d -> d.findElements(DecathlonSearchElements.BRAND_CHOICES).stream()
                .filter(WebElement::isDisplayed).filter(WebElement::isEnabled)
                .filter(input -> brandLabel(input).toLowerCase().contains(option.toLowerCase()))
                .findFirst().orElse(null));
        String label = brandLabel(choice);
        choice.click();
        return label;
    }

    public void clearFilters() {
        driver.findElements(DecathlonSearchElements.FILTER_BUTTONS).stream()
                .filter(WebElement::isDisplayed).filter(WebElement::isEnabled)
                .filter(button -> button.getText().trim().equalsIgnoreCase("Clear all"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Clear all filter control is absent"))
                .click();
        wait.until(d -> !productResults().isEmpty());
    }

    public void sortByPrice(boolean ascending) {
        String option = ascending ? "Price (low → high)" : "Price (high → low)";
        String firstBefore = productResults().stream().findFirst().map(ProductResult::href).orElse("");
        driver.findElements(DecathlonSearchElements.FILTER_BUTTONS).stream()
                .filter(WebElement::isDisplayed).filter(WebElement::isEnabled)
                .filter(button -> button.getText().trim().matches("Most relevant|Price \\(.*\\)"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Sort control is absent"))
                .click();
        WebElement sortOption = wait.until(d -> d.findElements(DecathlonSearchElements.SORT_OPTIONS).stream()
                .filter(WebElement::isDisplayed)
                .filter(element -> element.getText().replaceAll("\\s+", " ").trim().equals(option))
                .findFirst().orElse(null));
        sortOption.click();
        wait.until(d -> !productResults().isEmpty()
                && (!firstBefore.equals(productResults().get(0).href())
                || driver.findElements(DecathlonSearchElements.FILTER_BUTTONS).stream()
                .anyMatch(button -> button.isDisplayed() && button.getText().trim().equals(option))));
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

    public void waitForProducts(Predicate<ProductResult> condition) {
        wait.until(d -> !productResults().isEmpty()
                && productResults().stream().allMatch(condition));
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
