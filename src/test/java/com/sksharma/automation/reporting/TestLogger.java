
package com.sksharma.automation.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.sksharma.automation.utils.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public final class TestLogger {

    private static final ThreadLocal<ExtentTest> EXTENT_TEST =
            new ThreadLocal<>();
    private static final Logger LOGGER = LoggerUtils.getLogger(TestLogger.class);

    private TestLogger() {
        // Prevent object creation.
    }

    public static void setTest(ExtentTest test) {
        EXTENT_TEST.set(test);
    }

    public static void info(String message) {
        log("info", test -> test.log(Status.INFO, message));
    }

    public static void pass(String message) {
        log("pass", test -> test.pass(message));
    }

    public static void fail(String message) {
        log("failure", test -> test.fail(message));
    }

    public static void fail(Throwable throwable) {
        log("failure", test -> test.fail(throwable));
    }

    public static void skip(String message) {
        log("skip", test -> test.skip(message));
    }

    public static void attachScreenshot(String path) {
        log("screenshot", test ->
                test.addScreenCaptureFromPath(path, "Failure screenshot"));
    }

    public static void removeTest() {
        EXTENT_TEST.remove();
    }

    public static boolean hasTest() {
        return EXTENT_TEST.get() != null;
    }

    private static void log(String event, Consumer<ExtentTest> operation) {
        ExtentTest test = EXTENT_TEST.get();
        if (test == null) {
            recordLoggingFailure(
                    event,
                    new IllegalStateException(
                            "No ExtentTest is associated with the current thread."
                    )
            );
            return;
        }

        try {
            operation.accept(test);
        } catch (RuntimeException | AssertionError loggingFailure) {
            recordLoggingFailure(event, loggingFailure);
        }
    }

    private static void recordLoggingFailure(String event,
                                             Throwable loggingFailure) {
        try {
            LOGGER.error("Unable to write '{}' to the Extent report.",
                    event, loggingFailure);
        } catch (RuntimeException | AssertionError diagnosticFailure) {
            if (diagnosticFailure != loggingFailure) {
                loggingFailure.addSuppressed(diagnosticFailure);
            }
        }
    }
}
