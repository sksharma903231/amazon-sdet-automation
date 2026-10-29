package com.sksharma.automation.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public final class ExtentReportManager {

    private static ExtentReports extentReports;

    private ExtentReportManager() {
        // Prevent object creation.
    }

    public static ExtentReports getInstance() {
        if (extentReports == null) {
            ExtentSparkReporter sparkReporter =
                    new ExtentSparkReporter("reports/extent-report.html");

            sparkReporter.config().setDocumentTitle("Amazon SDET Automation");
            sparkReporter.config().setReportName("Automation Test Results");

            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            extentReports.setSystemInfo("Project", "Amazon SDET Automation");
            extentReports.setSystemInfo("Framework", "Selenium + TestNG");
        }

        return extentReports;
    }
}