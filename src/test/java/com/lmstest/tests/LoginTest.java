package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.LoginPage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginTest extends BaseTest {

    @Test
    public void TC01_validStudentLogin() {
        LoginPage loginPage = new LoginPage(driver, wait);

        loginPage.open();
        loginPage.loginAs(Config.USERNAME, Config.PASSWORD);

        assertTrue(
                loginPage.getTitle().contains("Dashboard"),
                "Student was not redirected to the Dashboard"
        );

        System.out.println("TC01 PASSED: Student logged in successfully");
    }

    @Test
    public void TC02_invalidStudentLogin() {
        LoginPage loginPage = new LoginPage(driver, wait);

        loginPage.open();
        loginPage.login("wrong_username", "wrong_password");

        assertTrue(
                loginPage.isErrorDisplayed(),
                "Login error was not displayed"
        );

        assertFalse(
                loginPage.getTitle().contains("Dashboard"),
                "Invalid student was logged in"
        );

        System.out.println("TC02 PASSED: Invalid login was rejected");
    }

    @Test
    public void TC03_blankStudentLogin() {
        LoginPage loginPage = new LoginPage(driver, wait);

        loginPage.open();
        loginPage.clickLogin();

        assertTrue(
                loginPage.isErrorDisplayed(),
                "Validation error was not displayed"
        );

        assertFalse(
                loginPage.getTitle().contains("Dashboard"),
                "Student was logged in with blank credentials"
        );

        System.out.println("TC03 PASSED: Blank login was rejected");
    }

   @Test
        public void TC04_studentCanLogout() {
        LoginPage loginPage = new LoginPage(driver, wait);

        loginPage.open();
        loginPage.loginAs(Config.USERNAME, Config.PASSWORD);

        loginPage.clickLogout();

        assertTrue(
                loginPage.isLoginLinkDisplayed(),
                "Login link was not displayed after logout"
        );

        System.out.println("TC04 PASSED: Student logged out successfully");
        }

    @Test
    public void TC05_forgotPasswordPageOpens() {
    LoginPage loginPage = new LoginPage(driver, wait);

    loginPage.open();
    loginPage.clickForgotPassword();

    assertTrue(
            loginPage.isForgotPasswordPageDisplayed(),
            "Forgot password page did not open"
    );

    System.out.println("TC05 PASSED: Forgot password page opened");
}

@Test
public void TC06_protectedPageRedirectsToLoginWhenLoggedOut() {
    LoginPage loginPage = new LoginPage(driver, wait);

    driver.get(BaseTest.BASE_URL + "/grade/report/overview/index.php");

    assertTrue(
            loginPage.isLoginPageDisplayed(),
            "User was not redirected to the Login page. Current URL: "
                    + driver.getCurrentUrl()
    );

    System.out.println(
            "TC06 PASSED: Protected page redirected logged-out user to Login"
    );
}
}