package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.LoginPage;
import com.lmstest.pages.ProfilePage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTests extends BaseTest {

    @BeforeEach
    void login() {
        new LoginPage(driver, wait).open().loginAs(STUDENT_USER, STUDENT_PASS);
    }

    @Test
    @DisplayName("View own profile")
    void viewOwnProfile() {
        ProfilePage profile = new ProfilePage(driver, wait).openProfile();

        assertFalse(profile.getHeading().isEmpty(), "Profile heading (user name) should not be empty");
        assertTrue(profile.getMainText().matches("(?is).*(user details|email address|course details).*"),
                "Profile page should show its detail sections");
    }

    @Test
    @DisplayName("Edit profile details")
    void editProfileDetails() {
        String newCity = "TestCity" + (System.currentTimeMillis() % 10000);

        ProfilePage profile = new ProfilePage(driver, wait).openEditForm();
        profile.setCity(newCity);
        profile.saveChanges();

        // Reopen the edit form and confirm the value was really saved
        profile.openEditForm();
        assertEquals(newCity, profile.getCityValue(), "City should be saved after submitting the form");
    }
}
