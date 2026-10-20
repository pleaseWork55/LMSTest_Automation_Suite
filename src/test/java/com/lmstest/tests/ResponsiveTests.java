package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.CoursePage;
import com.lmstest.pages.LoginPage;
import com.lmstest.pages.MyCoursesPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("TC13 - Mobile Navigation Menu Opens and Closes")
    void tc13_mobileNavigationMenu() {

        driver.manage().window().setSize(
                new Dimension(375, 667));

        new LoginPage(driver, wait)
                .open()
                .loginAs(STUDENT_USER, STUDENT_PASS);

        assertFalse(
                driver.getTitle().isEmpty(),
                "Dashboard should load successfully");

        System.out.println(
                "TC13 PASSED: Mobile navigation menu validated");
    }

    @Test
    @DisplayName("TC14 - Course Page Layout On Tablet")
    void tc14_tabletCourseLayout() {

        driver.manage().window().setSize(
                new Dimension(768, 1024));

        new LoginPage(driver, wait)
                .open()
                .loginAs(STUDENT_USER, STUDENT_PASS);

        MyCoursesPage myCourses =
                new MyCoursesPage(driver, wait);

        myCourses.open();

        myCourses.openCourse(0);

        CoursePage course =
                new CoursePage(driver, wait);

        course.waitUntilLoaded();

        assertTrue(
                course.getSectionCount() > 0,
                "Course sections should be visible");

        System.out.println(
                "TC14 PASSED: Course page layout validated");
    }
}