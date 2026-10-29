package com.sksharma.automation.pages;

import org.openqa.selenium.WebDriver;

public class ExamplePage extends BasePage{
    public ExamplePage(WebDriver driver){
        super(driver);
    }
    public String getPageTitle(){
        return driver.getTitle();
    }
}