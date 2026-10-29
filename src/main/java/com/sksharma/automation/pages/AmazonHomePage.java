package com.sksharma.automation.pages;
import org.openqa.selenium.WebDriver;
public class AmazonHomePage extends BasePage{
    public AmazonHomePage(WebDriver driver){
        super(driver);
    }
    public String getPageTitle(){
        return driver.getTitle();
    }
    public String getCurrentUrl(){
        return driver.getCurrentUrl();
    }
}
