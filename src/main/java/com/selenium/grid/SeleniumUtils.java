package com.selenium.grid;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class SeleniumUtils {

    public static void takeScreenShot(WebDriver driver){
        Random random = new Random();
        random.nextInt();
        File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(source, new File("/Users/suma/temp/"+random.nextInt(100)+ ".jpeg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
