package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.ForumPage;
import com.lmstest.pages.LoginPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Forum test suite – covers the four required test cases:
 *
 *   TC11 – Create a forum post
 *   TC10 – Reply to a forum post
 *   TC12 – Edit own forum post
 *   TC13 – Delete own forum post
 *
 * Each test is self-contained: it logs in, navigates to the target forum,
 * and performs its action in isolation.
 *
 * Target site : https://school.moodledemo.net
 * Credentials : student / moodle26  (published on the demo site)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ForumTests extends BaseTest {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /**
     * An existing discussion that student can open and reply to (TC10).
     * This discussion must already exist in the demo site's forum.
     */
    private static final String EXISTING_DISCUSSION_TITLE =
            "Pollution in the mountains";

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @BeforeEach
    void login() {
        // Use a longer wait for login to handle the Moodle demo site redirect chain
        // (it bounces through services.apps.moodle.com before landing back).
        WebDriverWait longWait = new WebDriverWait(
                driver, java.time.Duration.ofSeconds(60));
        LoginPage loginPage = new LoginPage(driver, longWait);

        try {
            loginPage.open().loginAs(BaseTest.STUDENT_USER, BaseTest.STUDENT_PASS);
        } catch (org.openqa.selenium.TimeoutException e) {
            // Retry once more if the first attempt timed out
            loginPage.open().loginAs(BaseTest.STUDENT_USER, BaseTest.STUDENT_PASS);
        }

        // Use a 40-second wait throughout each test (the demo site is slow)
        wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(40));
    }

    /**
     * Navigate to the target course then to the target forum.
     * Uses the Moodle dashboard search link as primary path; falls back to
     * MyCoursesPage if the dashboard course card is available.
     * Returns a {@link ForumPage} ready for further interactions.
     */
    private ForumPage openTargetForum() {
        // Navigate directly to the forum using its known URL from a previous run.
        // This avoids brittle dashboard link matching and is the most reliable approach.
        // Forum URL discovered from earlier runs: /mod/forum/view.php?id=944
        driver.get(BaseTest.BASE_URL + "/mod/forum/view.php?id=944");

        // Wait until the forum view page is loaded
        wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions
                        .urlContains("/mod/forum/view.php")
        );

        // If we got redirected to login, the session expired — fail with clear message
        if (driver.getCurrentUrl().contains("/login/")) {
            throw new IllegalStateException(
                    "Session expired navigating to forum. URL: " + driver.getCurrentUrl());
        }

        return new ForumPage(driver, wait);
    }

    // -----------------------------------------------------------------------
    // TC11 – Create a forum post
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("TC11 - Create a forum post")
    void tc11_createForumPost() {
        ForumPage forum = openTargetForum();

        String title   = "Automation post - " + System.currentTimeMillis();
        String message = "This is an automated forum post created by TC11.";

        forum.clickAddDiscussion();
        forum.createDiscussion(title, message);

        assertTrue(
                forum.isDiscussionDisplayed(title),
                "TC11 FAIL – The newly created discussion title should be visible."
        );
    }

    // -----------------------------------------------------------------------
    // TC10 – Reply to a forum post
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("TC10 - Reply to a forum post")
    void tc10_replyToForumPost() {
        ForumPage forum = openTargetForum();

        // Open an existing discussion so we have a post to reply to
        forum.openDiscussion(EXISTING_DISCUSSION_TITLE);

        String reply = "Automated TC10 reply - " + System.currentTimeMillis();

        forum.clickReplyOnOriginalPost();
        forum.enterReply(reply);
        forum.submitReply(reply);   // passes message to enable proper post-submit wait

        assertTrue(
                forum.isReplyDisplayed(reply),
                "TC10 FAIL – The submitted reply should be visible on the discussion page."
        );
    }

    // -----------------------------------------------------------------------
    // TC12 – Edit own forum post
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("TC12 - Edit own forum post")
    void tc12_editOwnForumPost() {
        ForumPage forum = openTargetForum();

        // Create a post first so we have something to edit
        String title          = "Automation edit - " + System.currentTimeMillis();
        String originalMessage = "Original body - " + System.currentTimeMillis();

        forum.clickAddDiscussion();
        forum.createDiscussion(title, originalMessage);

        // Now edit it
        String updatedMessage = "Updated body - " + System.currentTimeMillis();
        forum.editDiscussion(title, updatedMessage);

        assertTrue(
                forum.isPostContentDisplayed(),
                "TC12 FAIL – The forum post should still be visible after editing."
        );
        assertTrue(
                forum.isTextDisplayed(updatedMessage),
                "TC12 FAIL – The updated message body should be visible."
        );
    }

    // -----------------------------------------------------------------------
    // TC13 – Delete own forum post
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("TC13 - Delete own forum post")
    void tc13_deleteOwnForumPost() {
        ForumPage forum = openTargetForum();

        // Create a post first so we have something to delete
        String title   = "Post to delete - " + System.currentTimeMillis();
        String message = "This post will be deleted by TC13 automation.";

        forum.clickAddDiscussion();
        forum.createDiscussion(title, message);

        // Delete and verify it is gone
        forum.deleteOwnForumPost(title);

        assertTrue(
                forum.isDiscussionAbsent(title),
                "TC13 FAIL – The deleted discussion should no longer be displayed."
        );
    }
}
