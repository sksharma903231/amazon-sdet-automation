
package com.sksharma.automation.tests;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import com.sksharma.automation.driver.DriverFactory;
import org.testng.ITestResult;

public class BaseTest {

    protected WebDriver driver;

    @BeforeMethod
    public void setUp() {
        driver = DriverFactory.createDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult testResult) throws Throwable {
        try {
            DriverFactory.quitDriver();
        } catch (Throwable teardownFailure) {
            Throwable testFailure = testResult.getThrowable();
            if (testResult.getStatus() == ITestResult.FAILURE && testFailure != null) {
                testFailure.addSuppressed(teardownFailure);
                return;
            }
            throw teardownFailure;
        } finally {
            driver = null;
        }
    }
}
