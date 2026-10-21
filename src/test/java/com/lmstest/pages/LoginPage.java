package com.lmstest.pages;

import com.lmstest.base.BaseTest;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButton = By.id("loginbtn");
    private final By loginError = By.id("loginerrormessage");
    private final By loginLink = By.cssSelector( "a[href$='/login/index.php']");
    // Moodle forgot-password link
    private final By forgotPasswordLink = By.cssSelector(
            "a[href*='/login/forgot_password.php']"
    );

    // Moodle user menu and logout link
    private final By userMenuButton = By.id("user-menu-toggle");

    private final By logoutLink = By.cssSelector(
            "a[href*='/login/logout.php']"
    );

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

    public void login(String username, String password) {
        try {
            fillCredentials(username, password);
        } catch (StaleElementReferenceException e) {
            // The login page re-rendered while typing; fill the fields again on the fresh DOM
            fillCredentials(username, password);
        }

        clickLogin();
    }

    private void fillCredentials(String username, String password) {
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(usernameField)
        ).clear();

        driver.findElement(usernameField).sendKeys(username);

        driver.findElement(passwordField).clear();
        driver.findElement(passwordField).sendKeys(password);
    }

    public void loginAs(String username, String password) {
        login(username, password);

        wait.until(
                ExpectedConditions.not(
                        ExpectedConditions.urlContains("/login/")
                )
        );
    }

    public void clickLogin() {
        wait.until(
                ExpectedConditions.elementToBeClickable(loginButton)
        ).click();
    }

    public boolean isErrorDisplayed() {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(loginError)
        ).isDisplayed();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public void clickForgotPassword() {
        wait.until(
                ExpectedConditions.elementToBeClickable(forgotPasswordLink)
        ).click();
    }

    public boolean isForgotPasswordPageDisplayed() {
        return wait.until(
                ExpectedConditions.urlContains("/login/forgot_password.php")
        );
    }

    public void clickLogout() {
        wait.until(
                ExpectedConditions.elementToBeClickable(userMenuButton)
        ).click();

        wait.until(
                ExpectedConditions.elementToBeClickable(logoutLink)
        ).click();
    }

    public boolean isLoginPageDisplayed() {
    return driver.getCurrentUrl().contains("/login/index.php");
}
public boolean isLoginLinkDisplayed() {
    return wait.until(
            ExpectedConditions.visibilityOfElementLocated(loginLink)
    ).isDisplayed();
}
public boolean isLoggedOut() {
    String currentUrl = driver.getCurrentUrl();

    return currentUrl.equals(BaseTest.BASE_URL + "/")
            || currentUrl.contains("/login/index.php");
}
}