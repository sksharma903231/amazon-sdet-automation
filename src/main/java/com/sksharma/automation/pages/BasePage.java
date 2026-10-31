package com.sksharma.automation.pages;

import com.sksharma.automation.factory.MethodFactory;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

public class BasePage {

    protected final WebDriver driver;
    protected final MethodFactory methods;

    public BasePage(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "WebDriver must not be null");
        this.methods = new MethodFactory(driver);
    }
}
