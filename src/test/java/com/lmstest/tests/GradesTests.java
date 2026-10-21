package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.GradesPage;
import com.lmstest.pages.LoginPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GradesTests extends BaseTest {

    @BeforeEach
    void login() {
        new LoginPage(driver, wait).open().loginAs(STUDENT_USER, STUDENT_PASS);
    }

    @Test
    @DisplayName("Grades overview opens")
    void gradesOverviewOpens() {
        GradesPage grades = new GradesPage(driver, wait).openOverview();

        // The first <h1> is the user's name on this Moodle version, so check the page title instead
        assertTrue(grades.getPageTitle().toLowerCase().contains("grades"),
                "Page title should contain 'grades' but was: " + grades.getPageTitle());
        assertTrue(grades.getMainText().contains("Courses I am taking"),
                "Overview should list the user's courses");
    }

    @Test
    @DisplayName("Course grade report opens")
    void courseGradeReportOpens() {
        GradesPage grades = new GradesPage(driver, wait).openOverview();

        grades.openFirstCourseReport();

        assertTrue(driver.getCurrentUrl().matches(".*/(course/user\\.php|grade/report/user/index\\.php).*"),
                "Should be on a course grade report but was: " + driver.getCurrentUrl());
        assertTrue(grades.isGradeTableDisplayed(), "The course grade report should display a grade table");
    }
}