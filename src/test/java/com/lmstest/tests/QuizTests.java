package com.lmstest.tests;

import com.lmstest.base.BaseTest;
import com.lmstest.pages.CoursePage;
import com.lmstest.pages.LoginPage;
import com.lmstest.pages.MyCoursesPage;
import com.lmstest.pages.QuizPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M3 – Quiz (TC07 – TC08)
 *
 * Full navigation flow per test:
 *   1.  Login                             (LoginPage)
 *   2.  Open My Courses                   (MyCoursesPage)
 *   3.  Click first enrolled course       → /course/view.php?id=XX
 *   4.  Go to Course Overview             → /course/overview.php?id=XX
 *   5.  Click "Quizzes" section toggle    → #quiz_overview_collapsible a
 *   6.  Wait for #quiz_overview to show   → panel expanded + data-loaded
 *   7.  Collect quiz links                → a.activityname hrefs
 *   8.  Open first quiz                   → /mod/quiz/view.php?id=YYY
 *   9a. [TC07] Attempt → answer Q1        → verify answer kept
 *   9b. [TC08] Attempt → answer all → submit → verify result page
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class QuizTests extends BaseTest {

    /**
     * Steps 1-3 shared by every test:
     *   Login → My Courses → open first course (/course/view.php?id=XX)
     *
     * QuizPage.navigateToFirstQuizViaOverview() handles steps 4-8.
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

        // Step 3 – Open first enrolled course → /course/view.php?id=XX
        myCourses.openCourse(0);
        System.out.println("QuizTests @BeforeEach – first course opened");

        // Confirm course page is fully loaded before QuizPage reads the URL
        new CoursePage(driver, wait).waitUntilLoaded();

        System.out.println("QuizTests @BeforeEach – course page ready: "
                + driver.getCurrentUrl());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC07 – Start a quiz attempt and verify answer selection
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * TC07 - Start a quiz attempt
     *
     * Test Steps:
     *   1. Navigate to the first quiz via the course overview
     *   2. Click "Attempt quiz" to start a new attempt
     *   3. Select the first answer option on the first question
     *   4. Verify the answer remains selected
     *
     * Expected Result:
     *   - Quiz title is displayed on the intro page
     *   - Attempt starts successfully (URL changes to /mod/quiz/attempt.php)
     *   - Answer selection succeeds and persists
     */
    @Test
    @DisplayName("TC07 - Start a quiz attempt")
    void tc07_startQuizAttempt() {
        System.out.println("\n========== TC07 – START QUIZ ATTEMPT ==========");

        QuizPage quiz = new QuizPage(driver, wait);

        // Steps 4-8: overview → toggle → links → first quiz intro page
        String quizTitle = quiz.navigateToFirstQuizViaOverview();
        assertFalse(quizTitle.isEmpty(),
                "Quiz title should not be empty on the intro page");
        System.out.println("TC07 – quiz found: " + quizTitle);

        // Click "Attempt quiz"
        quiz.clickAttemptQuiz();
        assertTrue(quiz.isOnAttemptPage(),
                "After 'Attempt quiz' the URL should contain /mod/quiz/attempt.php"
                        + " but was: " + driver.getCurrentUrl());
        System.out.println("TC07 – attempt page opened: " + driver.getCurrentUrl());

        // Select the first answer on Q1
        boolean answered = quiz.selectFirstAnswer();

        if (answered) {
            assertTrue(quiz.isAnswerSelected(),
                    "The chosen answer should remain selected after clicking it");
            System.out.println("TC07 – first answer selected and confirmed ✓");
        } else {
            System.out.println("TC07 – no radio/checkbox on Q1 (e.g. essay); "
                    + "selection check skipped");
        }

        System.out.println("TC07 – TEST PASSED ✓\n");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC08 – Submit quiz and see the result
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * TC08 - Submit quiz and see the result
     *
     * Test Steps:
     *   1. Navigate to the first quiz via the course overview
     *   2. Click "Attempt quiz" to start a new attempt
     *   3. Answer all questions (select first option on each page)
     *   4. Navigate to the summary page by clicking "Finish attempt"
     *   5. Submit the quiz and handle the confirmation dialog
     *   6. Verify the result/review page is displayed
     *
     * Expected Result:
     *   - Quiz is successfully completed through all pages
     *   - Summary page shows quiz submission options
     *   - Result page is displayed after submission
     *   - Result page heading is non-empty
     *   - Result summary may contain grade/score information
     */
    @Test
    @DisplayName("TC08 - Submit quiz and see the result")
    void tc08_submitQuizAndSeeResult() {
        System.out.println("\n========== TC08 – SUBMIT QUIZ AND SEE RESULT ==========");

        QuizPage quiz = new QuizPage(driver, wait);

        // Steps 4-8: overview → toggle → links → first quiz intro page
        String quizTitle = quiz.navigateToFirstQuizViaOverview();
        System.out.println("TC08 – quiz found: " + quizTitle);

        // Start the attempt
        quiz.clickAttemptQuiz();
        assertTrue(quiz.isOnAttemptPage(),
                "Should be on the attempt page after clicking 'Attempt quiz'");
        System.out.println("TC08 – attempt page opened");

        // Answer every question page
        quiz.answerAllRemainingQuestions();
        System.out.println("TC08 – all questions answered");

        // Finish attempt → summary page
        quiz.clickFinishAttempt();
        System.out.println("TC08 – finished attempt, now on summary page");

        // Submit all + confirm dialog → result page
        quiz.submitAllAndFinish();
        System.out.println("TC08 – submission completed");

        // Assert result/review page is shown
        assertTrue(quiz.isOnResultPage(),
                "After submission the URL should contain /mod/quiz/review.php or "
                        + "/mod/quiz/view.php but was: " + driver.getCurrentUrl());
        System.out.println("TC08 – result page confirmed: " + driver.getCurrentUrl());

        // Get and verify result page heading
        String heading = quiz.getResultPageHeading();
        assertFalse(heading.isEmpty(),
                "The result page should display a non-empty heading");
        System.out.println("TC08 – result page heading: " + heading);

        // Get result summary rows (may be empty if no visible grade table)
        List<String> summaryRows = quiz.getResultSummaryRows();
        if (!summaryRows.isEmpty()) {
            System.out.println("TC08 – result summary rows found:");
            for (String row : summaryRows) {
                System.out.println("       • " + row);
            }
        } else {
            System.out.println("TC08 – no grade rows visible; "
                    + "result page presence check is sufficient ✓");
        }

        System.out.println("TC08 – TEST PASSED ✓\n");
    }
}
