
package com.sksharma.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FrameworkSetupTest {

    @Test
    public void verifyTestNGIsWorking() {
        String expected = "amazon-sdet-automation";
        String actual = "amazon-sdet-automation";

        Assert.assertEquals(actual, expected,
                "The project name should match the expected value.");
    }
}
