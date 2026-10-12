package com.lmstest.pages;

import com.lmstest.base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButton = By.id("loginbtn");

    // Moodle login error message
    private final By loginError = By.id("loginerrormessage");

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public LoginPage open() {
        driver.get(BaseTest.BASE_URL + "/login/index.php");

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(usernameField)
        );

        return this;
    }

    /**
     * Enters username and password, then clicks Login.
     *
     * This method does not wait for a redirect, so it can be used
     * for both valid and invalid credentials.
     */
    public void login(String username, String password) {
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(usernameField)
        ).clear();

        driver.findElement(usernameField).sendKeys(username);

        driver.findElement(passwordField).clear();
        driver.findElement(passwordField).sendKeys(password);

        clickLogin();
    }

    /**
     * Performs a successful login and waits until Moodle
     * leaves the login page.
     */
    public void loginAs(String username, String password) {
        login(username, password);

        wait.until(
                ExpectedConditions.not(
                        ExpectedConditions.urlContains("/login/")
                )
        );
    }

    /**
     * Clicks the Login button.
     */
    public void clickLogin() {
        wait.until(
                ExpectedConditions.elementToBeClickable(loginButton)
        ).click();
    }

    /**
     * Checks whether Moodle displays a login error.
     */
    public boolean isErrorDisplayed() {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(loginError)
        ).isDisplayed();
    }

    /**
     * Returns the current browser page title.
     */
    public String getTitle() {
        return driver.getTitle();
    }
}