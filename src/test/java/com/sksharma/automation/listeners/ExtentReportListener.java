
package com.sksharma.automation.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.ExtentReports;
import com.sksharma.automation.reports.ExtentReportManager;
import com.sksharma.automation.reporting.TestLogger;
import com.sksharma.automation.utils.LoggerUtils;
import org.apache.logging.log4j.Logger;
import org.testng.IExecutionListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ExtentReportListener implements IExecutionListener, ITestListener {

    private static final Logger LOGGER =
            LoggerUtils.getLogger(ExtentReportListener.class);

    @Override
    public void onExecutionStart() {
        try {
            ExtentReportManager.startExecution();
        } catch (RuntimeException | AssertionError reportingFailure) {
            recordReportingFailure(null, "execution start", reportingFailure);
        }
    }

    @Override
    public void onExecutionFinish() {
        try {
            ExtentReportManager.finishExecution();
        } catch (RuntimeException | AssertionError reportingFailure) {
            recordReportingFailure(null, "execution finish", reportingFailure);
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        TestLogger.removeTest();
        try {
            ExtentTest test = createTest(result);
            TestLogger.setTest(test);
            TestLogger.info("Test execution started.");
        } catch (RuntimeException | AssertionError reportingFailure) {
            TestLogger.removeTest();
            recordReportingFailure(result, "test start", reportingFailure);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        completeTest(result, "success", () ->
                TestLogger.pass("Test passed successfully."));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        completeTest(result, "failure", () -> {
            Throwable failure = result.getThrowable();
            if (failure == null) {
                TestLogger.fail("Test failed without a throwable.");
            } else {
                TestLogger.fail(failure);
            }
        });
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        completeTest(result, "skip", () ->
                TestLogger.skip("Test skipped."));
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        completeTest(result, "success-percentage failure", () -> {
            Throwable failure = result.getThrowable();
            if (failure == null) {
                TestLogger.fail(
                        "Test invocation failed within the success percentage."
                );
            } else {
                TestLogger.fail(failure);
            }
        });
    }

    private void completeTest(ITestResult result, String event,
                              Runnable reportOutcome) {
        try {
            ensureTestContext(result);
            reportOutcome.run();
        } catch (RuntimeException | AssertionError reportingFailure) {
            recordReportingFailure(result, event, reportingFailure);
        } finally {
            TestLogger.removeTest();
        }
    }

    private void ensureTestContext(ITestResult result) {
        if (TestLogger.hasTest()) {
            return;
        }

        TestLogger.setTest(createTest(result));
        TestLogger.info(
                "Test result received without an active test-start context."
        );
    }

    private ExtentTest createTest(ITestResult result) {
        ExtentReports extentReports = ExtentReportManager.getInstance();
        String testName = result.getTestClass().getName()
                + "." + result.getMethod().getMethodName();
        return extentReports.createTest(testName);
    }

    private void recordReportingFailure(ITestResult result, String event,
                                        Throwable reportingFailure) {
        if (result != null) {
            try {
                Throwable testFailure = result.getThrowable();
                if (testFailure != null && testFailure != reportingFailure) {
                    addSuppressedSafely(testFailure, reportingFailure);
                }
            } catch (RuntimeException | AssertionError resultFailure) {
                addSuppressedSafely(reportingFailure, resultFailure);
            }
        }

        String testName = "<execution>";
        if (result != null) {
            try {
                testName = result.getName();
            } catch (RuntimeException | AssertionError nameFailure) {
                addSuppressedSafely(reportingFailure, nameFailure);
            }
        }

        try {
            LOGGER.error(
                    "Unable to record Extent report event '{}' for test {}.",
                    event,
                    testName,
                    reportingFailure
            );
        } catch (RuntimeException | AssertionError diagnosticFailure) {
            addSuppressedSafely(reportingFailure, diagnosticFailure);
        }
    }

    private void addSuppressedSafely(Throwable primary, Throwable secondary) {
        if (primary == secondary) {
            return;
        }
        primary.addSuppressed(secondary);
    }
}
