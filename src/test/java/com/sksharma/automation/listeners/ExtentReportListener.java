package com.sksharma.automation.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.ExtentReports;
import com.sksharma.automation.reports.ExtentReportManager;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ExtentReportListener implements ITestListener {

    private static final ExtentReports EXTENT_REPORTS =
            ExtentReportManager.getInstance();

    private static final ThreadLocal<ExtentTest> EXTENT_TEST =
            new ThreadLocal<>();

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = EXTENT_REPORTS.createTest(
                result.getMethod().getMethodName()
        );

        EXTENT_TEST.set(test);
        test.log(Status.INFO, "Test execution started.");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        getTest().pass("Test passed successfully.");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        getTest().fail(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        getTest().skip("Test skipped.");
    }

    @Override
    public void onFinish(org.testng.ITestContext context) {
        EXTENT_REPORTS.flush();
        EXTENT_TEST.remove();
    }

    private ExtentTest getTest() {
        return EXTENT_TEST.get();
    }
}