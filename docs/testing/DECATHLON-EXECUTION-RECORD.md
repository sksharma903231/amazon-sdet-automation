# Decathlon execution record

**Local execution date:** 2026-10-31

## Final cleanup validation

| Validation | Methods / invocations | Results |
| --- | ---: | --- |
| Maven clean compile and test compile | — | Passed |
| Complete headed Chrome suite | 3 / 3 | 3 passed, 0 failed, 0 skipped |
| Complete headless Chrome suite | 3 / 3 | 3 passed, 0 failed, 0 skipped |

Each complete test run observed the three unique scenario IDs
(`DEC-DISC-01`, `DEC-FILTER-01`, `DEC-PDP-01`). The two runs are independent
browser executions, not additional business scenarios.

The final headed analytics are in
`reports/analytics/execution-1793469889133-c0b78695-d8ce-431d-9976-7ccfb2b7109c.json`
and its Extent report is `reports/ExtentReport-20261031-233449-134.html`.
The final headless analytics are in
`reports/analytics/execution-1793469872241-5677d8e5-00e8-4678-8ba0-c35bf4840b27.json`
and its Extent report is `reports/ExtentReport-20261031-233432-241.html`.
Both summaries report three test methods, three invocations, three passed
scenarios, zero failures or skips, zero configuration failures, and seven
unverified candidates.

## Investigation and repeatability evidence

The Adidas filter passed in the complete headed and headless runs, plus two
additional fresh headed sessions. Earlier live runs exposed a stale-element
failure after the listing SPA rerender; the page object was updated to read a
fresh visible listing snapshot for each assertion. No retry was added.

An intermediate headless run failed when search stayed on the homepage and
the screenshot showed unrelated text in the search field. Search now replaces
the input through keyboard interaction and waits until its value equals the
requested query before submitting. The final headed and headless runs then
passed with the original business assertions. The failure screenshot is
`reports/screenshots/com.sksharma.automation.tests.decathlon.DecathlonSearchTest_productDetailsMatchSelectedSearchResult_20261031-233344-107_fb79b8d0-fb15-40be-a577-4ba8865b371a.png`.

Failure screenshot capture was also exercised on earlier Decathlon failures;
Extent reports used paths relative to the report file. Passing runs correctly
have no failure screenshots.

## Behaviors not verified

- Price filter controls and min/max fields were observed, but range
  enforcement and reset remain unverified.
- No stable sorting control was found; numeric ordering is not asserted.
- No-results and category-filter behavior have not been established.
- Product title and displayed price are verified. Variant state and delivery
  availability are not asserted.
- Guest cart addition remains unverified. In the feasibility probe, selecting
  a size did not expose an enabled add-to-cart control. No cart mutation,
  login, personal information, checkout, or payment was attempted.
- GitHub Actions has not been run. Hosted CI status is unknown.

Generated evidence is under ignored `reports/` and `target/` directories and
is not committed.

## 2026-10-31 milestone update

Added `DEC-NAV-01`, which uses a fresh browser to open a result, navigate back
to a usable listing, refresh while retaining the decoded search URL, and
navigate forward to the same product. The first run exposed that the listing
needed a post-back content wait and that Decathlon serializes equivalent query
spaces as `%20` or `+`; both conditions are handled explicitly. No retry was
added.

| Validation | Methods / invocations | Results |
| --- | ---: | --- |
| Clean Maven build and headless Chrome suite | 4 / 4 | 4 passed, 0 failed, 0 skipped; 15.1 s total |
| Headed Chrome suite | 4 / 4 | 4 passed, 0 failed, 0 skipped; 16.9 s total |
| Isolated fresh headless navigation session | 1 / 1 | 1 passed |

The headed and headless full suites each observed four unique scenarios:
`DEC-DISC-01`, `DEC-FILTER-01`, `DEC-PDP-01`, and `DEC-NAV-01`. The headed
analytics record reports 15,295 ms of test-process execution, zero
configuration failures, and seven unverified candidate scenarios. Reporting
emitted a unique Extent report and analytics JSON. No screenshot was expected
because these runs passed.

Headed and headless local Chrome are verified. GitHub Actions is configured
for the complete headless suite but has not been run on hosted CI. The initial
unprivileged Maven attempt could not bind Selenium's local ChromeDriver port;
reruns with local port access succeeded. Selenium also emitted a non-fatal
Chrome 156 / CDP 155 nearest-binding warning.

The seven unverified inventory entries remain price range/reset, category and
additional filters, sorting, no-results behavior, variants/delivery state,
guest cart addition, and cart quantity/subtotal/removal. Checkout is not in the
verified inventory because it was not safely reachable or exercised. No
account, personal data, order, or payment was used.

## 2026-11-01 expanded functional milestone

Live DOM inspection found current Gender facets, Price sorting options, a
read-only price range display, and product-card Add to cart buttons. New
coverage adds partial and mixed-case query variations from a separate
properties fixture, the explicit no-results message (Decathlon also shows
recommendation cards), Men filtering with Clear all, and low-to-high numeric
price sorting. The sort control's high-to-low option did not consistently
produce descending sale-price order, so it remains unverified. Clicking a
listing Add to cart control did not show cart confirmation or alter a cart
state; cart and checkout remain unverified.

The first grouped Maven run exposed that inherited setup did not execute under
Surefire group filtering. Marking `BaseTest.setUp` `alwaysRun=true` corrected
this; grouped runs now launch a separate driver for each invocation. The
no-results page includes recommendations, so its assertion checks the exact
no-results message and absence of the unique unmatched term from product
evidence instead of requiring the whole page to have zero product links.

| Validation | Methods / invocations | Results |
| --- | ---: | --- |
| Search, filters, and sorting groups (headless) | 6 / 8 | 8 passed, 0 failed, 0 skipped |
| Search group after Extent labeling update (headless) | 3 / 5 | 5 passed, 0 failed, 0 skipped |
| Men → Running Shoes category (headless) | 1 / 1 | 1 passed |
| Full clean suite (headless) | 9 / 11 | 11 passed, 0 failed, 0 skipped; 34.1 s total |
| Full suite (headed, final code) | 9 / 11 | 11 passed, 0 failed, 0 skipped; 43.8 s total |

Both full runs observed nine unique business scenarios and three data rows
for `DEC-DISC-02`. Final headed analytics reports 9 methods, 11 invocations,
9 passed scenarios, zero configuration failures, eight unverified candidates,
and one blocked candidate. Per-module outcomes were recorded for search,
filters, sorting, product, and navigation. The failed intermediate high-to-low
assertion exercised screenshot capture; Extent reports contain the failure
entry and screenshot attachment. Final passing runs correctly have no failure
screenshots. Search group reports also confirmed scenario descriptions and
query parameters appear in Extent test names.

Current limitations are tracked in `in-scope-scenarios.txt`: price input is
blocked because both displayed min/max fields are read-only and no adjustable
range was exposed; empty/special-character search, other category facets,
high-to-low sort, product variant/delivery state, guest cart mutation, cart
calculations/removal, and safe checkout validation remain unverified. Men →
Running Shoes navigation is implemented. GitHub hosted CI has not been run.
