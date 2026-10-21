package com.lmstest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.lmstest.base.BaseTest;

public class NavigationPage {


    private final WebDriver driver;
    private final WebDriverWait wait;

    public NavigationPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    // ── Main navigation ───────────────────────────────────────────────────
    public void clickMyCourses() {
        clickNav(".primary-navigation a[href*='/my/courses.php']");
        wait.until(ExpectedConditions.urlContains("/my/courses.php"));
    }

    public void clickDashboard() {
        clickNav(".primary-navigation a[href$='/my/']");
        wait.until(ExpectedConditions.urlMatches(".*/my/?$"));
    }

    public void clickHome() {
        clickNav(".primary-navigation a[href*='redirect=0'], "
               + ".primary-navigation a[href$='school.moodledemo.net/']");
        wait.until(ExpectedConditions.urlMatches(".*school\\.moodledemo\\.net/(\\?redirect=0)?$"));
    }

    public String getHeading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1"))).getText();
    }

    private void clickNav(String css) {
        // Re-find on every call: the page reloads between clicks (avoids stale elements)
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css))).click();
    }

    // ── Calendar ──────────────────────────────────────────────────────────
    public NavigationPage openMonthCalendar() {
        driver.get(BaseTest.BASE_URL + "/calendar/view.php?view=month");
        wait.until(ExpectedConditions.urlContains("/calendar/view.php"));
        return this;
    }

    public String getCalendarText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".calendarwrapper, [data-region='calendar']"))).getText();
    }
}