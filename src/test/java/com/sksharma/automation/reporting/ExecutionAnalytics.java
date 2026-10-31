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
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.adidasBrandFilterChangesAndConstrainsListing", "DEC-FILTER-01"),
            Map.entry("com.sksharma.automation.tests.decathlon.DecathlonSearchTest.productDetailsMatchSelectedSearchResult", "DEC-PDP-01")
    );

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
        long unverified = unverifiedCount();
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
                + "  \"passedInvocations\": " + passedInvocations + ",\n"
                + "  \"failedInvocations\": " + failedInvocations + ",\n"
                + "  \"skippedInvocations\": " + skippedInvocations + ",\n"
                + "  \"configurationFailures\": " + configurationFailures.sum() + ",\n"
                + "  \"unverifiedCandidateScenarios\": " + unverified + ",\n"
                + "  \"screenshotCaptureSuccesses\": " + screenshotSuccesses.sum() + ",\n"
                + "  \"screenshotCaptureFailures\": " + screenshotFailures.sum() + ",\n"
                + "  \"scenarioResults\": " + scenarioJson(scenarioStatus) + ",\n"
                + "  \"invocations\": [\n" + invocationJson(ordered) + "  ]\n}\n";
        Files.writeString(output, json, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
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

    private static long unverifiedCount() throws IOException {
        Path inventory = Path.of("src/test/resources/in-scope-scenarios.txt");
        if (!Files.exists(inventory)) return 0;
        try (var lines = Files.lines(inventory)) {
            return lines.filter(line -> line.startsWith("UNVERIFIED|DEC-")).count();
        }
    }

    private String invocationJson(List<ITestResult> values) {
        List<String> items = new ArrayList<>();
        for (ITestResult result : values) {
            String method = methodName(result);
            items.add("    {\"scenarioId\":\"" + SCENARIO_IDS.getOrDefault(method, "UNSCOPED")
                    + "\",\"method\":\"" + escape(method) + "\",\"status\":\""
                    + statusName(results.get(result)) + "\",\"durationMs\":"
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
