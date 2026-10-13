
package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.LoginPage;
import com.lmstest.pages.MyCoursesPage;
import com.lmstest.pages.CoursePage;
import com.lmstest.pages.ForumPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
                .loginAs(STUDENT_USER, STUDENT_PASS);
    }

    private void openTargetForum() {
        MyCoursesPage courses = new MyCoursesPage(driver, wait);

        courses.open();
        courses.openCourseByName(COURSE_NAME);

        new CoursePage(driver, wait).waitUntilLoaded();

        ForumPage forum = new ForumPage(driver, wait);
        forum.openForum(FORUM_NAME);
    }

    @Test
    @DisplayName("TC09 - Open forum and view discussion")
    void tc09_viewForumDiscussion() {
        openTargetForum();

        ForumPage forum = new ForumPage(driver, wait);
        forum.openDiscussion(DISCUSSION_TITLE);

        assertTrue(
                forum.isDiscussionDisplayed(DISCUSSION_TITLE),
                "The expected discussion title should be displayed."
        );

        assertTrue(
                forum.isPostContentDisplayed(),
                "The discussion post content should be visible."
        );
    }

    @Test
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
}
