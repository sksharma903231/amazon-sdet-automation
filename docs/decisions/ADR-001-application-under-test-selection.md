# ADR-001: Select Decathlon India as the primary application under test

- **Status:** Accepted as the historical selection decision; active scope later consolidated on Decathlon
- **Date:** 2026-10-31

## Context

The repository began as an Amazon India automation project. Amazon was a useful
real storefront for establishing the Java, Selenium, TestNG, Maven, and Page
Object Model foundations. The existing work includes a ThreadLocal WebDriver
lifecycle, centralized driver creation, configurable properties with JVM
overrides, common page interactions and explicit waits, separate locator
classes, data-driven TestNG tests, listener registration through ServiceLoader,
ExtentReports, thread-local test logging, failure screenshots, unique report
names, execution analytics, CI configuration, and normalized query-term
matching.

The current Amazon suite exercises search-result visibility and query-term
evidence, refresh persistence, browser-back navigation, and basic search-input
readiness. Four search terms are rows of one data-driven scenario, not four
distinct business workflows. The documented Amazon scope therefore remains
small. It does not establish filter behavior, sorting, product details, or cart
workflows. This is a limit of the verified scope, not evidence that Amazon is
impossible to automate.

The project needs a broader verified customer journey set for its intended
functional portfolio scope. Flipkart India, Croma, and Decathlon India were
evaluated through real Selenium browser sessions. The detailed observations,
environment, locators, and artifacts are in
[`ECOMMERCE_WEBSITE_ASSESSMENT.md`](../feasibility/ECOMMERCE_WEBSITE_ASSESSMENT.md).

## Decision

Keep Amazon as an independently executable application-specific baseline and
add Decathlon India as the primary functional automation scope. Preserve the
shared framework and reporting infrastructure; avoid rewriting working
components solely to change the application target.

## Evidence and alternatives

- **Flipkart:** Headed and headless storefronts loaded. Headless search reached
  a listing and exposed product-like links and metadata. Headed interaction
  was inconsistent due to an overlay/login drawer or unavailable input. No
  product detail or cart journey was verified.
- **Croma:** Headed storefront loaded, but search reached a blank results area.
  Headless Chrome showed an explicit Access Denied page. No product workflow
  was verified.
- **Decathlon:** Headed and headless storefronts and search listings loaded.
  Product titles, links, prices, and image attributes were observable. An
  Adidas brand filter changed the listing and matched visible product titles
  in one run. A product detail page opened and its title matched the selected
  listing title.

Decathlon therefore has the broadest observed discovery workflow surface for
this project. The assessment did not validate price-range enforcement, did not
find a sorting control in the inspected listing controls, and did not establish
guest cart addition after selecting a size. Brand filtering showed dynamic UI
instability and needs fresh-session repetition before it is treated as robust.
These limitations remain explicit; the selection does not claim that every
Decathlon feature is automatable.

## Consequences

- Amazon code and tests remain available as a separate regression scope.
- New functional coverage belongs in Decathlon-specific page, element, and test
  packages while reusing framework services.
- Only workflows supported by actual DOM and browser evidence should be added.
- Cart, sorting, and price-range assertions remain candidates until normal user
  interactions and their resulting state are verified.
- Live storefront content, stock, prices, location, and dynamic controls may
  vary between sessions; results must be reported with execution evidence.

## References

- [Real-world feasibility assessment](../feasibility/ECOMMERCE_WEBSITE_ASSESSMENT.md)
- Existing Amazon scenario inventory: `src/test/resources/in-scope-scenarios.txt`

## Subsequent repository direction

The initial migration commit kept Amazon tests alongside Decathlon while
Decathlon journeys were introduced. In the later cleanup milestone, the
Amazon-specific source and tests were removed from the active tree after their
history had been preserved in Git. The active framework is now Decathlon-only;
this ADR and the feasibility assessment remain as historical decision
records.
