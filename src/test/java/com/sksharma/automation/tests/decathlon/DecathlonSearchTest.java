package com.sksharma.automation.tests.decathlon;

import com.sksharma.automation.applications.decathlon.pages.DecathlonProductPage;
import com.sksharma.automation.applications.decathlon.pages.DecathlonHomePage;
import com.sksharma.automation.applications.decathlon.pages.DecathlonSearchPage;
import com.sksharma.automation.config.ConfigReader;
import com.sksharma.automation.dataproviders.DecathlonSearchData;
import com.sksharma.automation.tests.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.WebElement;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

public class DecathlonSearchTest extends BaseTest {
    private static final String QUERY = "running shoes";
    private static final Pattern PRICE = Pattern.compile("₹\\s*[\\d,]+");

    @Test(groups = "search", description = "DEC-DISC-01: search returns identifiable priced product links")
    public void searchShowsProductNamesPricesAndLinks() {
        DecathlonHomePage home = new DecathlonHomePage(driver).open();
        DecathlonSearchPage page = home.searchFor(QUERY);
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

    @DataProvider(name = "runningSearchQueries")
    public Object[][] runningSearchQueries() {
        return DecathlonSearchData.validQueries();
    }

    @Test(dataProvider = "runningSearchQueries", groups = "search",
            description = "DEC-DISC-02: common case and partial query variations return relevant products")
    public void searchQueryVariationsReturnRelevantProducts(String query) {
        DecathlonSearchPage page = new DecathlonHomePage(driver).open().searchFor(query);
        var products = page.productResults();
        Assert.assertFalse(products.isEmpty(), "A valid running query should return products: " + query);
        Assert.assertTrue(driver.getTitle().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)),
                "Search title should preserve the query evidence");
        Assert.assertTrue(products.stream().anyMatch(product -> PRICE.matcher(product.text()).find()),
                "At least one result should expose a displayed price");
        Assert.assertTrue(products.stream().allMatch(product -> product.href().contains("/p/")),
                "Every returned product should link to a product details route");
    }

    @Test(groups = "search", description = "DEC-DISC-03: unmatched search shows the site's no-results message")
    public void unmatchedSearchShowsNoResultsState() {
        String query = DecathlonSearchData.noResultsQuery();
        DecathlonSearchPage page = new DecathlonHomePage(driver).open().searchFor(query);
        Assert.assertTrue(driver.getTitle().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)),
                "No-results route should retain the searched term");
        Assert.assertTrue(driver.findElement(org.openqa.selenium.By.tagName("body")).getText()
                        .contains("Sorry, No Results Found"),
                "The live application should render its explicit no-results message");
        Assert.assertTrue(page.productResults().stream()
                        .noneMatch(product -> product.text().toLowerCase(Locale.ROOT)
                                .contains(query.toLowerCase(Locale.ROOT))),
                "The unmatched query should not be represented as a matching product result");
    }

    @Test(groups = "filters", description = "DEC-FILTER-01: selected Adidas brand is reflected by the listing")
    public void adidasBrandFilterChangesAndConstrainsListing() {
        DecathlonHomePage home = new DecathlonHomePage(driver).open();
        DecathlonSearchPage page = home.searchFor(QUERY);
        String before = page.productResults().get(0).href();
        page.openBrandFilter();
        String selected = page.selectBrand("Adidas");
        Assert.assertNotNull(selected, "Observed Adidas brand option should be selectable");
        page.waitForProducts(product -> (product.text() + " " + product.imageAlt())
                .toLowerCase(Locale.ROOT).contains("adidas"));
        var products = page.productResults();
        Assert.assertFalse(products.isEmpty(), "Brand filter should leave product results");
        Assert.assertTrue(products.stream().allMatch(product ->
                        (product.text() + " " + product.imageAlt())
                                .toLowerCase(Locale.ROOT).contains("adidas")),
                "Every visible product result should identify the selected Adidas brand");
        Assert.assertNotEquals(products.get(0).href(), before,
                "Filtered listing should differ from its original first product");
    }

    @Test(groups = "filters", description = "DEC-FILTER-04: gender filter constrains results and clear restores the listing")
    public void genderFilterConstrainsListingAndCanBeCleared() {
        DecathlonSearchPage page = new DecathlonHomePage(driver).open().searchFor(QUERY);
        var original = page.productResults();
        Assert.assertTrue(original.stream().anyMatch(product -> !product.text().matches("(?s).*\\bMen\\b.*")),
                "Unfiltered listing should include at least one result outside the selected gender");
        page.openFilter("Gender");
        String selected = page.selectFacet("Men");
        Assert.assertTrue(selected.matches("(?s).*\\bMen\\b.*"),
                "The selected filter should be the observed Men option");
        page.waitForProducts(product -> product.text().matches("(?s).*\\bMen\\b.*"));
        var filtered = page.productResults();
        Assert.assertFalse(filtered.isEmpty(), "Gender filter should retain matching products");
        Assert.assertTrue(filtered.stream().allMatch(product -> product.text().matches("(?s).*\\bMen\\b.*")),
                "Every displayed result should identify the selected Men category");
        page.clearFilters();
        Assert.assertFalse(page.productResults().isEmpty(), "Clearing filters should restore product results");
        Assert.assertTrue(driver.findElements(org.openqa.selenium.By.cssSelector("input[type=checkbox]"))
                        .stream().filter(WebElement::isDisplayed).noneMatch(WebElement::isSelected),
                "Clearing filters should clear selected checkbox state");
    }

    @Test(groups = "sorting", description = "DEC-SORT-01: low-to-high sorting orders displayed sale prices numerically")
    public void priceLowToHighSortOrdersDisplayedPricesNumerically() {
        DecathlonSearchPage page = new DecathlonHomePage(driver).open().searchFor(QUERY);
        page.sortByPrice(true);
        assertPriceOrder(page.productResults(), true);
    }

    private static void assertPriceOrder(java.util.List<DecathlonSearchPage.ProductResult> products,
                                         boolean ascending) {
        var prices = products.stream().map(product -> PRICE.matcher(product.text()).results()
                .map(match -> Integer.parseInt(match.group().replaceAll("[^0-9]", "")))
                .findFirst().orElseThrow(() -> new AssertionError("Displayed product has no current price")))
                .toList();
        Assert.assertTrue(prices.size() > 1, "Price sorting needs multiple displayed products");
        for (int index = 1; index < prices.size(); index++) {
            Assert.assertTrue(ascending ? prices.get(index - 1) <= prices.get(index)
                            : prices.get(index - 1) >= prices.get(index),
                    "Displayed sale prices should be numerically ordered: " + prices);
        }
    }

    @Test(groups = "product", description = "DEC-PDP-01: selected search result identity and displayed price persist")
    public void productDetailsMatchSelectedSearchResult() {
        DecathlonHomePage home = new DecathlonHomePage(driver).open();
        DecathlonSearchPage page = home.searchFor(QUERY);
        var selected = page.productResults().get(0);
        String resultTitle = selected.text().split("\\R")[0].trim();
        String resultPrice = PRICE.matcher(selected.text()).results()
                .map(match -> match.group().replaceAll("\\s", ""))
                .findFirst().orElseThrow(() -> new AssertionError("Result price not displayed"));
        String productUrl = selected.href();
        page.openProduct(productUrl);
        Assert.assertTrue(driver.getCurrentUrl().contains("/p/"),
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

    @Test(groups = "navigation", description = "DEC-NAV-01: product navigation supports browser back, refresh, and forward")
    public void productNavigationSurvivesBackRefreshAndForward() {
        DecathlonSearchPage listing = new DecathlonHomePage(driver).open().searchFor(QUERY);
        var product = listing.productResults().get(0);
        String listingUrl = driver.getCurrentUrl();

        listing.openProduct(product.href());
        Assert.assertTrue(driver.getCurrentUrl().contains("/p/"),
                "Selecting a result should open its product details");

        driver.navigate().back();
        var wait = new WebDriverWait(driver,
                Duration.ofSeconds(ConfigReader.getIntProperty("wait.timeout.seconds")));
        wait.until(d -> d.getCurrentUrl().contains("/search"));
        wait.until(d -> !listing.productResults().isEmpty());
        Assert.assertFalse(listing.productResults().isEmpty(),
                "Browser back should restore a usable product listing");
        driver.navigate().refresh();
        wait.until(d -> !listing.productResults().isEmpty());
        Assert.assertTrue(driver.getCurrentUrl().contains("/search"),
                "Refreshing should retain the search results route");
        Assert.assertEquals(URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8),
                URLDecoder.decode(listingUrl, StandardCharsets.UTF_8),
                "Refresh should preserve the current search URL");

        driver.navigate().forward();
        wait.until(d -> d.getCurrentUrl().contains("/p/"));
        Assert.assertTrue(driver.getCurrentUrl().startsWith(product.href()),
                "Browser forward should restore the same product identity");
    }

    @Test(groups = "navigation", description = "DEC-NAV-02: homepage Men menu opens the Running Shoes category listing")
    public void mainNavigationOpensMenRunningShoesCategory() {
        DecathlonSearchPage listing = new DecathlonHomePage(driver).open()
                .openMenRunningShoesCategory();
        Assert.assertTrue(driver.findElement(org.openqa.selenium.By.tagName("body")).getText()
                        .contains("Men Running Shoes"),
                "The selected homepage category should be identified in the rendered page");
        var products = listing.productResults();
        Assert.assertFalse(products.isEmpty(), "Category route should expose product listing links");
        Assert.assertTrue(products.stream().allMatch(product -> product.text().toLowerCase(Locale.ROOT)
                        .contains("running") || product.text().toLowerCase(Locale.ROOT).contains("shoe")),
                "Products in the category should provide running/shoe relevance evidence");
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
