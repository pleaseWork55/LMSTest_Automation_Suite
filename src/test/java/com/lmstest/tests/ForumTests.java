package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.ForumPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Forum test suite – four test cases run in sequence.
 *
 *   TC11 – Create a forum post
 *   TC10 – Reply to a forum post
 *   TC12 – Edit own forum post
 *   TC13 – Delete own forum post
 *
 * Target site : https://school.moodledemo.net
 * Credentials : student / moodle26
 *
 * Each test creates its own Chrome session, logs in with cookie-clearing
 * (to bypass the SSO redirect to services.apps.moodle.com), and runs in
 * SAME_THREAD mode to ensure sequential execution.
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ForumTests extends BaseTest {

    private static final String EXISTING_DISCUSSION_TITLE = "Pollution in the mountains";

    // -------------------------------------------------------------------------
    // Per-test login — clears cookies to bypass SSO
    // -------------------------------------------------------------------------

    @BeforeEach
    void login() {
        org.openqa.selenium.JavascriptExecutor js =
                (org.openqa.selenium.JavascriptExecutor) driver;

        // Attempt login up to 4 times — each attempt fully clears state and retries
        boolean loggedIn = false;
        for (int attempt = 1; attempt <= 4 && !loggedIn; attempt++) {
            System.out.println("=== Login attempt " + attempt + " ===");

            // 1. Clear all browser state
            driver.get("about:blank");
            driver.manage().deleteAllCookies();
            try {
                driver.get(BaseTest.BASE_URL + "/login/index.php");
                js.executeScript("try{localStorage.clear();}catch(e){}");
                js.executeScript("try{sessionStorage.clear();}catch(e){}");
            } catch (Exception ignored) {}
            driver.manage().deleteAllCookies();

            // 2. Navigate to login page and wait for native form (not SSO)
            driver.get(BaseTest.BASE_URL + "/login/index.php");
            boolean gotForm = false;
            try {
                gotForm = new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
                    String u = d.getCurrentUrl();
                    if (!u.contains("school.moodledemo.net") || u.contains("services.apps")) {
                        return false;
                    }
                    List<WebElement> els = d.findElements(By.id("username"));
                    return !els.isEmpty() && els.get(0).isDisplayed();
                });
            } catch (org.openqa.selenium.TimeoutException ignored) {}

            if (!gotForm) {
                System.out.println("Attempt " + attempt + ": login form not reached, URL: "
                        + driver.getCurrentUrl());
                continue; // clear state and retry
            }

            // 3. Fill credentials — always re-find elements fresh to avoid stale refs
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions
                                .elementToBeClickable(By.id("username")))
                        .clear();
                driver.findElement(By.id("username")).sendKeys(BaseTest.STUDENT_USER);

                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions
                                .elementToBeClickable(By.id("password")))
                        .clear();
                driver.findElement(By.id("password")).sendKeys(BaseTest.STUDENT_PASS);

                driver.findElement(By.id("loginbtn")).click();
            } catch (Exception e) {
                System.out.println("Attempt " + attempt
                        + ": error filling credentials: " + e.getMessage());
                continue;
            }

            // 4. Wait up to 60 s to leave the login page
            try {
                new WebDriverWait(driver, Duration.ofSeconds(60))
                        .until(d -> !d.getCurrentUrl().contains("/login/"));
                loggedIn = true;
            } catch (org.openqa.selenium.TimeoutException ignored) {
                System.out.println("Attempt " + attempt
                        + ": still on login after 60s, URL: " + driver.getCurrentUrl());
            }
        }

        if (!loggedIn) {
            throw new RuntimeException(
                    "Could not log in after 4 attempts. URL: " + driver.getCurrentUrl());
        }

        System.out.println("Logged in: " + driver.getCurrentUrl());

        // If landed on services.apps.moodle.com (OAuth relay), navigate to school directly
        if (driver.getCurrentUrl().contains("services.apps.moodle.com")) {
            driver.get(BaseTest.BASE_URL + "/my/");
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(d -> d.getCurrentUrl().contains("school.moodledemo.net")
                            && !d.getCurrentUrl().contains("/login/"));
            System.out.println("Navigated to school: " + driver.getCurrentUrl());
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    // -------------------------------------------------------------------------
    // Forum navigation
    // -------------------------------------------------------------------------

    private ForumPage openTargetForum() {
        driver.get(BaseTest.BASE_URL + "/mod/forum/view.php?id=944");

        // Wait up to 30 s for the forum page on school.moodledemo.net.
        // If we land on services.apps.moodle.com instead, navigate back.
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> {
                    String u = d.getCurrentUrl();
                    // If on services.apps.moodle.com, navigate back to school
                    if (u.contains("services.apps.moodle.com")
                            && u.contains("/mod/forum/view.php")) {
                        d.get(BaseTest.BASE_URL + "/mod/forum/view.php?id=944");
                        return false;
                    }
                    return u.contains("school.moodledemo.net")
                            && u.contains("/mod/forum/view.php");
                });

        if (driver.getCurrentUrl().contains("/login/")) {
            throw new IllegalStateException(
                    "Session expired: " + driver.getCurrentUrl());
        }

        return new ForumPage(driver, wait);
    }

    // -------------------------------------------------------------------------
    // TC11 – Create a forum post
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("TC11 - Create a forum post")
    void tc11_createForumPost() {
        ForumPage forum = openTargetForum();

        String title   = "Automation post - " + System.currentTimeMillis();
        String message = "Automated forum post created by TC11.";

        forum.clickAddDiscussion();
        forum.createDiscussion(title, message);

        assertTrue(
                forum.isDiscussionDisplayed(title),
                "TC11 FAIL – Newly created discussion title should be visible.");
    }

    // -------------------------------------------------------------------------
    // TC10 – Reply to a forum post
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("TC10 - Reply to a forum post")
    void tc10_replyToForumPost() {
        ForumPage forum = openTargetForum();

        forum.openDiscussion(EXISTING_DISCUSSION_TITLE);

        String reply = "Automated TC10 reply - " + System.currentTimeMillis();

        forum.clickReplyOnOriginalPost();
        forum.enterReply(reply);
        forum.submitReply(reply);

        assertTrue(
                forum.isReplyDisplayed(reply),
                "TC10 FAIL – Submitted reply should be visible.");
    }

    // -------------------------------------------------------------------------
    // TC12 – Edit own forum post
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("TC12 - Edit own forum post")
    void tc12_editOwnForumPost() {
        ForumPage forum = openTargetForum();

        String title           = "Automation edit - " + System.currentTimeMillis();
        String originalMessage = "Original body - " + System.currentTimeMillis();

        forum.clickAddDiscussion();
        forum.createDiscussion(title, originalMessage);

        String updatedMessage = "Updated body - " + System.currentTimeMillis();
        forum.editDiscussion(title, updatedMessage);

        assertTrue(
                forum.isPostContentDisplayed(),
                "TC12 FAIL – Post should still be visible after editing.");
        assertTrue(
                forum.isTextDisplayed(updatedMessage),
                "TC12 FAIL – Updated message body should be visible.");
    }

    // -------------------------------------------------------------------------
    // TC13 – Delete own forum post
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("TC13 - Delete own forum post")
    void tc13_deleteOwnForumPost() {
        ForumPage forum = openTargetForum();

        String title   = "Post to delete - " + System.currentTimeMillis();
        String message = "This post will be deleted by TC13 automation.";

        forum.clickAddDiscussion();
        forum.createDiscussion(title, message);

        forum.deleteOwnForumPost(title);

        assertTrue(
                forum.isDiscussionAbsent(title),
                "TC13 FAIL – Deleted discussion should no longer be displayed.");
    }
}
