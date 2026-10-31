# Real-World E-commerce Website Feasibility Assessment

**Assessment date:** 2026-10-31
**Scope:** Flipkart India, Croma, and Decathlon India using the repository's Java/Selenium/TestNG framework.

## Executive summary

Decathlon India is the strongest candidate for the portfolio application's primary test target. Selenium completed search in headed and headless Chrome, exposed product links, titles, images, and prices, opened a real brand filter, verified an Adidas result set from product-title brand tokens, and opened a product whose heading matched the selected result. Its price panel is observable, but the assessment did not apply a range. Sorting was not exposed in the inspected result controls. Cart addition remained incomplete after a size selection.

Flipkart is a viable secondary candidate for search coverage. Headless search navigated to results and exposed product links; headed search was inconsistent because the search interaction was intercepted by an overlay or the input was unavailable. Product detail, filters, sorting, and cart were not reached.

Croma is the weakest candidate in this environment. Headed Chrome reached the storefront, but a location dialog initially covered the search field; after closing it, search navigated to `/searchB` and rendered a blank results area. Headless Chrome received an explicit `Access Denied` page. No product workflow was verified.

This is a bounded feasibility assessment, not a reliability study. The results support choosing Decathlon for search, product listing, brand filtering, and product-detail scenarios. They do not support claiming its price-range, sorting, or cart workflows are automated reliably.

## Environment and execution

- Existing repository architecture: Java 21, Selenium 4.51.0, TestNG 7.11.0, Maven, Chrome, `DriverFactory` with `ThreadLocal<WebDriver>`, `BaseTest` lifecycle, ExtentReports listener, JSON analytics listener.
- Host: Linux x86_64; Java 21.0.12.1.
- Browser observed: Chrome 156.0.8078.73. Selenium logged that its closest available DevTools protocol binding was version 155; ChromeDriver sessions nevertheless ran and produced browser evidence.
- Each TestNG diagnostic method received a fresh driver through the existing `BaseTest`. Headed diagnostics ran first; a limited headless comparison then repeated landing and search only.
- The diagnostic class is isolated under `com.sksharma.automation.feasibility`. It did not alter Amazon page objects, Amazon tests, configuration, or CI.
- Screenshots and per-site JSON evidence are under the ignored generated-artifact directory `reports/feasibility/`. Existing Extent reports and analytics use unique run names and were not overwritten.
- No login, account creation, personal data entry, checkout, payment, CAPTCHA solving, or access-control bypass was attempted.

The assessment used repeated fresh sessions while correcting diagnostic-only issues surfaced during the first pass. The first instrumentation pass exposed and fixed a JavaScript-number conversion issue; one Decathlon filter pass also returned a stale-element exception. Those observations are retained in the dated JSON artifacts and are not represented as site access blocks.

## Journey counts

Counts below are **unique customer journey types**, not browser launches. “Verified” means Selenium completed the interaction and the stated result was observed. Landing accessibility is listed separately. Headed/headless differences are called out rather than merged into a single pass.

| Website | Initial access | Unique journeys attempted | Successfully verified | Result |
| --- | --- | ---: | ---: | --- |
| Flipkart | Headed and headless landing pages loaded. | 1: search/listing | 1, headless only | **PARTIAL** |
| Croma | Headed landing loaded; headless page explicitly denied access. | 1: search/listing | 0 | **PARTIAL overall; headless BLOCKED** |
| Decathlon | Headed and headless landing pages loaded. | 5: search/listing, brand filter, price filter, product detail, guest cart | 3: search/listing, brand filter (one run), product detail | **PARTIAL** |

Sorting was inspected where a listing was reached, but no price-ascending control was found in the observed Decathlon result controls, so a sort interaction was not counted as attempted. The brand interaction was repeatable only once; another filter pass encountered dynamic/stale UI. The price panel opened, but no range was applied or verified.

## Flipkart India

**Landing URL/title:** `https://www.flipkart.com/` — “Online Shopping Site for Mobiles, Electronics, Furniture, Grocery, Lifestyle, Books & More. Best Offers!”

| Journey | Outcome | Evidence and reason |
| --- | --- | --- |
| Initial accessibility | PASS | The actual Flipkart storefront loaded in headed and headless Chrome. The headed landing page exposed a login drawer/overlay and a search input. |
| Search and listing | PARTIAL headed; PASS headless | Headed input clicks were intercepted by an overlay (`div.mcO4kT...`) or the visible search input was unavailable after the close action. Headless Chrome submitted `running shoes men`, reached `https://www.flipkart.com/search`, and exposed 125 product-like links. The page title identified the query. |
| Product metadata | PARTIAL | Headless DOM evidence included product link text, image alt/source attributes, and visible price strings. This assessment did not validate card-level price comparability, brand metadata, or ordering. |
| Brand/price filter and sorting | NOT TESTED | The headed search flow did not reach results. The headless comparison intentionally stopped after search. |
| Product details | NOT TESTED | No result was opened. |
| Guest cart | NOT TESTED | No product page was reached. |

**Assessment:** Search is a realistic candidate in headless Chrome, but headed interaction instability and the lack of deeper workflow evidence make Flipkart less suitable than Decathlon for the primary target in this assessment.

Evidence:

- Headed landing: [flipkart-landing.png](../../reports/feasibility/20261031-230520-870/headed/flipkart-landing.png)
- Headless landing: [flipkart-landing.png](../../reports/feasibility/20261031-230726-528/headless/flipkart-landing.png)
- Headless results: [flipkart-search.png](../../reports/feasibility/20261031-230726-528/headless/flipkart-search.png)
- Structured DOM/run evidence: [headed JSON](../../reports/feasibility/20261031-230520-870/headed/flipkart.json), [headless JSON](../../reports/feasibility/20261031-230726-528/headless/flipkart.json)

## Croma

**Headed landing URL/title:** `https://www.croma.com/` — “Croma Electronics | Online Electronics Shopping | Buy Electronics Online”
**Headless landing title:** “Access Denied”

| Journey | Outcome | Evidence and reason |
| --- | --- | --- |
| Initial accessibility | PASS headed; BLOCKED headless | Headed Chrome loaded the Croma storefront. The landing screenshot showed a “SELECT YOUR LOCATION” dialog covering the page; the site had a pincode field and location actions. The dialog was closed without entering a pincode. Headless Chrome displayed an explicit `Access Denied` page. |
| Search and listing | PARTIAL headed; NOT TESTED headless | The headed search field was `input#searchV2[name=search]`. Before the location dialog was closed, clicks were intercepted by the modal. After closure, search navigated to `https://www.croma.com/searchB` and the page title included “Search wireless headphones”, but the page body/results area was blank and no product-like links or prices were observed. The saved search screenshot shows the blank content area. |
| Product metadata | NOT TESTED | No search results were available to inspect. |
| Brand/price filter and sorting | NOT TESTED | No listing was reached. |
| Product details | NOT TESTED | No product link was available. |
| Guest cart | NOT TESTED | No product page was reached. |

**Assessment:** Croma is not recommended as the portfolio target in this environment. The headless denial is an explicit restriction; the headed blank search page is recorded as a partial application flow, not misclassified as a bot block.

Evidence:

- Headed landing/location dialog: [croma-landing.png](../../reports/feasibility/20261031-230520-870/headed/croma-landing.png)
- Headed blank search: [croma-search.png](../../reports/feasibility/20261031-230520-870/headed/croma-search.png)
- Headless access denial: [croma-landing.png](../../reports/feasibility/20261031-230726-528/headless/croma-landing.png)
- Structured DOM/run evidence: [headed JSON](../../reports/feasibility/20261031-230520-870/headed/croma.json), [headless JSON](../../reports/feasibility/20261031-230726-528/headless/croma.json)

## Decathlon India

**Landing URL/title:** `https://www.decathlon.in/` — “Buy Sporting Goods, Sportswear and Equipments | Download App”
**Search:** `running shoes`
**Search result URL/title:** `https://www.decathlon.in/search` — “Search | running shoes”

| Journey | Outcome | Evidence and reason |
| --- | --- | --- |
| Initial accessibility | PASS headed and headless | The actual storefront loaded in both modes. A delivery location (Bangalore) was visible in the page content; no user location was entered. |
| Search and listing | PASS headed and headless | The search field was an observed `input[type=search]`. Search completed and the DOM exposed 20 product-path links in the final headed run (26 in another session) and 20 in the headless run. Links included readable product names, `/p/{id}/...` paths, image alt/source values, and displayed prices. |
| Brand filter | PASS once; repeatability PARTIAL | The result page exposed `Brand`; Selenium opened it, selected the observed “Adidas 114” option, and the listing changed. In that run, every visible product-path link title contained the selected Adidas brand token. Another run encountered a stale element, and a later returned-to-results page did not expose the same Brand control. Treat the filter as supported but dynamically unstable until repeated in the final page state. |
| Price filter | PARTIAL | The observed `Price` control opened and displayed `Minimum` and `Maximum` inputs. Example displayed defaults varied by session (`₹1,899`–`₹19,999` and `₹699`–`₹19,999`). No range was entered or applied, so range enforcement was not verified. |
| Price sorting | NOT TESTED | The inspected result controls included Price, Brand, and More Filters; no native select or explicit price-ascending option was found. Numeric ordering was not asserted. |
| Product details | PASS | Selenium opened a result with an observed `/p/{productId}/...` product link. The detail `h1` matched the selected result title under lowercase/punctuation-insensitive normalization. The page exposed a current price and MRP (for example ₹5,503 and ₹8,599 in one session), size options, and delivery/service text. Stock status was not established from the visible delivery text. |
| Guest cart | PARTIAL; not verified | The first add-to-cart attempt displayed “Please select a size.” In a later fresh session Selenium selected a displayed size (`5.5`), but no enabled add-to-cart control was available afterward. No cart item, quantity, or removal state was verified. No login prompt was observed in this flow. |

**Assessment:** Decathlon has the broadest verified UI scope and is the recommended primary application under test, limited to search/listing, brand filtering (with repeatability risk), and product-detail identity/information. Do not include price enforcement, price sorting, or cart assertions in a regression suite until their interactions are verified and repeatable.

Evidence:

- Headed landing: [decathlon-landing.png](../../reports/feasibility/20261031-230520-870/headed/decathlon-landing.png)
- Headed search results: [decathlon-search.png](../../reports/feasibility/20261031-230520-870/headed/decathlon-search.png)
- Headed product details: [decathlon-product-details.png](../../reports/feasibility/20261031-230520-870/headed/decathlon-product-details.png)
- Final size/cart probe: [decathlon-product-details.png](../../reports/feasibility/20261031-230859-786/headed/decathlon-product-details.png)
- Headless landing/results: [landing](../../reports/feasibility/20261031-230726-528/headless/decathlon-landing.png), [search](../../reports/feasibility/20261031-230726-528/headless/decathlon-search.png)
- Structured DOM/run evidence: [headed results and filters JSON](../../reports/feasibility/20261031-230520-870/headed/decathlon.json), [final cart probe JSON](../../reports/feasibility/20261031-230859-786/headed/decathlon.json), [headless JSON](../../reports/feasibility/20261031-230726-528/headless/decathlon.json)

## DOM observability and locator maintenance

| Website | Observed locator candidates | Reliability notes |
| --- | --- | --- |
| Flipkart | `input[name=q][title="Search for Products, Brands and More"]`; result links under `/search`; listing links exposed text and image attributes. | Search was reliable headlessly, but headed clicks were intercepted or the visible input disappeared after an overlay interaction. SPA overlays and rotating landing content add maintenance risk. Product cards/details were not opened. |
| Croma | `input#searchV2[name=search]`; search route `/searchB`; landing had product links and price text. | Search field locator was observable but blocked by a location dialog until it was closed. After the search route loaded, its content area was blank. Headless access was explicitly denied. MUI/autocomplete implementation and location state are additional risk. |
| Decathlon | `input[type=search]`; product anchors with `/p/{id}/...`; product page exposed `article.pdp-container`, `h1`, visible prices, size choices, and delivery text. | Strongest observed data surface. The Brand control/checkbox caused a visible listing change and product titles corroborated the selected brand. A subsequent session saw a stale element or absent control; dynamic rerendering makes component scoping and explicit waits important. |

The JSON evidence stores visible text snippets, candidate selector counts, product-link text/hrefs, image alt/source values, and observed form-control labels. It does not store cookies, page scripts, authentication data, or full page source.

## Ranking and recommendation

1. **Decathlon India** — best candidate. Search, product listing, one brand-filter result, and product-detail identity/information were actually exercised. Price panel is observable but not validated; cart and sorting remain incomplete.
2. **Flipkart India** — headless search/listing is observable, but headed interaction differs and no deeper product workflow was verified.
3. **Croma** — headed search returned a blank page and headless access was denied.

The current framework can support the Decathlon assessment without architectural changes. It already supplies fresh Chrome sessions, explicit Selenium interactions, TestNG execution, screenshots, ExtentReports, and JSON analytics. Future regression work would add Decathlon-specific locator/page classes and tests alongside the Amazon package; no migration is required.

## Validation and limitations

- `mvn --batch-mode -DskipTests test-compile` passed for the diagnostic class.
- The headed diagnostic invocations and limited headless comparison completed without TestNG errors; individual journey outcomes are recorded in each JSON file. A TestNG pass here means the diagnostic evidence was collected, not that every site workflow passed.
- Headed and headless comparison was performed for landing and search only. Deeper flows were not repeated headlessly.
- The first diagnostic iteration had a harness-only Java numeric conversion issue, which was corrected. One dynamic Decathlon filter interaction produced `StaleElementReferenceException`; this is documented as instability, not a website access block.
- Site content, delivery location, stock, promotions, and prices are dynamic. This single-day assessment cannot establish long-run reliability or regional behavior.
- The generated `reports/feasibility/` files are ignored by the repository's existing `reports/` ignore rule and remain local unless explicitly included later.
- No GitHub Actions run was performed. No framework migration, Amazon test modification, commit, or push was performed.

To rerun the diagnostic later:

```bash
mvn --batch-mode -Dtest=EcommerceWebsiteFeasibilityTest \
  -Dassessment.mode=headed -Dbrowser=chrome -Dheadless=false test

mvn --batch-mode -Dtest=EcommerceWebsiteFeasibilityTest \
  -Dassessment.mode=headless -Dbrowser=chrome -Dheadless=true test
```
