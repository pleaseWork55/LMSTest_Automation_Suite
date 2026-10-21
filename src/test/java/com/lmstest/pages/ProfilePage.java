package com.lmstest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.lmstest.base.BaseTest;

public class ProfilePage {

    

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By heading    = By.cssSelector("h1");
    private final By mainRegion = By.id("region-main");
    private final By cityField  = By.id("id_city");
    private final By saveButton = By.id("id_submitbutton");

    public ProfilePage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    // ── View profile ──────────────────────────────────────────────────────
    public ProfilePage openProfile() {
        driver.get(BaseTest.BASE_URL + "/user/profile.php");
        wait.until(ExpectedConditions.urlContains("/user/profile.php"));
        return this;
    }

    public String getHeading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(heading)).getText().trim();
    }

    public String getMainText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(mainRegion)).getText();
    }

    // ── Edit profile ──────────────────────────────────────────────────────
    public ProfilePage openEditForm() {
        driver.get(BaseTest.BASE_URL + "/user/edit.php");
        wait.until(ExpectedConditions.visibilityOfElementLocated(cityField));
        return this;
    }

    public ProfilePage setCity(String city) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(cityField));
        field.clear();
        field.sendKeys(city);
        return this;
    }

    /** Saves the form. Moodle redirects to preferences.php (or profile.php, depending on version). */
    public void saveChanges() {
        wait.until(ExpectedConditions.elementToBeClickable(saveButton)).click();
        wait.until(ExpectedConditions.urlMatches(".*/user/(preferences|profile)\\.php.*"));
    }

    public String getCityValue() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(cityField)).getAttribute("value");
    }
}