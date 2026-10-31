package com.sksharma.automation.reporting;

import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Writes a per-run summary based on observed TestNG callbacks. */
public final class ExecutionAnalytics {
    private static final String PROJECT_NAME = "ecommerce-ui-automation";
    private static final String APPLICATION_NAME = "decathlon-india";
    private static final Map<String, String> SCENARIO_IDS = Map.ofEntries(
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.searchShowsProductNamesPricesAndLinks", "DEC-DISC-01"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.searchQueryVariationsReturnRelevantProducts", "DEC-DISC-02"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.unmatchedSearchShowsNoResultsState", "DEC-DISC-03"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.adidasBrandFilterChangesAndConstrainsListing", "DEC-FILTER-01"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.productDetailsMatchSelectedSearchResult", "DEC-PDP-01"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.productNavigationSurvivesBackRefreshAndForward", "DEC-NAV-01"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.mainNavigationOpensMenRunningShoesCategory", "DEC-NAV-02"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.genderFilterConstrainsListingAndCanBeCleared", "DEC-FILTER-04"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.priceLowToHighSortOrdersDisplayedPricesNumerically", "DEC-SORT-01")
    );
    private static final Map<String, String> SCENARIO_MODULES = Map.of(
            "DEC-DISC-01", "search", "DEC-DISC-02", "search", "DEC-DISC-03", "search",
            "DEC-FILTER-01", "filters", "DEC-FILTER-04", "filters", "DEC-SORT-01", "sorting",
            "DEC-PDP-01", "product", "DEC-NAV-01", "navigation", "DEC-NAV-02", "navigation");

    private final ConcurrentHashMap<ITestResult, Integer> results = new ConcurrentHashMap<>();
    private final LongAdder screenshotSuccesses = new LongAdder();
    private final LongAdder screenshotFailures = new LongAdder();
    private final LongAdder configurationFailures = new LongAdder();
    private Instant startTime;
    private String executionId;

    public synchronized void start() {
        results.clear();
        screenshotSuccesses.reset();
        screenshotFailures.reset();
        configurationFailures.reset();
        startTime = Instant.now();
        executionId = UUID.randomUUID().toString();
    }

    public void record(ITestResult result) { results.putIfAbsent(result, result.getStatus()); }
    public void screenshotCaptured() { screenshotSuccesses.increment(); }
    public void screenshotCaptureFailed() { screenshotFailures.increment(); }
    public void configurationFailed() { configurationFailures.increment(); }

    public synchronized void writeSummary() throws IOException {
        Instant endTime = Instant.now();
        List<ITestResult> ordered = results.keySet().stream()
                .sorted(Comparator.comparingLong(ITestResult::getStartMillis)).toList();
        Map<String, Integer> scenarioStatus = new LinkedHashMap<>();
        java.util.Set<String> methods = new java.util.HashSet<>();
        long passedInvocations = 0, failedInvocations = 0, skippedInvocations = 0;
        for (ITestResult result : ordered) {
            String method = methodName(result);
            methods.add(method);
            String scenario = SCENARIO_IDS.get(method);
            if (scenario != null) scenarioStatus.merge(scenario, results.get(result),
                    ExecutionAnalytics::worstStatus);
            switch (results.get(result)) {
                case ITestResult.SUCCESS -> passedInvocations++;
                case ITestResult.FAILURE -> failedInvocations++;
                case ITestResult.SKIP -> skippedInvocations++;
                default -> { }
            }
        }
        long passedScenarios = scenarioStatus.values().stream().filter(s -> s == ITestResult.SUCCESS).count();
        long failedScenarios = scenarioStatus.values().stream().filter(s -> s == ITestResult.FAILURE).count();
        long skippedScenarios = scenarioStatus.values().stream().filter(s -> s == ITestResult.SKIP).count();
        long unverified = scenarioCount("UNVERIFIED");
        long blocked = scenarioCount("BLOCKED");
        long durationMs = startTime == null ? 0 : endTime.toEpochMilli() - startTime.toEpochMilli();
        Path directory = Path.of("reports", "analytics");
        Files.createDirectories(directory);
        Path output = directory.resolve("execution-" + startTime.toEpochMilli() + "-" + executionId + ".json");
        String json = "{\n"
                + "  \"executionId\": \"" + executionId + "\",\n"
                + "  \"project\": \"" + PROJECT_NAME + "\",\n"
                + "  \"application\": \"" + APPLICATION_NAME + "\",\n"
                + "  \"startTime\": \"" + startTime + "\",\n"
                + "  \"endTime\": \"" + endTime + "\",\n"
                + "  \"totalTestMethods\": " + methods.size() + ",\n"
                + "  \"totalInvocations\": " + ordered.size() + ",\n"
                + "  \"uniqueBusinessScenarios\": " + scenarioStatus.size() + ",\n"
                + "  \"passedScenarios\": " + passedScenarios + ",\n"
                + "  \"failedScenarios\": " + failedScenarios + ",\n"
                + "  \"skippedScenarios\": " + skippedScenarios + ",\n"
                + "  \"unverifiedScenarios\": " + unverified + ",\n"
                + "  \"blockedScenarios\": " + blocked + ",\n"
                + "  \"passedInvocations\": " + passedInvocations + ",\n"
                + "  \"failedInvocations\": " + failedInvocations + ",\n"
                + "  \"skippedInvocations\": " + skippedInvocations + ",\n"
                + "  \"configurationFailures\": " + configurationFailures.sum() + ",\n"
                + "  \"executionDurationMs\": " + durationMs + ",\n"
                + "  \"unverifiedCandidateScenarios\": " + unverified + ",\n"
                + "  \"screenshotCaptureSuccesses\": " + screenshotSuccesses.sum() + ",\n"
                + "  \"screenshotCaptureFailures\": " + screenshotFailures.sum() + ",\n"
                + "  \"scenarioResults\": " + scenarioJson(scenarioStatus) + ",\n"
                + "  \"moduleOutcomes\": " + moduleJson(scenarioStatus) + ",\n"
                + "  \"invocations\": [\n" + invocationJson(ordered) + "  ]\n}\n";
        Files.writeString(output, json, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private String moduleJson(Map<String, Integer> values) {
        Map<String, Map<String, Long>> modules = new LinkedHashMap<>();
        values.forEach((id, status) -> {
            String module = SCENARIO_MODULES.getOrDefault(id, "unmapped");
            Map<String, Long> counts = modules.computeIfAbsent(module, ignored ->
                    new LinkedHashMap<>(Map.of("passed", 0L, "failed", 0L, "skipped", 0L)));
            String key = statusName(status);
            if (counts.containsKey(key)) counts.compute(key, (ignored, count) -> count + 1);
        });
        List<String> entries = new ArrayList<>();
        modules.forEach((name, counts) -> entries.add("\"" + name + "\":{\"passed\":"
                + counts.get("passed") + ",\"failed\":" + counts.get("failed")
                + ",\"skipped\":" + counts.get("skipped") + "}"));
        return "{" + String.join(",", entries) + "}";
    }

    private static int worstStatus(int previous, int next) {
        if (previous == ITestResult.FAILURE || next == ITestResult.FAILURE) return ITestResult.FAILURE;
        if (previous == ITestResult.SKIP || next == ITestResult.SKIP) return ITestResult.SKIP;
        return ITestResult.SUCCESS;
    }

    private String scenarioJson(Map<String, Integer> values) {
        List<String> items = new ArrayList<>();
        values.forEach((id, status) -> items.add("{\"scenarioId\":\"" + id
                + "\",\"status\":\"" + statusName(status) + "\"}"));
        return "[" + String.join(",", items) + "]";
    }

    private static long scenarioCount(String status) throws IOException {
        Path inventory = Path.of("src/test/resources/in-scope-scenarios.txt");
        if (!Files.exists(inventory)) return 0;
        try (var lines = Files.lines(inventory)) {
            return lines.filter(line -> line.startsWith(status + "|DEC-")).count();
        }
    }

    private String invocationJson(List<ITestResult> values) {
        List<String> items = new ArrayList<>();
        for (ITestResult result : values) {
            String method = methodName(result);
            String parameters = java.util.Arrays.stream(result.getParameters())
                    .map(value -> "\"" + escape(String.valueOf(value)) + "\"")
                    .collect(java.util.stream.Collectors.joining(","));
            items.add("    {\"scenarioId\":\"" + SCENARIO_IDS.getOrDefault(method, "UNSCOPED")
                    + "\",\"method\":\"" + escape(method) + "\",\"status\":\""
                    + statusName(results.get(result)) + "\",\"parameters\":[" + parameters
                    + "],\"durationMs\":"
                    + Math.max(0, result.getEndMillis() - result.getStartMillis()) + "}");
        }
        return String.join(",\n", items) + (items.isEmpty() ? "" : "\n");
    }

    private static String methodName(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }
    private static String statusName(int status) {
        return switch (status) {
            case ITestResult.SUCCESS -> "passed";
            case ITestResult.FAILURE -> "failed";
            case ITestResult.SKIP -> "skipped";
            default -> "unknown";
        };
    }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
