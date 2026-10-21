package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.LoginPage;
import com.lmstest.pages.MyCoursesPage;
import com.lmstest.pages.CoursePage;
import com.lmstest.pages.ForumPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ForumTests extends BaseTest {

    private static final String COURSE_NAME =
            "Moodle Mountain (LMS training 01)";

    private static final String FORUM_NAME =
            "This week's discussion topic..";

    private static final String DISCUSSION_TITLE =
            "Pollution in the mountains";

    @BeforeEach
    void login() {
        new LoginPage(driver, wait)
                .open()
                .loginAs(BaseTest.STUDENT_USER, BaseTest.STUDENT_PASS);
    }

    // Navigate to the course forum.
    private void openTargetForum() {
        MyCoursesPage courses = new MyCoursesPage(driver, wait);

        courses.open();
        courses.openCourseByName(COURSE_NAME);

        new CoursePage(driver, wait).waitUntilLoaded();

        ForumPage forum = new ForumPage(driver, wait);
        forum.openForum(FORUM_NAME);
    }

    @Test
    @Order(1)
    @DisplayName("TC09 - Open forum and view discussion")
    void tc09_viewForumDiscussion() {
        openTargetForum();

        ForumPage forum = new ForumPage(driver, wait);
        forum.openDiscussion(DISCUSSION_TITLE);

        assertTrue(
                forum.isTextDisplayed(DISCUSSION_TITLE),
                "The expected discussion title should be displayed."
        );

        assertTrue(
                forum.isPostContentDisplayed(),
                "The discussion post content should be visible."
        );
    }

    @Test
    @Order(2)
    @DisplayName("TC10 - Post a reply to a discussion")
    void tc10_postForumReply() {
        openTargetForum();

        ForumPage forum = new ForumPage(driver, wait);
        forum.openDiscussion(DISCUSSION_TITLE);

        String message =
                "Automated TC10 reply - " + System.currentTimeMillis();

        forum.clickReplyOnOriginalPost();
        forum.enterReply(message);
        forum.submitReply();

        assertTrue(
                forum.isReplyDisplayed(message),
                "The newly submitted reply should be displayed."
        );
    }

    @Test
    @Order(3)
    @DisplayName("TC11 - Create a forum discussion")
    void createForumPost() {
        openTargetForum();

        ForumPage forum = new ForumPage(driver, wait);

        String title =
                "Automation discussion - " + System.currentTimeMillis();
        String message =
                "This is an automated forum discussion.";

        forum.clickAddDiscussion();
        forum.createDiscussion(title, message);

        assertTrue(
                forum.isDiscussionDisplayed(title),
                "The newly created discussion should be displayed."
        );
    }

    @Test
    @Order(4)
    @DisplayName("TC12 - Edit own forum post")
    void tc12_editOwnForumPost() {
        openTargetForum();

        ForumPage forum = new ForumPage(driver, wait);

        String originalTitle =
                "Automation edit - " + System.currentTimeMillis();
        String originalMessage =
                "Original automated message - " + System.currentTimeMillis();

        forum.clickAddDiscussion();
        forum.createDiscussion(originalTitle, originalMessage);

        String updatedMessage =
                "Updated automated message - " + System.currentTimeMillis();

        forum.editDiscussion(originalTitle, updatedMessage);

        assertTrue(
                forum.isPostContentDisplayed(),
                "The forum post should be visible after editing."
        );

        assertTrue(
                forum.isTextDisplayed(updatedMessage),
                "The updated message should be displayed."
        );
    }

    @Test
    @Order(5)
    @DisplayName("TC13 - Delete own forum post")
    void tc13_deleteOwnForumPost() {
        openTargetForum();

        ForumPage forum = new ForumPage(driver, wait);

        String title =
                "Post to delete - " + System.currentTimeMillis();
        String message =
                "This post will be deleted by automation.";

        // Create a post before attempting to delete it.
        forum.clickAddDiscussion();
        forum.createDiscussion(title, message);

        // Delete the post and verify that it is gone.
        forum.deleteOwnForumPost(title);

        assertTrue(
                forum.isDiscussionAbsent(title),
                "The deleted discussion should no longer be displayed."
        );
    }
}