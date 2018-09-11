package com.selenium.grid;

import org.openqa.selenium.WebDriver;

public class GoogleTest {

    public static void main(String[] args) {
        SeleniumContext context = new SeleniumContext();
        WebDriver driver = context.getDriver();
        driver.get("https://www.google.com");
        SeleniumUtils.takeScreenShot(driver);


        try {
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
