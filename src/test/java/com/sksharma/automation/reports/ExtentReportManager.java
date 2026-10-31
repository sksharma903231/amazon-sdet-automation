
package com.sksharma.automation.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ExtentReportManager {

    private static ExtentReports extentReports;

    private ExtentReportManager() {
        // Prevent object creation.
    }

    public static synchronized void startExecution() {
        if (extentReports != null) {
            throw new IllegalStateException(
                    "An ExtentReports execution is already active."
            );
        }

        Path reservedReportPath = null;
        try {
            Path reportsDirectory = Paths.get("reports");
            Files.createDirectories(reportsDirectory);

            reservedReportPath = reserveReportPath(reportsDirectory);
            ExtentSparkReporter sparkReporter =
                    new ExtentSparkReporter(reservedReportPath.toString());

            sparkReporter.config().setDocumentTitle(
                    "E-commerce UI Automation"
            );
            sparkReporter.config().setReportName(
                    "Decathlon India Test Results"
            );

            ExtentReports reports = new ExtentReports();
            reports.attachReporter(sparkReporter);
            reports.setSystemInfo("Project", "E-commerce UI Automation");
            reports.setSystemInfo("Application Under Test", "Decathlon India");
            reports.setSystemInfo("Framework", "Selenium + TestNG");
            extentReports = reports;
        } catch (IOException | RuntimeException e) {
            removeReservedReport(reservedReportPath, e);
            throw new IllegalStateException(
                    "Unable to initialize the ExtentReports execution.", e
            );
        }
    }

    public static synchronized ExtentReports getInstance() {
        if (extentReports == null) {
            throw new IllegalStateException(
                    "ExtentReports has not been initialized for this execution."
            );
        }
        return extentReports;
    }

    public static synchronized com.aventstack.extentreports.ExtentTest createTest(
            String name) {
        if (extentReports == null) {
            throw new IllegalStateException(
                    "ExtentReports has not been initialized for this execution."
            );
        }
        return extentReports.createTest(name);
    }

    public static synchronized void finishExecution() {
        if (extentReports == null) {
            return;
        }

        ExtentReports reports = extentReports;
        try {
            reports.flush();
        } finally {
            extentReports = null;
        }
    }

    private static Path reserveReportPath(Path reportsDirectory)
            throws IOException {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
        );
        String baseName = "ExtentReport-" + timestamp;

        for (int suffix = 0; ; suffix++) {
            String fileName = suffix == 0
                    ? baseName + ".html"
                    : baseName + "-" + suffix + ".html";
            Path reportPath = reportsDirectory.resolve(fileName);
            try {
                return Files.createFile(reportPath);
            } catch (FileAlreadyExistsException e) {
                // Preserve prior reports and try the next unique name.
            }
        }
    }

    private static void removeReservedReport(Path reportPath,
                                             Exception failure) {
        if (reportPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(reportPath);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
