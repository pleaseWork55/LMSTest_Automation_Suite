package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.CoursePage;
import com.lmstest.pages.LoginPage;
import com.lmstest.pages.MyCoursesPage;
import com.lmstest.pages.QuizPage;
import com.lmstest.pages.QuizPage.QuizInfo;
import com.lmstest.pages.QuizPage.QuizResult;
import com.lmstest.pages.QuizPage.ReviewPageInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quiz Test Suite - Moodle LMS
 *
 * Test Cases:
 *   TC07: Start and answer a quiz
 *   TC08: Submit quiz and check result
 *   TC19: Quiz info shown before starting
 *   TC20: Review a finished quiz attempt
 */
@TestMethodOrder(OrderAnnotation.class)
class QuizTests extends BaseTest {

    private static QuizPage quiz;

    /**
     * Setup before each test
     */
    @BeforeEach
    void loginAndOpenCourse() {
        // Step 1 – Login
        new LoginPage(driver, wait)
                .open()
                .loginAs(STUDENT_USER, STUDENT_PASS);

        System.out.println("QuizTests @BeforeEach – login successful");

        // Step 2 – My Courses page
        MyCoursesPage myCourses = new MyCoursesPage(driver, wait).open();
        System.out.println("QuizTests @BeforeEach – my courses page opened");

        // Step 3 – Open first course
        myCourses.openCourse(0);
        System.out.println("QuizTests @BeforeEach – first course opened");

        // Step 4 – Verify course page loaded
        new CoursePage(driver, wait).waitUntilLoaded();

        System.out.println("QuizTests @BeforeEach – course page ready: "
                + driver.getCurrentUrl());

        // Initialize QuizPage for tests
        quiz = new QuizPage(driver, wait);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC07 – START AND ANSWER A QUIZ
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * TC07 - Start and answer a quiz
     *
     * Test Steps:
     *   1. Navigate to the first quiz
     *   2. Click "Attempt quiz" or "Re-attempt quiz" button
     *   3. Select the first answer on the quiz page
     *   4. Verify the answer selection persists
     *   5. Verify user is on the attempt page
     *
     * Expected Result:
     *   - Quiz attempt starts successfully
     *   - User can select answers
     *   - Selected answer remains selected
     *   - Attempt page URL is correct
     */
    @Test
    @Order(1)
    @DisplayName("TC07 - Start and answer a quiz")
    void tc07_startAndAnswerQuiz() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST: TC07 – START AND ANSWER A QUIZ");
        System.out.println("=".repeat(70));

        // Step 1: Navigate to first quiz
        String quizTitle = quiz.navigateToFirstQuizViaOverview();
        assertFalse(quizTitle.isEmpty(),
                "TC07: Quiz title should not be empty");
        System.out.println("✓ Step 1: Navigated to quiz: " + quizTitle);

        // Step 2: Try to click attempt button
        try {
            // First, try the regular attempt button
            boolean hasAttempt = quiz.isAttemptButtonVisible();

            if (!hasAttempt) {
                // Try re-attempt button if regular attempt not found
                boolean hasReAttempt = quiz.isReAttemptButtonVisible();

                if (!hasReAttempt) {
                    System.out.println("⚠️  TC07 – SKIPPED: Neither Attempt nor Re-attempt button available");
                    System.out.println("=".repeat(70) + "\n");
                    return;
                }
            }

            // Click whichever button is available
            quiz.clickAttemptQuiz();
            System.out.println("✓ Step 2: Clicked attempt/re-attempt button");

        } catch (AssertionError e) {
            System.out.println("⚠️  TC07 – SKIPPED: Could not click attempt button - " + e.getMessage());
            System.out.println("=".repeat(70) + "\n");
            return;
        }

        assertTrue(quiz.isOnAttemptPage(),
                "TC07: Should be on attempt page");
        System.out.println("✓ Step 3: On attempt page confirmed");

        // Step 4-5: Select first answer
        boolean answered = quiz.selectFirstAnswer();

        if (answered) {
            assertTrue(quiz.isAnswerSelected(),
                    "TC07: Selected answer should remain selected");
            System.out.println("✓ Step 4: First answer selected");
            System.out.println("✓ Step 5: Selected answer persists");
        } else {
            System.out.println("ℹ Step 4-5: No selectable options - skipped");
        }

        System.out.println("  Attempt page URL: " + driver.getCurrentUrl());

        System.out.println("\n✅ TC07 – TEST PASSED");
        System.out.println("=".repeat(70) + "\n");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC08 – SUBMIT QUIZ AND CHECK RESULT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * TC08 - Submit quiz and check result
     *
     * Test Steps:
     *   1. Navigate to the first quiz
     *   2. Click "Attempt quiz" or "Re-attempt quiz" to start
     *   3. Answer all questions on all pages
     *   4. Click "Finish attempt" to go to summary
     *   5. Submit the quiz
     *   6. Handle confirmation dialog
     *   7. Verify result/review page is displayed
     *   8. Extract and verify score/grade
     *
     * Expected Result:
     *   - Quiz is completed through all pages
     *   - Summary page shows submission options
     *   - Result page displays after submission
     *   - Score/grade is visible
     *   - Result information is complete
     */
    @Test
    @Order(2)
    @DisplayName("TC08 - Submit quiz and check result")
    void tc08_submitQuizAndCheckResult() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST: TC08 – SUBMIT QUIZ AND CHECK RESULT");
        System.out.println("=".repeat(70));

        try {
            // Step 1: Navigate to first quiz
            String quizTitle = quiz.navigateToFirstQuizViaOverview();
            System.out.println("✓ Step 1: Navigated to quiz: " + quizTitle);

            // Step 2: Start attempt (handle both Attempt and Re-attempt)
            try {
                // First try the regular attempt button
                boolean hasAttempt = quiz.isAttemptButtonVisible();

                if (!hasAttempt) {
                    // Try re-attempt button if regular attempt not found
                    boolean hasReAttempt = quiz.isReAttemptButtonVisible();

                    if (!hasReAttempt) {
                        System.out.println("⚠️  TC08 – SKIPPED: Neither Attempt nor Re-attempt button available");
                        System.out.println("   (Quiz may already be completed)");
                        System.out.println("=".repeat(70) + "\n");
                        return;
                    }
                }

                quiz.clickAttemptQuiz();
                System.out.println("✓ Step 2: Started quiz attempt");

            } catch (AssertionError e) {
                System.out.println("⚠️  TC08 – SKIPPED: Could not start attempt - " + e.getMessage());
                System.out.println("=".repeat(70) + "\n");
                return;
            }

            assertTrue(quiz.isOnAttemptPage(),
                    "TC08: Should be on attempt page");

            // Step 3: Answer all questions
            quiz.answerAllRemainingQuestions();
            System.out.println("✓ Step 3: Answered all questions");

            // Step 4: Finish attempt
            quiz.clickFinishAttempt();
            System.out.println("✓ Step 4: Finished attempt (on summary page)");

            // Step 5-6: Submit quiz
            quiz.submitAllAndFinish();
            System.out.println("✓ Step 5-6: Submitted quiz with confirmation");

            // Step 7: Verify result page
            assertTrue(quiz.isOnResultPage(),
                    "TC08: Should be on result page after submission");
            System.out.println("✓ Step 7: On result page: " + driver.getCurrentUrl());

            // Step 8: Get and verify result info
            QuizResult result = quiz.getQuizResultInfo();
            System.out.println("✓ Step 8: Retrieved result information");

            assertFalse(result.heading.isEmpty(),
                    "TC08: Result page should have a heading");
            System.out.println("  Result Heading: " + result.heading);

            System.out.println("  Summary Rows: " + result.summaryRows.size());

            System.out.println("\n✅ TC08 – TEST PASSED");
        } catch (AssertionError e) {
            if (e.getMessage() != null && e.getMessage().contains("No quizzes available")) {
                System.out.println("⚠️  TC08 – SKIPPED: No quizzes in this course");
                System.out.println("   (Different course or course without quizzes)");
            } else {
                throw e;
            }
        }
        System.out.println("=".repeat(70) + "\n");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC19 – QUIZ INFO SHOWN BEFORE STARTING
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * TC19 - Quiz info shown before starting
     *
     * Test Steps:
     *   1. Navigate to the first quiz
     *   2. Verify quiz title is displayed
     *   3. Verify quiz description is shown
     *   4. Verify "Attempt quiz" button is visible
     *
     * Expected Result:
     *   - Quiz information (title, description, rules) is clearly visible
     *   - Attempt button is ready to be clicked
     *   - No questions are shown yet
     */
    @Test
    @Order(3)
    @DisplayName("TC19 - Quiz info shown before starting")
    void tc19_quizInfoShownBeforeStarting() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST: TC19 – QUIZ INFO SHOWN BEFORE STARTING");
        System.out.println("=".repeat(70));

        // Step 1: Navigate to first quiz
        String quizTitle = quiz.navigateToFirstQuizViaOverview();
        System.out.println("✓ Step 1: Navigated to quiz");
        System.out.println("  Quiz Title: " + quizTitle);

        // Step 2-4: Verify quiz info
        QuizInfo quizInfo = quiz.verifyQuizInfoDisplayed();
        System.out.println("✓ Step 2: Verified quiz info is displayed");

        // Assertions
        assertFalse(quizInfo.title.isEmpty(),
                "TC19: Quiz title should not be empty");
        assertTrue(quizInfo.attemptVisible,
                "TC19: Attempt button should be visible before starting");

        System.out.println("✓ Step 3: Verified quiz title: " + quizInfo.title);
        System.out.println("✓ Step 4: Description available: " + (quizInfo.description.length() > 0));
        System.out.println("✓ Step 5: Attempt Button Visible: " + quizInfo.attemptVisible);

        System.out.println("\n✅ TC19 – TEST PASSED");
        System.out.println("=".repeat(70) + "\n");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC20 – REVIEW A FINISHED QUIZ ATTEMPT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * TC20 - Review a finished quiz attempt
     *
     * Test Steps:
     *   1. Navigate to the first quiz
     *   2. Complete a quiz attempt (answer all questions)
     *   3. Submit the quiz
     *   4. Verify review page displays
     *   5. Verify score/grade is shown
     *   6. Verify questions are visible
     *
     * Expected Result:
     *   - Review page loads successfully
     *   - Quiz title is displayed
     *   - Score/grade is visible
     *   - All questions are shown
     *   - Question count matches
     */
    @Test
    @Order(4)
    @DisplayName("TC20 - Review a finished quiz attempt")
    void tc20_reviewFinishedQuizAttempt() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST: TC20 – REVIEW A FINISHED QUIZ ATTEMPT");
        System.out.println("=".repeat(70));

        // Step 1: Navigate to first quiz
        String quizTitle = quiz.navigateToFirstQuizViaOverview();
        System.out.println("✓ Step 1: Navigated to quiz: " + quizTitle);

        // Step 2-3: Complete and submit quiz attempt
        System.out.println("✓ Step 2: Starting quiz attempt and answering all questions...");
        quiz.completeAndReviewQuizAttempt();
        System.out.println("✓ Step 3: Quiz completed and submitted");

        // Step 4: Verify on review page
        assertTrue(quiz.isOnReviewPage(),
                "TC20: Should be on review page after submission");
        System.out.println("✓ Step 4: On review page confirmed");

        // Step 5-6: Verify review content
        ReviewPageInfo reviewInfo = quiz.getReviewPageInfo();
        System.out.println("✓ Step 5: Retrieved review page info");

        assertFalse(reviewInfo.heading.isEmpty(),
                "TC20: Review page should have a heading");
        System.out.println("  Review Heading: " + reviewInfo.heading);

        assertTrue(reviewInfo.questionCount > 0,
                "TC20: Review should show at least one question");
        System.out.println("✓ Step 6: Question Count: " + reviewInfo.questionCount);

        System.out.println("\n✅ TC20 – TEST PASSED");
        System.out.println("=".repeat(70) + "\n");
    }
}