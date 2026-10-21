package com.lmstest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.lmstest.base.BaseTest;

public class GradesPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Course links in the overview table point to /course/user.php?mode=grade&id=..
    private final By courseGradeLinks = By.cssSelector("a[href*='/course/user.php?mode=grade']");

    public GradesPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public GradesPage openOverview() {
        driver.get(BaseTest.BASE_URL + "/grade/report/overview/index.php");
        wait.until(ExpectedConditions.urlContains("/grade/report/overview/index.php"));
        return this;
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getMainText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("region-main"))).getText();
    }

    public void openFirstCourseReport() {
        wait.until(ExpectedConditions.elementToBeClickable(courseGradeLinks)).click();
        wait.until(ExpectedConditions.urlMatches(".*/(course/user\\.php|grade/report/user/index\\.php).*"));
    }

    public boolean isGradeTableDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table"))).isDisplayed();
    }
}