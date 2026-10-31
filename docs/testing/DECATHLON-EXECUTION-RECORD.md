# Decathlon and Amazon execution record

**Local execution date:** 2026-10-31

## Validation matrix

| Scope | Browser | Methods / invocations | Results |
| --- | --- | ---: | --- |
| Complete Decathlon suite | Headed Chrome | 3 / 3 | 3 passed, 0 failed, 0 skipped |
| Complete Decathlon suite | Headless Chrome | 3 / 3 | 3 passed, 0 failed, 0 skipped |
| Adidas filter repeat, fresh session 1 | Headed Chrome | 1 / 1 | 1 passed |
| Adidas filter repeat, fresh session 2 | Headed Chrome | 1 / 1 | 1 passed |
| Amazon regression (search, homepage, launch) | Headed Chrome | 6 methods / 9 invocations | 9 passed, 0 failed, 0 skipped |

The complete Decathlon runs each observed all three unique scenario IDs
(`DEC-DISC-01`, `DEC-FILTER-01`, `DEC-PDP-01`). The four successful filter
observations across headed/headless and the two additional headed runs are
repeatability evidence for those sessions, not four distinct business
scenarios and not a long-run reliability estimate.

The first live headed suite run had two failures: the filter assertion read a
stale Selenium element after the site's SPA rerender, and one independent
search session did not expose results before the explicit wait expired. The
filter page object was changed to snapshot visible listing data from the
current DOM on each read. Subsequent full headed and headless runs passed with
the same brand and product identity assertions. The initial restricted-sandbox
attempts failed before browser startup because Selenium could not probe a free
ChromeDriver port; the successful runs used the approved local execution
environment. No test retry was added.

## Behaviors not verified

- Price filter controls and min/max fields have been observed, but applying a
  range, validating the resulting prices, and clearing the range remain
  unverified.
- No stable sorting control was found in the inspected results. Numeric order
  was not asserted.
- No-result and category-filter behavior have not been established.
- Product detail heading and selected listing price are verified. Variant
  selection state and delivery availability are not part of the regression
  assertions.
- Guest cart addition remains unverified. In the feasibility probe, the site
  first required a size; a displayed size was selected in another probe, but
  an enabled add-to-cart control did not become available. No cart mutation,
  login, personal information, checkout, or payment was attempted.
- GitHub Actions has not been run. The workflow configuration is present, but
  hosted CI status is unknown.

## Local artifacts

Generated artifacts are under ignored `reports/` and `target/` directories;
they are not committed. The final successful full headed run is recorded in:

- `reports/analytics/execution-1793469202727-a02276b7-38c9-48b3-8417-8747d45138e3.json`
- `reports/ExtentReport-20261031-232322-728.html`

The successful full headless run is recorded in:

- `reports/analytics/execution-1793469222599-1f7e0387-004f-4ea8-8526-ac09ab29541d.json`
- `reports/ExtentReport-20261031-232342-600.html`

The preserved Amazon regression run is recorded in:

- `reports/analytics/execution-1793469258798-6fdf85e2-4e6d-4448-b457-31543d75ecf5.json`
- `reports/ExtentReport-20261031-232418-799.html`

Failure screenshot capture was exercised on the initial live Decathlon
failures. The images are under `reports/screenshots/` and referenced from the
Extent execution report. Clean passing runs correctly have no failure
screenshots.
