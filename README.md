# E-commerce UI Automation — Decathlon India

Maintainable Java/Selenium UI automation for verified product-discovery
workflows on [Decathlon India](https://www.decathlon.in/). This is a focused
SDET portfolio project: scenario coverage is driven by observed storefront
behavior, and unsupported flows remain explicit in the scenario inventory.

## Technology

| Component | Version / choice |
| --- | --- |
| Java | 21 |
| Build | Maven 3.9+ |
| Browser automation | Selenium Java 4.51.0 |
| Test framework | TestNG 7.11.0 |
| Reporting | ExtentReports 5.1.2 |
| Logging | Log4j2 2.26.1 |
| Browser | Google Chrome, headed or headless |

## Architecture

```text
src/main/java/com/sksharma/automation/
  applications/decathlon/elements/  Decathlon-specific locators
  applications/decathlon/pages/     Home, search, and product page objects
  config/                           Properties and JVM overrides
  driver/                           ThreadLocal Chrome lifecycle
  factory/                          Shared Selenium interactions and waits
  pages/                            Shared BasePage
  utils/                            Logging and common wait utilities
src/test/java/com/sksharma/automation/
  tests/decathlon/                  Business-level Decathlon scenarios
  dataproviders/                    Decathlon search query fixtures/provider
  listeners/                        TestNG reporting and diagnostics listener
  reporting/                        Thread-local logger and run analytics
  reports/                          ExtentReports lifecycle
docs/                               Decisions, feasibility, execution evidence
```

Page objects own browser interactions; tests assert business outcomes. Locator
definitions are isolated from page behavior. `DriverFactory` creates one
WebDriver per test thread and `BaseTest` always tears it down. Explicit waits
use the `wait.timeout.seconds` property. `ConfigReader` reads
`src/main/resources/config.properties` and allows JVM system-property
overrides.

The application-selection rationale and browser evidence are documented in
[ADR-001](docs/decisions/ADR-001-application-under-test-selection.md) and the
[feasibility assessment](docs/feasibility/ECOMMERCE_WEBSITE_ASSESSMENT.md).
The recorded local run outcomes are in the
[execution record](docs/testing/DECATHLON-EXECUTION-RECORD.md).

## Implemented business scenarios

The source of truth is [`in-scope-scenarios.txt`](src/test/resources/in-scope-scenarios.txt).

| ID | Workflow | Business evidence |
| --- | --- | --- |
| DEC-DISC-01 | Search for running shoes | Search route and query evidence; visible product names, prices, and product-detail links |
| DEC-DISC-02 | Partial and mixed-case search | Provider-driven `running shoes`, `running`, and `RUNNING SHOES` queries retain relevant route/product evidence |
| DEC-DISC-03 | No-results search | Unmatched query shows “Sorry, No Results Found”; recommendation cards may still appear |
| DEC-FILTER-01 | Apply the Adidas brand filter | Select the observed brand option; verify the listing changes and every visible result identifies Adidas |
| DEC-FILTER-04 | Apply and clear the Men filter | Verify results identify Men and Clear all clears the selected checkbox |
| DEC-PDP-01 | Open a search result | Verify product route, title identity, and selected listing price on the detail page |
| DEC-NAV-01 | Listing and product navigation | Verify browser back, refresh, and forward preserve listing/product identity |
| DEC-NAV-02 | Homepage category navigation | Open Men → Running Shoes and verify the category heading and relevant product listing |
| DEC-SORT-01 | Price low to high | Verify visible sale prices are numerically ordered after selecting the live option |

These are nine unique scenarios. `DEC-DISC-02` has three TestNG data rows;
those rows are invocations of one business scenario. TestNG groups are
`search`, `filters`, `sorting`, `product`, and `navigation`.

## Prerequisites and setup

Install Java 21, Maven 3.9+, Google Chrome, and ensure Decathlon India is
reachable. Selenium Manager resolves a compatible ChromeDriver.

From the repository root, compile the project with:

```bash
mvn --batch-mode -DskipTests test-compile
```

The Maven project name is `ecommerce-ui-automation`.

## Run tests

```bash
# Full suite, headed Chrome
mvn --batch-mode -Dbrowser=chrome -Dheadless=false test

# Full suite, headless Chrome
mvn --batch-mode -Dbrowser=chrome -Dheadless=true test

# Run one or more TestNG groups
mvn --batch-mode -Dgroups=search -Dbrowser=chrome -Dheadless=true test
mvn --batch-mode -Dgroups=filters,sorting -Dbrowser=chrome -Dheadless=true test
```

Default settings are in `config.properties`: `browser=chrome`,
`headless=false`, `base.url=https://www.decathlon.in/`, and
`wait.timeout.seconds=20`. Override them for an execution with JVM properties,
for example:

```bash
mvn --batch-mode -Dtest=DecathlonSearchTest -Dbrowser=chrome \
  -Dheadless=true -Dbase.url=https://www.decathlon.in/ \
  -Dwait.timeout.seconds=30 test
```

The browser factory currently supports Chrome. Headed Chrome requires a
graphical session; CI uses headless mode.

## Reporting, logs, and failures

- Extent HTML reports: `reports/ExtentReport-<timestamp>.html`
- Failure screenshots: `reports/screenshots/`, linked with paths relative to
  each report
- JSON execution summaries: `reports/analytics/`
- Surefire XML and text: `target/surefire-reports/`
- Log4j output: `logs/`

The TestNG listener is registered through
`src/test/resources/META-INF/services/org.testng.ITestNGListener`. It records
test outcomes in ExtentReports, captures screenshots for failures, and writes
analytics on execution completion. Reporting errors are logged and do not
replace the original test outcome. Reports use unique timestamped filenames
and are never overwritten.

Analytics distinguish test methods, TestNG invocations, unique scenario IDs,
scenario and invocation outcomes, configuration failures, failure screenshot
capture outcomes, blocked candidates, and unverified Decathlon candidates.
Module outcomes and total execution duration are included. Data rows do not
become new business scenarios. No coverage or reliability percentage is
reported. Extent test entries include scenario descriptions and data-provider
parameters.

## CI

`.github/workflows/selenium-tests.yml` configures Temurin Java 21 and Chrome,
runs the complete suite headlessly with a 15-minute timeout, and uploads Surefire output,
ExtentReports, screenshots, analytics, and logs even when the test step fails.
The workflow has not yet been verified by a hosted GitHub Actions run.

## Known limitations

- The Adidas filter and listing-to-detail path passed repeated local sessions;
  the storefront uses dynamic rendering, so selectors can still change.
- Search browser back, refresh, and forward behavior passed in a fresh headless
  session. The site may re-encode equivalent query URLs (`%20` and `+`).
- Men → Running Shoes homepage navigation is covered. Other menu/category
  destinations, empty search, and leading/trailing or special-character
  searches remain unverified.
- Price range controls were observed, but applying a range, verifying numeric
  enforcement, and clearing it are blocked in the current UI: the visible
  min/max inputs are read-only and no adjustable slider was exposed.
- Price low-to-high is verified. High-to-low did not consistently order the
  displayed sale prices and remains unverified.
- Gender filtering for Men is verified. Other category and attribute filters
  remain unverified.
- The no-results page also displays recommendation cards; assertions identify
  the explicit no-results message and exclude the unmatched query from product
  evidence rather than requiring an empty page.
- Product title and price identity are covered. Variant state and delivery
  availability are not asserted.
- A click on a listing card's Add to cart control produced no cart confirmation
  or cart state change. Product variants, cart quantity/subtotal/removal, and
  checkout validation remain unverified. No login, order, or payment flow is
  automated.
- The target is a live site; product order, price, inventory, availability,
  and markup can change by time and location.

No authentication, personal information, checkout, payment, or access-control
bypass is part of this suite. Decathlon India is a third-party live application
used for UI automation practice; this project is not affiliated with or
endorsed by Decathlon.
