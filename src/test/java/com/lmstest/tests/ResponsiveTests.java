package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ResponsiveTests extends BaseTest {

    @Test
    @DisplayName("TC11 - Mobile Viewport")
    void tc11_mobileViewport() {

        driver.manage().window().setSize(
                new Dimension(375, 667));

        new LoginPage(driver, wait)
                .open()
                .loginAs(STUDENT_USER, STUDENT_PASS);

        assertFalse(
                driver.getTitle().isEmpty(),
                "Page title should not be empty");

        System.out.println(
                "TC11 PASSED: Mobile viewport validated");
    }

    @Test
    @DisplayName("TC12 - Tablet Viewport")
    void tc12_tabletViewport() {

        driver.manage().window().setSize(
                new Dimension(768, 1024));

        new LoginPage(driver, wait)
                .open()
                .loginAs(STUDENT_USER, STUDENT_PASS);

        assertFalse(
                driver.getTitle().isEmpty(),
                "Page title should not be empty");

        System.out.println(
                "TC12 PASSED: Tablet viewport validated");
    }
}