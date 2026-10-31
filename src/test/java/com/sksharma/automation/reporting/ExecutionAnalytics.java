package com.sksharma.automation.reporting;

import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Aggregates actual TestNG invocation events and writes one JSON per run. */
public final class ExecutionAnalytics {
    private static final java.util.Map<String, String> SCENARIO_IDS =
            java.util.Map.of(
                    "com.sksharma.automation.tests.BrowserLaunchTest.shouldOpenAmazonIndia", "NAV-01",
                    "com.sksharma.automation.tests.AmazonHomePageTest.shouldShowSearchInput", "NAV-02",
                    "com.sksharma.automation.tests.AmazonHomePageTest.shouldEnableSearchInput", "NAV-03",
                    "com.sksharma.automation.tests.AmazonSearchTest.shouldDisplaySearchResultsForSearchTerm", "DISC-01",
                    "com.sksharma.automation.tests.AmazonSearchTest.shouldRetainSearchStateAfterRefresh", "NAV-04",
                    "com.sksharma.automation.tests.AmazonSearchTest.shouldReturnToHomepageWithBrowserBack", "NAV-05"
            );
    private static final Set<String> AUTOMATED_SCENARIOS = Set.of(
            "com.sksharma.automation.tests.BrowserLaunchTest.shouldOpenAmazonIndia",
            "com.sksharma.automation.tests.AmazonHomePageTest.shouldShowSearchInput",
            "com.sksharma.automation.tests.AmazonHomePageTest.shouldEnableSearchInput",
            "com.sksharma.automation.tests.AmazonSearchTest.shouldDisplaySearchResultsForSearchTerm",
            "com.sksharma.automation.tests.AmazonSearchTest.shouldRetainSearchStateAfterRefresh",
            "com.sksharma.automation.tests.AmazonSearchTest.shouldReturnToHomepageWithBrowserBack"
    );
    private final ConcurrentHashMap<ITestResult, Integer> invocations =
            new ConcurrentHashMap<>();
    private final LongAdder screenshotSuccesses = new LongAdder();
    private final LongAdder screenshotFailures = new LongAdder();
    private final LongAdder configurationFailures = new LongAdder();
    private Instant startTime;
    private String executionId;

    public synchronized void start() {
        invocations.clear();
        screenshotSuccesses.reset();
        screenshotFailures.reset();
        configurationFailures.reset();
        startTime = Instant.now();
        executionId = UUID.randomUUID().toString();
    }

    public void record(ITestResult result) {
        invocations.putIfAbsent(result, result.getStatus());
    }

    public void screenshotCaptured() { screenshotSuccesses.increment(); }
    public void screenshotCaptureFailed() { screenshotFailures.increment(); }
    public void configurationFailed() { configurationFailures.increment(); }

    public synchronized void writeSummary() throws IOException {
        Instant endTime = Instant.now();
        long passed = 0, failed = 0, skipped = 0, durationTotal = 0;
        Set<String> scenarios = new HashSet<>();
        for (ITestResult result : invocations.keySet()) {
            scenarios.add(result.getTestClass().getName() + "."
                    + result.getMethod().getMethodName());
            durationTotal += Math.max(0, result.getEndMillis() - result.getStartMillis());
            int status = invocations.get(result);
            if (status == ITestResult.SUCCESS) passed++;
            else if (status == ITestResult.FAILURE) failed++;
            else if (status == ITestResult.SKIP) skipped++;
        }
        long total = invocations.size();
        Set<String> executedMethods = invocations.entrySet().stream()
                .filter(entry -> entry.getValue() != ITestResult.SKIP)
                .map(entry -> methodName(entry.getKey()))
                .collect(java.util.stream.Collectors.toSet());
        long executedInScope = executedMethods.stream()
                .filter(SCENARIO_IDS::containsKey).count();
        long suiteDuration = Math.max(0, endTime.toEpochMilli()
                - startTime.toEpochMilli());
        double passRate = passed + failed == 0 ? 0.0
                : passed * 100.0 / (passed + failed);
        Set<String> documentedScenarios = documentedScenarios();
        long unverifiedCandidates = scenarioDefinitions().stream()
                .filter(line -> line.startsWith("UNVERIFIED|"))
                .count();
        long documented = documentedScenarios.size();
        long automated = documentedScenarios.stream()
                .filter(method -> AUTOMATED_SCENARIOS.contains(
                        "com.sksharma.automation.tests." + method)).count();
        double coverage = documented == 0 ? 0.0 : automated * 100.0 / documented;
        double executionCoverage = documented == 0 ? 0.0
                : executedInScope * 100.0 / documented;
        long screenshotAttempts = screenshotSuccesses.sum() + screenshotFailures.sum();
        double screenshotRate = screenshotAttempts == 0 ? 0.0
                : screenshotSuccesses.sum() * 100.0 / screenshotAttempts;
        Path directory = Path.of("reports", "analytics");
        Files.createDirectories(directory);
        Path output = directory.resolve("execution-" + startTime.toEpochMilli()
                + "-" + executionId + ".json");
        String json = "{\n"
                + "  \"executionId\": \"" + executionId + "\",\n"
                + "  \"startTime\": \"" + startTime + "\",\n"
                + "  \"endTime\": \"" + endTime + "\",\n"
                + "  \"totalDurationMs\": " + suiteDuration + ",\n"
                + "  \"totalInvocations\": " + total + ",\n"
                + "  \"uniqueScenarios\": " + scenarios.size() + ",\n"
                + "  \"passed\": " + passed + ",\n"
                + "  \"failed\": " + failed + ",\n"
                + "  \"skipped\": " + skipped + ",\n"
                + "  \"configurationFailures\": " + configurationFailures.sum() + ",\n"
                + "  \"passRate\": " + decimal(passRate) + ",\n"
                + "  \"averageTestDurationMs\": "
                + (total == 0 ? 0 : durationTotal / total) + ",\n"
                + "  \"screenshotCaptureSuccessRate\": "
                + decimal(screenshotRate) + ",\n"
                + "  \"screenshotCaptureSuccesses\": " + screenshotSuccesses.sum() + ",\n"
                + "  \"screenshotCaptureFailures\": " + screenshotFailures.sum() + ",\n"
                + "  \"automatedScenarios\": " + automated + ",\n"
                + "  \"documentedScenarios\": " + documented + ",\n"
                + "  \"unverifiedCandidates\": " + unverifiedCandidates + ",\n"
                + "  \"automationCoveragePercentage\": " + decimal(coverage) + ",\n"
                + "  \"executedInScopeScenarios\": " + executedInScope + ",\n"
                + "  \"executionScenarioCoveragePercentage\": "
                + decimal(executionCoverage) + ",\n"
                + "  \"invocations\": [\n" + invocationDetails() + "  ]\n"
                + "}\n";
        Files.writeString(output, json, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }

    private String invocationDetails() {
        var results = invocations.keySet().stream()
                .sorted(java.util.Comparator.comparingLong(ITestResult::getStartMillis))
                .toList();
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            ITestResult result = results.get(i);
            String name = methodName(result);
            String status = switch (invocations.get(result)) {
                case ITestResult.SUCCESS -> "passed";
                case ITestResult.FAILURE -> "failed";
                case ITestResult.SKIP -> "skipped";
                default -> "unknown";
            };
            details.append("    {\"scenarioId\": \"")
                    .append(SCENARIO_IDS.getOrDefault(name, "UNSCOPED"))
                    .append("\", \"scenario\": \"").append(name)
                    .append("\", \"status\": \"").append(status)
                    .append("\", \"durationMs\": ")
                    .append(Math.max(0, result.getEndMillis() - result.getStartMillis()))
                    .append("}").append(i + 1 == results.size() ? "\n" : ",\n");
        }
        return details.toString();
    }

    private Set<String> documentedScenarios() throws IOException {
        return scenarioDefinitions().stream()
                .filter(line -> line.startsWith("IMPLEMENTED|"))
                .map(line -> line.split("\\|", 3)[2])
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private java.util.List<String> scenarioDefinitions() throws IOException {
        try (var lines = Files.lines(Path.of("src", "test", "resources",
                "in-scope-scenarios.txt"))) {
            return lines.map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
        }
    }

    private static String methodName(ITestResult result) {
        return result.getTestClass().getName() + "."
                + result.getMethod().getMethodName();
    }

    private String decimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
