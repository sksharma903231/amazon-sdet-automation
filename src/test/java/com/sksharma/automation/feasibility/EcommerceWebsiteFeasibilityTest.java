package com.sksharma.automation.feasibility;

import com.sksharma.automation.tests.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bounded, evidence-only diagnostics. These are not application regression tests. */
public class EcommerceWebsiteFeasibilityTest extends BaseTest {

    private static final String MODE = System.getProperty("assessment.mode", "headed");
    private static final String RUN_ID = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
    private static final Path RUN_DIRECTORY = Path.of("reports", "feasibility",
            RUN_ID, MODE);

    private static final List<Site> SITES = List.of(
            new Site("flipkart", "https://www.flipkart.com/", "running shoes men"),
            new Site("croma", "https://www.croma.com/", "wireless headphones"),
            new Site("decathlon", "https://www.decathlon.in/", "running shoes")
    );

    @Test
    public void assessFlipkart() throws IOException { assess(SITES.get(0)); }

    @Test
    public void assessCroma() throws IOException { assess(SITES.get(1)); }

    @Test
    public void assessDecathlon() throws IOException { assess(SITES.get(2)); }

    private void assess(Site site) throws IOException {
        Files.createDirectories(RUN_DIRECTORY);
        List<Step> steps = new ArrayList<>();
        Path evidenceFile = RUN_DIRECTORY.resolve(site.id() + ".json");
        String screenshotPrefix = site.id();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));

        try {
            long started = System.currentTimeMillis();
            driver.get(site.url());
            boolean ready = waitForDocumentReady(Duration.ofSeconds(30));
            record(steps, "initial_access", ready ? "PASS" : "PARTIAL",
                    "document.readyState reached interactive/complete",
                    "elapsedMs=" + (System.currentTimeMillis() - started));
            capture(screenshotPrefix + "-landing.png");
            record(steps, "landing_observation", "PASS",
                    "Final URL=" + cleanUrl(driver.getCurrentUrl())
                            + "; title=" + driver.getTitle(),
                    domInventory());

            String blockingText = detectExplicitBlock();
            if (!blockingText.isBlank()) {
                record(steps, "access_restriction", "BLOCKED", blockingText,
                        "Flow stopped without attempting to bypass the page.");
                save(evidenceFile, site, steps);
                return;
            }

            dismissObviousBlockingOverlay();
            record(steps, "consent_overlay", "PASS",
                    "No modal blocked page interaction, or an explicit close control for a visible login/location panel was used.",
                    visibleDialogSummary());

            if (MODE.equalsIgnoreCase("headless")) {
                runSearch(site, steps, screenshotPrefix);
                save(evidenceFile, site, steps);
                return;
            }

            runSearch(site, steps, screenshotPrefix);
            if (steps.stream().noneMatch(step -> step.name().equals("search")
                    && step.status().equals("PASS"))) {
                save(evidenceFile, site, steps);
                return;
            }

            record(steps, "listing_observation", "PASS",
                    "Observed listing-related DOM after a successful search.",
                    listingInventory());
            inspectProductAndGuestCart(steps, screenshotPrefix);
            returnToSearchResults(steps);
            inspectFilterAndSort(steps);
        } catch (Exception e) {
            record(steps, "diagnostic_exception", "PARTIAL",
                    e.getClass().getSimpleName() + ": " + safeMessage(e),
                    "Current URL=" + safeCurrentUrl());
            try {
                capture(screenshotPrefix + "-exception.png");
            } catch (Exception ignored) {
                // Preserve the primary diagnostic result if screenshot capture fails.
            }
        } finally {
            save(evidenceFile, site, steps);
        }
    }

    private void returnToSearchResults(List<Step> steps) {
        try {
            for (int attempt = 0; attempt < 3; attempt++) {
                if (cleanUrl(driver.getCurrentUrl()).contains("/search")
                        && productHrefCount() > 0) {
                    record(steps, "return_to_results", "PASS",
                            "Returned to product listings after the product/cart inspection.",
                            "URL=" + cleanUrl(driver.getCurrentUrl()));
                    return;
                }
                driver.navigate().back();
                new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                        cleanUrl(d.getCurrentUrl()).contains("/search")
                                || d.getCurrentUrl().contains("/p/"));
            }
            record(steps, "return_to_results", "PARTIAL",
                    "Browser back did not restore the search listing in three transitions.",
                    "URL=" + safeCurrentUrl());
        } catch (Exception e) {
            record(steps, "return_to_results", "PARTIAL",
                    e.getClass().getSimpleName() + ": " + safeMessage(e),
                    "URL=" + safeCurrentUrl());
        }
    }

    private void runSearch(Site site, List<Step> steps, String screenshotPrefix) {
        List<WebElement> inputs = driver.findElements(By.cssSelector("input"));
        WebElement search = null;
        int bestScore = 0;
        for (WebElement input : inputs) {
            if (!visible(input) || !input.isEnabled()) continue;
            String type = attr(input, "type").toLowerCase(Locale.ROOT);
            String metadata = (attr(input, "name") + " " + attr(input, "id") + " "
                    + attr(input, "placeholder") + " " + attr(input, "aria-label"))
                    .toLowerCase(Locale.ROOT);
            if (type.equals("password") || type.equals("email")) continue;
            int score = (type.equals("search") ? 5 : 0)
                    + (metadata.contains("search") ? 3 : 0)
                    + (metadata.contains("product") ? 1 : 0);
            if (score > bestScore) {
                bestScore = score;
                search = input;
            }
        }
        if (search == null || bestScore == 0) {
            record(steps, "search", "NOT_TESTED",
                    "No visible, enabled search input with search-oriented attributes was found.",
                    inputSummary(inputs));
            return;
        }

        String beforeUrl = driver.getCurrentUrl();
        int beforeProductLinks = productHrefCount();
        String metadata = "type=" + attr(search, "type") + ", name="
                + attr(search, "name") + ", id=" + attr(search, "id")
                + ", placeholder=" + attr(search, "placeholder");
        try {
            search.click();
            search.clear();
            search.sendKeys(site.query());
            search.sendKeys(Keys.ENTER);
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
                    !d.getCurrentUrl().equals(beforeUrl)
                            || productHrefCount() > beforeProductLinks);
            waitForDocumentReady(Duration.ofSeconds(15));
            capture(screenshotPrefix + "-search.png");
            String searchStatus = productHrefCount() > 0 ? "PASS" : "PARTIAL";
            record(steps, "search", searchStatus,
                    "Search submitted for query='" + site.query() + "'; URL="
                            + cleanUrl(driver.getCurrentUrl()) + "; productLikeLinks="
                            + productHrefCount(),
                    metadata + "; " + listingInventory());
        } catch (Exception e) {
            record(steps, "search", "PARTIAL",
                    e.getClass().getSimpleName() + ": " + safeMessage(e),
                    metadata + "; URL=" + safeCurrentUrl());
        }
    }

    private void inspectFilterAndSort(List<Step> steps) {
        String controls = (String) ((JavascriptExecutor) driver).executeScript("""
                const visible = e => !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
                const nodes = [...document.querySelectorAll('button,a,label,[role=button],select,input')]
                  .filter(visible).map(e => ({tag:e.tagName, text:(e.innerText||e.value||'').trim().slice(0,100),
                    aria:e.getAttribute('aria-label'), name:e.getAttribute('name'), type:e.getAttribute('type'),
                    placeholder:e.getAttribute('placeholder')}));
                return JSON.stringify(nodes.filter(x => /brand|price|sort|filter|under|low to high/i
                  .test([x.text,x.aria,x.name,x.placeholder].join(' '))).slice(0,80));
                """);
        int sortOptions = driver.findElements(By.cssSelector("select option")).size();
        int checkboxes = driver.findElements(By.cssSelector(
                "input[type='checkbox'],input[type='radio']")).stream()
                .filter(this::visible).toList().size();
        record(steps, "filter_sort_observation", "PASS",
                "Candidates were inventoried; no control is considered verified from visibility alone.",
                "sortOptionCount=" + sortOptions + "; visibleCheckboxRadioCount="
                        + checkboxes + "; labeledCandidates=" + controls);

        // Interact only with a real option whose accessible text identifies price ascending.
        for (WebElement selectElement : driver.findElements(By.cssSelector("select"))) {
            if (!visible(selectElement) || !selectElement.isEnabled()) continue;
            List<WebElement> options = selectElement.findElements(By.tagName("option"));
            WebElement ascending = options.stream().filter(this::visible)
                    .filter(option -> option.getText().toLowerCase(Locale.ROOT)
                            .matches(".*price.*(low to high|ascending|lowest first).*"))
                    .findFirst().orElse(null);
            if (ascending != null) {
                String text = ascending.getText();
                try {
                    String before = listingInventory();
                    new Select(selectElement).selectByVisibleText(text);
                    new WebDriverWait(driver, Duration.ofSeconds(12)).until(d ->
                            !listingInventory().equals(before));
                    String after = listingInventory();
                    record(steps, "price_sort", "PARTIAL",
                            "Selected observed option '" + text
                                    + "' and listing content changed; numeric ordering was not extracted from comparable product cards.",
                            after);
                } catch (Exception e) {
                    record(steps, "price_sort", "PARTIAL",
                            "Observed sort option could not be verified after selection: "
                                    + e.getClass().getSimpleName() + ": " + safeMessage(e),
                            "Option text=" + text);
                }
                return;
            }
        }
        record(steps, "price_sort", "NOT_TESTED",
                "No visible native select option explicitly named price ascending was found.",
                "No guessed sort interaction was attempted.");

        interactWithBrandFilter(steps);
        inspectPriceFilter(steps);
    }

    private void interactWithBrandFilter(List<Step> steps) {
        WebElement brandButton = driver.findElements(By.cssSelector("button,[role='button']"))
                .stream().filter(this::visible).filter(WebElement::isEnabled)
                .filter(button -> button.getText().trim().equalsIgnoreCase("brand"))
                .findFirst().orElse(null);
        if (brandButton == null) {
            record(steps, "brand_filter", "NOT_TESTED",
                    "No visible brand control was identified.", "");
            return;
        }
        try {
            brandButton.click();
            new WebDriverWait(driver, Duration.ofSeconds(8)).until(d ->
                    driver.findElements(By.cssSelector("input[type='checkbox'],input[type='radio']"))
                            .stream().anyMatch(this::visible));
            List<WebElement> choices = driver.findElements(By.cssSelector(
                    "input[type='checkbox'],input[type='radio']")).stream()
                    .filter(this::visible).filter(WebElement::isEnabled).toList();
            if (choices.isEmpty()) {
                record(steps, "brand_filter", "PARTIAL",
                        "Brand section opened, but no selectable brand option was observed.",
                        listingInventory());
                return;
            }
            WebElement choice = choices.get(0);
            String brand = String.valueOf(((JavascriptExecutor) driver).executeScript("""
                    const e=arguments[0]; const label=(e.labels&&e.labels[0])||e.closest('label');
                    return (label?.innerText||e.parentElement?.innerText||e.getAttribute('value')||'').trim();
                    """, choice)).trim();
            String before = listingInventory();
            choice.click();
            new WebDriverWait(driver, Duration.ofSeconds(12)).until(d ->
                    !listingInventory().equals(before));
            String after = listingInventory();
            boolean brandVerified = verifyBrandOnProductTitles(brand);
            record(steps, "brand_filter", brandVerified ? "PASS" : "PARTIAL",
                    "Selected observed brand option '" + brand + "'; all visible product titles match the selected brand token="
                            + brandVerified + ".",
                    after);
        } catch (Exception e) {
            record(steps, "brand_filter", "PARTIAL",
                    "Brand control was observed but filter response was not confirmed: "
                            + e.getClass().getSimpleName() + ": " + safeMessage(e),
                    listingInventory());
        }
    }

    private boolean verifyBrandOnProductTitles(String brandLabel) {
        String brand = brandLabel.replaceAll("\\s+\\d+\\s*$", "").trim();
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("""
                const brand=String(arguments[0]).trim().toLowerCase();
                const links=[...document.querySelectorAll('a[href*="/p/"]')]
                  .filter(a=>a.offsetWidth||a.offsetHeight||a.getClientRects().length)
                  .map(a=>(a.innerText||a.querySelector('img')?.alt||'').trim()).filter(Boolean);
                const esc=brand.replace(/[.*+?^${}()|[\\]\\\\]/g,'\\\\$&');
                const pattern=new RegExp('(?:^|[^a-z0-9])'+esc+'(?:$|[^a-z0-9])','i');
                return links.length>0 && links.every(title=>pattern.test(title));
                """, brand));
    }

    private void inspectPriceFilter(List<Step> steps) {
        WebElement priceButton = driver.findElements(By.cssSelector("button,[role='button']"))
                .stream().filter(this::visible).filter(WebElement::isEnabled)
                .filter(button -> button.getText().trim().equalsIgnoreCase("price"))
                .findFirst().orElse(null);
        if (priceButton == null) {
            record(steps, "price_filter", "NOT_TESTED",
                    "No visible price filter control was identified.", "");
            return;
        }
        try {
            priceButton.click();
            String controls = (String) ((JavascriptExecutor) driver).executeScript("""
                    const visible=e=>!!(e.offsetWidth||e.offsetHeight||e.getClientRects().length);
                    return JSON.stringify([...document.querySelectorAll('input,button,[role=option],label')]
                      .filter(visible).map(e=>({tag:e.tagName,type:e.type||'',text:(e.innerText||e.value||'').trim().slice(0,100),
                        name:e.name||'',placeholder:e.placeholder||''})).filter(x=>/price|₹|under|above|minimum|maximum/i
                        .test([x.text,x.name,x.placeholder].join(' '))).slice(0,60));
                    """);
            record(steps, "price_filter", "PARTIAL",
                    "Opened the observed Price control; no interval was applied without verified range semantics.",
                    controls);
        } catch (Exception e) {
            record(steps, "price_filter", "PARTIAL",
                    "Price control was observed but could not be expanded: "
                            + e.getClass().getSimpleName(), "");
        }
    }

    private void inspectProductAndGuestCart(List<Step> steps, String screenshotPrefix) {
        List<WebElement> anchors = driver.findElements(By.cssSelector("a[href]"));
        WebElement productLink = anchors.stream().filter(this::visible)
                .filter(anchor -> !anchor.getText().isBlank())
                .filter(anchor -> productLikeUrl(attr(anchor, "href")))
                .findFirst().orElse(null);
        if (productLink == null) {
            record(steps, "product_details", "NOT_TESTED",
                    "No visible product-like link with a product path and readable text was found.",
                    "visibleAnchorCount=" + anchors.stream().filter(this::visible).count());
            record(steps, "guest_cart", "NOT_TESTED",
                    "A product details page was not reached.", "");
            return;
        }

        String selectedText = productLink.getText().trim();
        String selectedUrl = cleanUrl(attr(productLink, "href"));
        try {
            productLink.click();
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
                    d.getWindowHandles().size() > 1
                            || !cleanUrl(d.getCurrentUrl()).equals(selectedUrl));
            if (driver.getWindowHandles().size() > 1) {
                String current = driver.getWindowHandle();
                for (String handle : driver.getWindowHandles()) {
                    if (!handle.equals(current)) driver.switchTo().window(handle);
                }
            }
            waitForDocumentReady(Duration.ofSeconds(15));
            capture(screenshotPrefix + "-product-details.png");
            String detailText = bodyText();
            String h1 = driver.findElements(By.cssSelector("h1")).stream()
                    .filter(this::visible).map(WebElement::getText)
                    .filter(text -> !text.isBlank()).findFirst().orElse("");
            boolean identityMatches = !h1.isBlank()
                    && normalize(selectedText).contains(normalize(h1));
            record(steps, "product_details", identityMatches ? "PASS" : "PARTIAL",
                    "Selected result='" + selectedText + "'; detail URL="
                            + cleanUrl(driver.getCurrentUrl()) + "; h1='" + h1
                            + "'; selectedResultContainsDetailTitle=" + identityMatches + ".",
                    "selectedHref=" + selectedUrl + "; detailInfo=" + detailInventory(detailText));
            inspectGuestCart(steps, screenshotPrefix);
        } catch (Exception e) {
            record(steps, "product_details", "PARTIAL",
                    e.getClass().getSimpleName() + ": " + safeMessage(e),
                    "Selected result='" + selectedText + "'; target=" + selectedUrl);
            record(steps, "guest_cart", "NOT_TESTED",
                    "Product detail flow did not complete.", "");
        }
    }

    private void inspectGuestCart(List<Step> steps, String screenshotPrefix) {
        WebElement addButton = findAddToCartControl();
        WebElement displayedSize = findDisplayedSize();
        boolean sizeSelected = false;
        if ((addButton == null || !addButton.isEnabled()) && displayedSize != null) {
            String sizeLabel = displayedSize.getText().trim();
            try {
                displayedSize.click();
                sizeSelected = true;
                record(steps, "product_variant", "PASS",
                        "Selected an available displayed size before retrying the disabled add-to-cart control.",
                        "selectedSize=" + sizeLabel);
                addButton = new WebDriverWait(driver, Duration.ofSeconds(8))
                        .until(d -> {
                            WebElement candidate = findAddToCartControl();
                            return candidate != null && candidate.isEnabled() ? candidate : null;
                        });
            } catch (Exception e) {
                record(steps, "product_variant", "PARTIAL",
                        "Displayed size " + sizeLabel + " could not enable the add-to-cart control: "
                                + e.getClass().getSimpleName(), "");
            }
        }
        if (addButton == null) {
            record(steps, "guest_cart", "NOT_TESTED",
                    "No visible enabled add-to-cart control was identified on the product page.",
                    buttonSummary(driver.findElements(By.cssSelector(
                            "button,a,[role='button'],[tabindex]"))));
            return;
        }
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'})", addButton);
            String before = bodyText();
            addButton.click();
            new WebDriverWait(driver, Duration.ofSeconds(12)).until(d -> {
                String text = bodyText().toLowerCase(Locale.ROOT);
                return text.contains("please select a size")
                        || text.contains("added to cart") || text.contains("added to bag")
                        || d.getCurrentUrl().toLowerCase(Locale.ROOT).contains("cart")
                        || !bodyText().equals(before);
            });
            if (!sizeSelected
                    && bodyText().toLowerCase(Locale.ROOT).contains("please select a size")) {
                WebElement size = driver.findElements(By.cssSelector(
                                "button,[role='button'],label,[tabindex]"))
                        .stream().filter(this::visible).filter(WebElement::isEnabled)
                        .filter(element -> element.getText().trim().matches(
                                "(?i)(?:\\d+(?:\\.\\d+)?|\\d+\\s*-\\s*EU\\s*\\d+.*)"))
                        .findFirst().orElse(null);
                if (size == null) {
                    record(steps, "product_variant", "PARTIAL",
                            "Add-to-cart exposed a size requirement, but no enabled displayed size was selectable.",
                            buttonSummary(driver.findElements(By.cssSelector(
                                    "button,[role='button'],label,[tabindex]"))));
                    return;
                }
                String sizeLabel = size.getText().trim();
                size.click();
                record(steps, "product_variant", "PASS",
                        "Selected a displayed product size after the site requested a variant.",
                        "selectedSize=" + sizeLabel);
                WebElement retryAdd = findAddToCartControl();
                if (retryAdd == null) {
                    record(steps, "guest_cart_add", "PARTIAL",
                            "No add-to-cart control was available after selecting size " + sizeLabel + ".", "");
                    return;
                }
                retryAdd.click();
                new WebDriverWait(driver, Duration.ofSeconds(12)).until(d ->
                        bodyText().toLowerCase(Locale.ROOT).contains("added to cart")
                                || bodyText().toLowerCase(Locale.ROOT).contains("added to bag")
                                || d.getCurrentUrl().toLowerCase(Locale.ROOT).contains("cart"));
            }
            if (!cartAddConfirmed()) {
                record(steps, "guest_cart_add", "PARTIAL",
                        "Add control responded, but no cart confirmation or cart navigation was verified.",
                        "URL=" + cleanUrl(driver.getCurrentUrl()) + "; page=" + snippet(bodyText(), 900));
                return;
            }
            capture(screenshotPrefix + "-cart.png");
            record(steps, "guest_cart_add", "PASS",
                    "Guest add action produced cart-related confirmation or navigation.",
                    "URL=" + cleanUrl(driver.getCurrentUrl()) + "; " + cartInventory());
            openCartAndInspect(steps);
        } catch (Exception e) {
            record(steps, "guest_cart_add", "PARTIAL",
                    e.getClass().getSimpleName() + ": " + safeMessage(e),
                    "Authentication/challenge/page text=" + snippet(bodyText(), 900));
        }
    }

    private WebElement findAddToCartControl() {
        return driver.findElements(By.cssSelector(
                        "button,a,[role='button'],input[type='submit'],[tabindex]"))
                .stream().filter(this::visible)
                .filter(element -> buttonLabel(element).matches(
                        ".*(add to cart|add to bag|add to basket).*"))
                .findFirst().orElse(null);
    }

    private WebElement findDisplayedSize() {
        return driver.findElements(By.cssSelector("button,[role='button'],label,[tabindex]"))
                .stream().filter(this::visible).filter(WebElement::isEnabled)
                .filter(element -> element.getText().trim().matches(
                        "(?i)(?:\\d+(?:\\.\\d+)?|\\d+\\s*-\\s*EU\\s*\\d+.*)"))
                .findFirst().orElse(null);
    }

    private boolean cartAddConfirmed() {
        String text = bodyText().toLowerCase(Locale.ROOT);
        return text.contains("added to cart") || text.contains("added to bag")
                || driver.getCurrentUrl().toLowerCase(Locale.ROOT).contains("cart");
    }

    private void openCartAndInspect(List<Step> steps) {
        WebElement cartLink = driver.findElements(By.cssSelector("a[href],button,[role='button']"))
                .stream().filter(this::visible).filter(WebElement::isEnabled)
                .filter(element -> attr(element, "href").toLowerCase(Locale.ROOT).contains("cart")
                        || buttonLabel(element).matches(".*(view cart|go to cart|cart).*"))
                .findFirst().orElse(null);
        if (cartLink == null) {
            inspectCartControls(steps);
            return;
        }
        try {
            cartLink.click();
            new WebDriverWait(driver, Duration.ofSeconds(12)).until(d ->
                    d.getCurrentUrl().toLowerCase(Locale.ROOT).contains("cart")
                            || bodyText().toLowerCase(Locale.ROOT).contains("shopping cart"));
            capture("decathlon-cart.png");
            record(steps, "guest_cart_open", "PASS",
                    "Cart navigation completed without login or checkout.", cartInventory());
            inspectCartControls(steps);
        } catch (Exception e) {
            record(steps, "guest_cart_open", "PARTIAL",
                    e.getClass().getSimpleName() + ": " + safeMessage(e), cartInventory());
        }
    }

    private void inspectCartControls(List<Step> steps) {
        List<WebElement> qty = driver.findElements(By.cssSelector(
                "select[name*='quantity' i],input[name*='quantity' i],select[id*='quantity' i]"));
        List<WebElement> remove = driver.findElements(By.cssSelector(
                "button,a,[role='button']"));
        String removeLabels = remove.stream().filter(this::visible)
                .map(this::buttonLabel).filter(text -> text.contains("remove")
                        || text.contains("delete")).limit(10).toList().toString();
        record(steps, "guest_cart_controls", "PASS",
                "Cart controls inventoried; no quantity or removal action was assumed from a locator alone.",
                "quantityControls=" + qty.stream().filter(this::visible).count()
                        + "; removeLabels=" + removeLabels + "; " + cartInventory());
    }

    private boolean waitForDocumentReady(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d -> "interactive".equals(
                    ((JavascriptExecutor) d).executeScript("return document.readyState"))
                    || "complete".equals(((JavascriptExecutor) d)
                    .executeScript("return document.readyState")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String detectExplicitBlock() {
        String text = bodyText().toLowerCase(Locale.ROOT);
        for (String phrase : List.of("verify you are human", "access denied",
                "unusual traffic", "automated access", "robot check",
                "captcha", "security verification")) {
            if (text.contains(phrase)) return "Visible page text indicates restriction/challenge: " + phrase;
        }
        boolean captchaFrame = driver.findElements(By.cssSelector(
                "iframe[src*='captcha' i],iframe[title*='captcha' i],#captcha")).stream()
                .anyMatch(this::visible);
        return captchaFrame ? "Visible CAPTCHA frame/container detected." : "";
    }

    private void dismissObviousBlockingOverlay() {
        // Close a visible modal using its observed top-right close control.
        for (WebElement dialog : driver.findElements(By.cssSelector("[role='dialog'],dialog"))) {
            if (!visible(dialog)) continue;
            List<WebElement> buttons = dialog.findElements(By.cssSelector("button,[role='button']"));
            WebElement close = buttons.stream().filter(this::visible)
                    .max(java.util.Comparator.comparingDouble(button -> {
                        org.openqa.selenium.Point point = button.getLocation();
                        return point.getX() - point.getY() * 0.2;
                    })).orElse(null);
            if (close != null) {
                try {
                    close.click();
                    new WebDriverWait(driver, Duration.ofSeconds(4))
                            .until(d -> !visible(dialog));
                } catch (Exception ignored) { }
            }
        }
        for (WebElement button : driver.findElements(By.cssSelector(
                "button,[role='button']"))) {
            if (!visible(button) || !button.isEnabled()) continue;
            String label = buttonLabel(button);
            if (label.matches("^(✕|×|x|close)$")) {
                try { button.click(); } catch (Exception ignored) { }
                return;
            }
            if (label.matches(".*(accept all|allow all|got it|agree and continue).*")) {
                try { button.click(); } catch (Exception ignored) { }
                return;
            }
        }
    }

    private int productHrefCount() {
        return ((Number) ((JavascriptExecutor) driver).executeScript("""
                return [...document.querySelectorAll('a[href]')].filter(a =>
                  /\\/(p|product|products|itm)\\//i.test(a.getAttribute('href')||'')).length;
                """)).intValue();
    }

    private String domInventory() {
        return (String) ((JavascriptExecutor) driver).executeScript("""
                const vis=e => !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
                const count=s=>document.querySelectorAll(s).length;
                return JSON.stringify({readyState:document.readyState, bodyText:(document.body?.innerText||'').slice(0,2200),
                  counts:{inputs:count('input'),buttons:count('button'),links:count('a[href]'),images:count('img'),
                    articles:count('article'),dataId:count('[data-id]'),productClass:count('[class*=product i]'),
                    selects:count('select'),dialogs:count('[role=dialog]')},
                  visibleInputs:[...document.querySelectorAll('input')].filter(vis).slice(0,30).map(e=>({type:e.type,name:e.name,id:e.id,
                    placeholder:e.placeholder,aria:e.getAttribute('aria-label')})),
                  visibleButtons:[...document.querySelectorAll('button,[role=button]')].filter(vis).slice(0,40)
                    .map(e=>(e.innerText||e.getAttribute('aria-label')||'').trim().slice(0,100))});
                """);
    }

    private String listingInventory() {
        return (String) ((JavascriptExecutor) driver).executeScript("""
                const vis=e => !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
                const candidates=[...document.querySelectorAll('article,[data-id],[class*=product i]')]
                  .filter(vis).filter(e => (e.innerText||'').trim().length>20).slice(0,40);
                const links=[...document.querySelectorAll('a[href]')].filter(vis).filter(a => a.querySelector('img') ||
                  /\\/(p|product|products)\\//i.test(a.getAttribute('href')||'')).slice(0,30)
                  .map(a=>({text:(a.innerText||a.querySelector('img')?.alt||'').trim().slice(0,180),href:a.href,
                    imageAlt:a.querySelector('img')?.alt||'',imageSrc:a.querySelector('img')?.getAttribute('src')||''}));
                const prices=(document.body?.innerText||'').match(/(?:₹|Rs\\.?\\s*)[0-9,]+(?:\\.[0-9]{1,2})?/g)||[];
                return JSON.stringify({candidateCardCount:candidates.length,cards:candidates.slice(0,15).map(e=>
                  ({tag:e.tagName,classes:String(e.className||'').slice(0,140),text:(e.innerText||'').trim().slice(0,260)})),
                  productLinks:links,visiblePriceStrings:[...new Set(prices)].slice(0,40)});
                """);
    }

    private String detailInventory(String text) {
        return "visiblePriceText=" + text.lines()
                .filter(line -> line.contains("₹") || line.toLowerCase(Locale.ROOT).contains("rs."))
                .limit(12).toList() + "; availabilityText=" + text.lines()
                .filter(line -> line.toLowerCase(Locale.ROOT).matches(
                        ".*(in stock|out of stock|available|delivery|deliver to|sold out).*"))
                .limit(12).toList() + "; " + listingInventory();
    }

    private String cartInventory() {
        return (String) ((JavascriptExecutor) driver).executeScript("""
                const visible=e=>!!(e.offsetWidth||e.offsetHeight||e.getClientRects().length);
                return JSON.stringify({text:(document.body?.innerText||'').slice(0,2500),
                  quantity:[...document.querySelectorAll('select,input')].filter(visible).map(e=>({tag:e.tagName,
                    name:e.name,id:e.id,value:e.value,options:e.tagName==='SELECT'?[...e.options].map(o=>o.text):[]})),
                  remove:[...document.querySelectorAll('button,a,[role=button]')].filter(visible)
                    .map(e=>(e.innerText||e.getAttribute('aria-label')||'').trim()).filter(t=>/remove|delete/i.test(t)).slice(0,15)});
                """);
    }

    private String visibleDialogSummary() {
        return driver.findElements(By.cssSelector("[role='dialog'],dialog"))
                .stream().filter(this::visible).map(e -> snippet(e.getText(), 400)).toList().toString();
    }

    private String inputSummary(List<WebElement> inputs) {
        return inputs.stream().filter(this::visible).limit(30).map(input ->
                "type=" + attr(input, "type") + ",name=" + attr(input, "name")
                        + ",id=" + attr(input, "id") + ",placeholder="
                        + attr(input, "placeholder") + ",aria=" + attr(input, "aria-label"))
                .toList().toString();
    }

    private String buttonSummary(List<WebElement> buttons) {
        return buttons.stream().filter(this::visible).limit(40)
                .map(this::buttonLabel).toList().toString();
    }

    private String bodyText() {
        try { return driver.findElement(By.tagName("body")).getText(); }
        catch (Exception e) { return ""; }
    }

    private String buttonLabel(WebElement element) {
        return (element.getText() + " " + attr(element, "aria-label") + " "
                + attr(element, "value")).trim().toLowerCase(Locale.ROOT);
    }

    private boolean productLikeUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("/p/") || lower.contains("/product/")
                || lower.contains("/products/") || lower.contains("/itm/");
    }

    private boolean visible(WebElement element) {
        try { return element.isDisplayed(); } catch (Exception e) { return false; }
    }

    private String attr(WebElement element, String name) {
        try {
            String value = element.getAttribute(name);
            return value == null ? "" : value;
        }
        catch (Exception e) { return ""; }
    }

    private void capture(String fileName) throws IOException {
        Path path = RUN_DIRECTORY.resolve(fileName);
        Files.write(path, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }

    private void record(List<Step> steps, String name, String status,
                        String outcome, String evidence) {
        steps.add(new Step(name, status, outcome, evidence));
        System.out.println("[FEASIBILITY " + MODE + "] " + name + " " + status
                + " :: " + snippet(outcome, 260));
    }

    private void save(Path path, Site site, List<Step> steps) {
        try {
            StringBuilder json = new StringBuilder("{\n")
                    .append("  \"site\": ").append(quote(site.id())).append(",\n")
                    .append("  \"baseUrl\": ").append(quote(site.url())).append(",\n")
                    .append("  \"query\": ").append(quote(site.query())).append(",\n")
                    .append("  \"mode\": ").append(quote(MODE)).append(",\n")
                    .append("  \"finalUrl\": ").append(quote(safeCurrentUrl())).append(",\n")
                    .append("  \"pageTitle\": ").append(quote(safeTitle())).append(",\n")
                    .append("  \"steps\": [\n");
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                json.append("    {\"name\": ").append(quote(step.name()))
                        .append(", \"status\": ").append(quote(step.status()))
                        .append(", \"outcome\": ").append(quote(step.outcome()))
                        .append(", \"evidence\": ").append(quote(step.evidence()))
                        .append("}").append(i + 1 == steps.size() ? "\n" : ",\n");
            }
            json.append("  ]\n}\n");
            Files.writeString(path, json);
        } catch (Exception e) {
            System.err.println("Unable to write feasibility evidence " + path + ": " + e);
        }
    }

    private String safeCurrentUrl() {
        try { return cleanUrl(driver.getCurrentUrl()); } catch (Exception e) { return "unavailable"; }
    }

    private String safeTitle() {
        try { return driver.getTitle(); } catch (Exception e) { return "unavailable"; }
    }

    private String cleanUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return new java.net.URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
                    null, null).toString();
        } catch (Exception e) { return url; }
    }

    private String normalize(String text) {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private String safeMessage(Exception e) {
        return snippet(String.valueOf(e.getMessage()), 500).replaceAll("https?://\\S+", "[url]");
    }

    private String snippet(String value, int limit) {
        String compact = String.valueOf(value).replaceAll("\\s+", " ").trim();
        return compact.length() <= limit ? compact : compact.substring(0, limit) + "…";
    }

    private String quote(String value) {
        String text = String.valueOf(value);
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private record Site(String id, String url, String query) { }
    private record Step(String name, String status, String outcome, String evidence) { }
}
