package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.LoginPage;
import com.lmstest.pages.NavigationPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class NavigationTests extends BaseTest {

    @BeforeEach
    void login() {
        new LoginPage(driver, wait).open().loginAs(STUDENT_USER, STUDENT_PASS);
    }

    @Test
    @DisplayName("Main navigation links work")
    void mainNavigationLinksWork() {
        NavigationPage nav = new NavigationPage(driver, wait);

        nav.clickMyCourses();
        assertTrue(driver.getCurrentUrl().contains("/my/courses.php"), "My courses link should open /my/courses.php");

        nav.clickDashboard();
        assertTrue(nav.getHeading().contains("Dashboard"), "Dashboard link should show the Dashboard heading");

        nav.clickHome();
        assertTrue(driver.getCurrentUrl().contains("school.moodledemo.net"), "Home link should open the site home page");
    }

    @Test
    @DisplayName("Calendar page shows current month")
    void calendarShowsCurrentMonth() {
        // e.g. "October 2026"
        String monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

        NavigationPage nav = new NavigationPage(driver, wait).openMonthCalendar();

        assertTrue(nav.getCalendarText().contains(monthYear),
                "Calendar should show the current month: " + monthYear);
    }
}