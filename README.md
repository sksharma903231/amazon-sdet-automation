# Decathlon India E-commerce Automation

Java 21, Selenium, and TestNG UI automation for verified Decathlon India
product-discovery journeys. The repository preserves its Amazon India tests as
a separate application scope and retains the shared framework infrastructure.

## Stack and structure

- Java 21, Maven, Selenium 4.51.0, TestNG 7.11.0
- Page Objects, ThreadLocal WebDriver lifecycle, explicit waits, configurable
  Chrome/headless execution
- ExtentReports, Log4j2, failure screenshots, and per-run JSON analytics
- GitHub Actions uses Chrome headlessly and retains Surefire, reports, and logs

```text
src/main/java/com/sksharma/automation/
  config/ driver/ factory/ pages/ utils/
  applications/decathlon/{elements,pages}/
  elements/ pages/                         # preserved Amazon pages
src/test/java/com/sksharma/automation/
  tests/decathlon/                         # primary functional suite
  tests/                                   # preserved Amazon suite
  dataproviders/ feasibility/ listeners/ reporting/ reports/
docs/decisions/ docs/feasibility/
```

## Verified Decathlon scenarios

`src/test/resources/in-scope-scenarios.txt` is the explicit scenario inventory.
Currently implemented scenarios are:

| ID | Customer workflow | Evidence asserted |
| --- | --- | --- |
| DEC-DISC-01 | Search for running shoes | Search route and query evidence; visible named results, prices, and product links |
| DEC-FILTER-01 | Filter search results by Adidas | Select the observed brand choice; verify listing changes and all visible product names identify Adidas |
| DEC-PDP-01 | Open a search result | Detail route, title identity, and displayed price agree with the selected listing |

These are three unique business scenarios. The search query is currently one
controlled data point; repeated rows or browser modes are not additional
business scenarios. Price range, category filters, sorting, no-results,
variants, delivery availability, and cart are listed as unverified candidates
until their resulting behavior is demonstrated.

The selection rationale and dated live-browser observations are in
[`ADR-001`](docs/decisions/ADR-001-application-under-test-selection.md) and the
[feasibility assessment](docs/feasibility/ECOMMERCE_WEBSITE_ASSESSMENT.md).
Local test outcomes and unsupported behavior are recorded in the
[execution record](docs/testing/DECATHLON-EXECUTION-RECORD.md).

## Run locally

Prerequisites: Java 21, Maven 3.9+, Google Chrome, and network access to
Decathlon India. Selenium Manager resolves ChromeDriver.

```bash
mvn --batch-mode -Dtest=DecathlonSearchTest -Dbrowser=chrome -Dheadless=false test
mvn --batch-mode -Dtest=DecathlonSearchTest -Dbrowser=chrome -Dheadless=true test
```

Run Amazon independently with its application URL explicitly selected:

```bash
mvn --batch-mode -Dtest=AmazonSearchTest,AmazonHomePageTest,BrowserLaunchTest \
  -Dbase.url=https://www.amazon.in/ -Dbrowser=chrome -Dheadless=false test
```

`base.url` and `headless` can be overridden with JVM system properties. A test
method owns a fresh browser session; each DataProvider row also receives its
own setup and teardown.

## Reports and analytics

- Extent HTML: `reports/ExtentReport-<timestamp>.html`
- Failure screenshots: `reports/screenshots/`, with relative report references
- Execution JSON: `reports/analytics/execution-<timestamp>-<id>.json`
- Surefire XML/text: `target/surefire-reports/`
- Log4j output: `logs/`

JSON distinguishes observed TestNG methods, invocations, unique scenario IDs,
scenario outcomes, invocation outcomes, configuration failures, screenshot
capture outcomes, and unverified candidates. A DataProvider invocation does
not create a new scenario. Reports are diagnostic outputs and do not alter
test outcomes.

## CI and limitations

GitHub Actions configures Temurin Java 21, Chrome, and headless execution. It
uploads Surefire results and generated reports even after test failures. A
workflow file is not evidence of a successful hosted run; CI status should be
reported only after an actual Actions execution.

The target is a live, changing storefront. Prices, inventory, delivery,
product ordering, filters, and page markup can vary. The suite avoids login,
personal information, checkout, and payment. It does not bypass access
controls. Amazon functionality remains in its existing packages and can be
run separately as shown above.
