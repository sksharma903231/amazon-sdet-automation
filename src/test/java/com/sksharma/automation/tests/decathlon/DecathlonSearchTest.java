package com.sksharma.automation.tests.decathlon;

import com.sksharma.automation.applications.decathlon.pages.DecathlonProductPage;
import com.sksharma.automation.applications.decathlon.pages.DecathlonSearchPage;
import com.sksharma.automation.tests.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.regex.Pattern;

public class DecathlonSearchTest extends BaseTest {
    private static final String QUERY = "running shoes";
    private static final Pattern PRICE = Pattern.compile("₹\\s*[\\d,]+");

    @Test(description = "DISC-IN-01: search returns identifiable priced product links")
    public void searchShowsProductNamesPricesAndLinks() {
        driver.get("https://www.decathlon.in/");
        DecathlonSearchPage page = new DecathlonSearchPage(driver);
        page.search(QUERY);
        Assert.assertTrue(driver.getCurrentUrl().contains("/search"),
                "Search should navigate to the product results route");
        var products = page.productResults();
        Assert.assertFalse(products.isEmpty(), "Search should return product links");
        var product = products.get(0);
        Assert.assertFalse(product.text().isBlank(), "Product name should be visible");
        Assert.assertTrue(product.text().contains("₹")
                        && PRICE.matcher(product.text()).find(),
                "Visible product listing should include a displayed price");
        Assert.assertTrue(product.href().contains("/p/"),
                "Result should link to a product detail route");
        Assert.assertTrue(driver.getTitle().toLowerCase(Locale.ROOT).contains("running shoes"),
                "Page title should retain the searched product terms");
    }

    @Test(description = "DISC-IN-02: selected Adidas brand is reflected by the listing")
    public void adidasBrandFilterChangesAndConstrainsListing() {
        driver.get("https://www.decathlon.in/");
        DecathlonSearchPage page = new DecathlonSearchPage(driver);
        page.search(QUERY);
        String before = page.productResults().get(0).href();
        page.openBrandFilter();
        String selected = page.selectBrand("Adidas");
        Assert.assertNotNull(selected, "Observed Adidas brand option should be selectable");
        page.waitForProductListingChange(before);
        var products = page.productResults();
        Assert.assertFalse(products.isEmpty(), "Brand filter should leave product results");
        Assert.assertTrue(products.stream().allMatch(product ->
                        (product.text() + " " + product.imageAlt())
                                .toLowerCase(Locale.ROOT).contains("adidas")),
                "Every visible product result should identify the selected Adidas brand");
        Assert.assertNotEquals(products.get(0).href(), before,
                "Filtered listing should differ from its original first product");
    }

    @Test(description = "PDP-IN-01: selected search result identity and displayed price persist")
    public void productDetailsMatchSelectedSearchResult() {
        driver.get("https://www.decathlon.in/");
        DecathlonSearchPage page = new DecathlonSearchPage(driver);
        page.search(QUERY);
        var selected = page.productResults().get(0);
        String resultTitle = selected.text().split("\\R")[0].trim();
        String resultPrice = PRICE.matcher(selected.text()).results()
                .map(match -> match.group().replaceAll("\\s", ""))
                .findFirst().orElseThrow(() -> new AssertionError("Result price not displayed"));
        String productUrl = selected.href();
        page.openProduct(productUrl);
        Assert.assertTrue(new org.openqa.selenium.support.ui.WebDriverWait(driver,
                java.time.Duration.ofSeconds(15)).until(d -> d.getCurrentUrl().contains("/p/")),
                "Selecting a product should navigate to its details");
        DecathlonProductPage details = new DecathlonProductPage(driver);
        String detailTitle = details.title();
        Assert.assertTrue(normalize(resultTitle).contains(normalize(detailTitle)),
                "Product details heading should identify the selected listing result");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(productUrl),
                "Product identity should remain consistent in the detail URL");
        Assert.assertTrue(details.productDetailsText().replaceAll("\\s", "")
                        .contains(resultPrice),
                "The selected listing price should be present in product details");
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
