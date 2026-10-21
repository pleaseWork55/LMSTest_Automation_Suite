package com.lmstest.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for Moodle quiz interactions.
 *
 * Test Cases:
 *   TC07: Start and answer a quiz
 *   TC08: Submit quiz and check result
 *   TC19: Quiz info shown before starting
 *   TC20: Review a finished quiz attempt
 */
public class QuizPage {

    private final WebDriver     driver;
    private final WebDriverWait wait;

    private static final String BASE_URL = "https://school.moodledemo.net";

    // ═════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═════════════════════════════════════════════════════════════════════════

    private final By quizSectionToggle = By.cssSelector(
            "#quiz_overview_collapsible a[href='#quiz_overview']"
    );

    private final By quizOverviewPanel = By.cssSelector(
            "#quiz_overview"
    );

    private final By quizPanelLoaded = By.cssSelector(
            "#quiz_overview [data-region='loading-icon-container'][data-loaded='true']"
    );

    private final By quizActivityLinks = By.cssSelector(
            "#quiz_overview td[data-mdl-overview-item='name'] a.activityname"
    );

    private final By quizTitle      = By.cssSelector(
            "h1, .page-header-headings h1"
    );

    private final By quizDescription = By.cssSelector(
            ".quizinfo, .quiz-description, .generalbox.info"
    );

    private final By attemptQuizBtn = By.cssSelector(
            "button[data-action='attempt-quiz-button'], " +
                    ".quizstartbuttondiv button, "                +
                    "input[value='Attempt quiz'], "               +
                    "input[value='Re-attempt quiz'], "            +
                    "[data-action='attempt-quiz-button']"
    );

    private final By radioOptions    = By.cssSelector(
            "input[type='radio'], .answer input[type='radio']"
    );

    private final By checkboxOptions = By.cssSelector(
            "input[type='checkbox']"
    );

    private final By nextPageBtn     = By.cssSelector(
            "input[name='next'], "       +
                    "input[value='Next page'], " +
                    "button[name='next']"
    );

    private final By finishAttemptBtn = By.cssSelector(
            "input[value='Finish attempt ...'], " +
                    "input[value='Finish attempt'], "     +
                    "a.endtestlink, "                     +
                    ".endtestlink"
    );

    private final By submitAllBtn = By.xpath(
            "//button[@type='submit' and contains(text(), 'Submit all and finish')] | " +
                    "//button[@type='submit' and contains(@class, 'btn-primary')] | " +
                    "//input[@value='Submit all and finish'] | " +
                    "//button[@data-action='submit-quiz'] | " +
                    "//*[@class='submitbtns']//input[@type='submit']"
    );

    private final By confirmSubmitBtn = By.cssSelector(
            ".modal-footer button.btn-primary, "                               +
                    ".confirmation-buttons input[value='Submit all and finish'], "     +
                    "button[data-action='save'], "                                     +
                    "#id_submitbutton"
    );

    private final By gradeSummary = By.cssSelector(
            ".quizreviewsummary td, "           +
                    "table.generaltable tbody tr td, "  +
                    ".grade"
    );

    private final By reviewHeading = By.cssSelector(
            "h1, .page-header-headings h1"
    );

    private final By scoreDisplay = By.cssSelector(
            ".quizreviewsummary, .gradetable, .grade-info"
    );

    private final By questionReview = By.cssSelector(
            ".que, .question, .review-question"
    );

    // ─────────────────────────────────────────────────────────────────────────

    public QuizPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait   = wait;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Navigate to course overview page
     */
    public void goToCourseOverview() {
        String currentUrl = driver.getCurrentUrl();
        System.out.println("QuizPage – current course URL: " + currentUrl);

        String courseId = extractCourseId(currentUrl);
        System.out.println("QuizPage – extracted course ID: " + courseId);

        String overviewUrl = BASE_URL + "/course/overview.php?id=" + courseId;
        System.out.println("QuizPage – navigating to: " + overviewUrl);
        driver.get(overviewUrl);

        wait.until(ExpectedConditions.urlContains("/course/overview.php"));
        System.out.println("QuizPage – overview page loaded: "
                + driver.getCurrentUrl());
    }

    /**
     * Extract course ID from URL
     */
    private String extractCourseId(String url) {
        if (!url.contains("/course/view.php")) {
            throw new AssertionError(
                    "goToCourseOverview() must be called while on a course page "
                            + "(/course/view.php?id=XX). Current URL: " + url);
        }
        String query = url.substring(url.indexOf('?') + 1);
        for (String param : query.split("&")) {
            if (param.startsWith("id=")) {
                return param.substring(3);
            }
        }
        throw new AssertionError(
                "Could not extract course id from URL: " + url);
    }

    /**
     * Open quiz section dropdown
     */
    public void openQuizSectionDropdown() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(quizSectionToggle));
        } catch (TimeoutException e) {
            System.out.println("QuizPage – quiz section toggle not found on this course");
            throw new AssertionError("No quizzes available in this course");
        }

        WebElement toggle = driver.findElement(quizSectionToggle);

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                        "window.scrollBy(0, -150);",
                toggle);

        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        String expanded = toggle.getAttribute("aria-expanded");
        System.out.println("QuizPage – quiz section aria-expanded = " + expanded);

        if ("false".equals(expanded)) {
            try {
                toggle.click();
                System.out.println("QuizPage – toggle clicked (regular click)");
            } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                System.out.println("QuizPage – regular click intercepted, using JavaScript click");
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();",
                        toggle);
            }
        } else {
            System.out.println("QuizPage – quiz section already expanded, skipping click");
        }

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        wait.until(ExpectedConditions.attributeContains(
                quizOverviewPanel, "class", "show"));
        System.out.println("QuizPage – quiz panel is now expanded (#quiz_overview has 'show')");

        wait.until(ExpectedConditions.presenceOfElementLocated(quizPanelLoaded));
        System.out.println("QuizPage – quiz panel data fully loaded");
    }

    /**
     * Get quiz URLs from overview
     */
    public List<String> getQuizUrlsFromOverview() {
        wait.until(ExpectedConditions.presenceOfElementLocated(quizActivityLinks));

        List<String> urls = driver.findElements(quizActivityLinks)
                .stream()
                .map(el -> el.getAttribute("href"))
                .filter(href -> href != null
                        && href.contains("/mod/quiz/view.php"))
                .distinct()
                .collect(Collectors.toList());

        System.out.println("QuizPage – quiz URLs collected: " + urls);
        return urls;
    }

    /**
     * Navigate to quiz by index
     */
    public String navigateToQuizByIndex(int quizIndex) {
        goToCourseOverview();
        openQuizSectionDropdown();

        List<String> quizUrls = getQuizUrlsFromOverview();
        if (quizUrls.isEmpty()) {
            throw new AssertionError(
                    "No quiz URLs found. Current course may not have quizzes. "
                            + "Current URL: " + driver.getCurrentUrl());
        }

        if (quizIndex >= quizUrls.size()) {
            throw new AssertionError(
                    "Quiz index " + quizIndex + " is out of bounds. "
                            + "Only " + quizUrls.size() + " quizzes found.");
        }

        String quizUrl = quizUrls.get(quizIndex);
        System.out.println("QuizPage – opening quiz at index " + quizIndex + ": " + quizUrl);
        driver.get(quizUrl);

        waitUntilIntroLoaded();
        return getQuizTitle();
    }

    /**
     * Navigate to first quiz
     */
    public String navigateToFirstQuizViaOverview() {
        return navigateToQuizByIndex(0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // QUIZ INTRO PAGE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Wait for quiz intro page to load
     */
    public void waitUntilIntroLoaded() {
        wait.until(ExpectedConditions.urlContains("/mod/quiz/view.php"));
        wait.until(ExpectedConditions.presenceOfElementLocated(quizTitle));
        System.out.println("QuizPage – quiz intro page loaded");
    }

    /**
     * Get quiz title
     */
    public String getQuizTitle() {
        return driver.findElement(quizTitle).getText().trim();
    }

    /**
     * Get quiz description
     */
    public String getQuizDescription() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(quizDescription));
            return driver.findElement(quizDescription).getText().trim();
        } catch (TimeoutException e) {
            System.out.println("QuizPage – no quiz description found");
            return "";
        }
    }

    /**
     * Check if attempt button is visible
     */
    public boolean isAttemptButtonVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(attemptQuizBtn));
            System.out.println("QuizPage – attempt button is visible");
            return true;
        } catch (TimeoutException e) {
            System.out.println("QuizPage – attempt button NOT visible");
            return false;
        }
    }

    /**
     * Check if re-attempt button is visible (XPath version)
     */
    public boolean isReAttemptButtonVisible() {
        try {
            By reAttemptBtn = By.xpath(
                    "//input[@value='Re-attempt quiz'] | " +
                            "//button[contains(text(), 'Re-attempt')]"
            );
            wait.until(ExpectedConditions.visibilityOfElementLocated(reAttemptBtn));
            System.out.println("QuizPage – re-attempt button is visible");
            return true;
        } catch (TimeoutException e) {
            System.out.println("QuizPage – re-attempt button not visible");
            return false;
        }
    }

    /**
     * Get quiz intro info
     */
    public QuizInfo getQuizIntroInfo() {
        String title = getQuizTitle();
        String description = getQuizDescription();
        boolean attemptVisible = isAttemptButtonVisible();

        return new QuizInfo(title, description, attemptVisible);
    }

    /**
     * Verify quiz info is displayed
     */
    public QuizInfo verifyQuizInfoDisplayed() {
        System.out.println("QuizPage – verifying quiz info is displayed...");

        String title = getQuizTitle();
        System.out.println("QuizPage – quiz title: " + title);

        String description = getQuizDescription();
        System.out.println("QuizPage – quiz description length: " + description.length() + " chars");

        boolean buttonVisible = isAttemptButtonVisible();
        System.out.println("QuizPage – attempt button visible: " + buttonVisible);

        return new QuizInfo(title, description, buttonVisible);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC20  –  REVIEW A FINISHED QUIZ ATTEMPT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Complete and review quiz attempt
     */
    public void completeAndReviewQuizAttempt() {
        System.out.println("QuizPage – completing quiz attempt for review...");

        clickAttemptQuiz();
        answerAllRemainingQuestions();
        clickFinishAttempt();
        submitAllAndFinish();

        System.out.println("QuizPage – quiz attempt completed and submitted");
        System.out.println("QuizPage – currently on review page: " + driver.getCurrentUrl());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC07  –  START ATTEMPT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Click attempt quiz button (handles both Attempt and Re-attempt)
     */
    public void clickAttemptQuiz() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(attemptQuizBtn));
            driver.findElement(attemptQuizBtn).click();
            System.out.println("QuizPage – 'Attempt quiz' button clicked");
        } catch (TimeoutException e) {
            System.out.println("QuizPage – Attempt button not clickable, trying re-attempt...");
            By reAttemptBtn = By.xpath("//input[@value='Re-attempt quiz']");
            try {
                wait.until(ExpectedConditions.elementToBeClickable(reAttemptBtn));
                driver.findElement(reAttemptBtn).click();
                System.out.println("QuizPage – 'Re-attempt quiz' button clicked");
            } catch (TimeoutException e2) {
                throw new AssertionError("No attempt or re-attempt button found");
            }
        }

        handleAttemptConfirmationIfPresent();
        waitUntilAttemptLoaded();
    }

    /**
     * Handle attempt confirmation modal if present
     */
    private void handleAttemptConfirmationIfPresent() {
        try {
            By startConfirm = By.cssSelector(
                    ".modal-footer button.btn-primary, " +
                            "input[value='Start attempt'], "     +
                            "button[data-action='save']"
            );
            new WebDriverWait(driver, java.time.Duration.ofSeconds(4))
                    .until(ExpectedConditions.elementToBeClickable(startConfirm))
                    .click();
            System.out.println("QuizPage – start-attempt confirmation modal dismissed");
        } catch (Exception ignored) {
            System.out.println("QuizPage – no start-attempt modal (skipped)");
        }
    }

    /**
     * Wait for attempt page to load
     */
    public void waitUntilAttemptLoaded() {
        wait.until(ExpectedConditions.urlContains("/mod/quiz/attempt.php"));
        System.out.println("QuizPage – attempt page loaded");
    }

    /**
     * Check if on attempt page
     */
    public boolean isOnAttemptPage() {
        return driver.getCurrentUrl().contains("/mod/quiz/attempt.php");
    }

    /**
     * Select first answer on attempt page
     */
    public boolean selectFirstAnswer() {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.que")));

        System.out.println("QuizPage – question container found, waiting for form inputs");

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='radio']")));

        System.out.println("QuizPage – radio inputs are now available");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        List<WebElement> radios = driver.findElements(radioOptions);
        System.out.println("QuizPage – found " + radios.size() + " radio button(s)");

        if (!radios.isEmpty()) {
            WebElement firstRadio = radios.get(0);

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                            "window.scrollBy(0, -100);",
                    firstRadio);

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            try {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();",
                        firstRadio);
                System.out.println("QuizPage – radio answer selected (JS click)");
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                return firstRadio.isSelected();
            } catch (Exception e1) {
                System.out.println("QuizPage – JS click failed: " + e1.getMessage());
            }

            try {
                wait.until(ExpectedConditions.elementToBeClickable(firstRadio));
                firstRadio.click();
                System.out.println("QuizPage – radio answer selected (regular click)");
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                return firstRadio.isSelected();
            } catch (Exception e2) {
                System.out.println("QuizPage – regular click failed: " + e2.getMessage());
                return false;
            }
        }

        List<WebElement> checks = driver.findElements(checkboxOptions);
        System.out.println("QuizPage – found " + checks.size() + " checkbox(es)");

        if (!checks.isEmpty()) {
            WebElement firstCheckbox = checks.get(0);

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                            "window.scrollBy(0, -100);",
                    firstCheckbox);

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            try {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();",
                        firstCheckbox);
                System.out.println("QuizPage – checkbox answer selected (JS click)");
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                return firstCheckbox.isSelected();
            } catch (Exception e1) {
                System.out.println("QuizPage – JS click on checkbox failed: " + e1.getMessage());
            }

            try {
                wait.until(ExpectedConditions.elementToBeClickable(firstCheckbox));
                firstCheckbox.click();
                System.out.println("QuizPage – checkbox answer selected (regular click)");
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                return firstCheckbox.isSelected();
            } catch (Exception e2) {
                System.out.println("QuizPage – regular click on checkbox failed: " + e2.getMessage());
                return false;
            }
        }

        System.out.println("QuizPage – NO radio/checkbox options found on this question");
        return false;
    }

    /**
     * Check if answer is selected
     */
    public boolean isAnswerSelected() {
        for (WebElement r : driver.findElements(radioOptions))
            if (r.isSelected()) return true;
        for (WebElement c : driver.findElements(checkboxOptions))
            if (c.isSelected()) return true;
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC08  –  SUBMIT & RESULT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Answer all remaining questions
     */
    public void answerAllRemainingQuestions() {
        int safetyCounter = 0;

        while (isOnAttemptPage() && safetyCounter < 30) {
            safetyCounter++;
            System.out.println("QuizPage – answering page " + safetyCounter);

            selectFirstAnswer();

            List<WebElement> nextBtns = driver.findElements(nextPageBtn);

            if (!nextBtns.isEmpty()) {
                WebElement nextBtn = nextBtns.get(0);

                if (nextBtn.isDisplayed()) {
                    System.out.println("QuizPage – 'Next page' button found, clicking it");

                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}

                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                                    "window.scrollBy(0, -150);",
                            nextBtn);

                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}

                    try {
                        ((JavascriptExecutor) driver).executeScript(
                                "arguments[0].click();",
                                nextBtn);
                        System.out.println("QuizPage – 'Next page' clicked (JS)");
                    } catch (Exception e1) {
                        System.out.println("QuizPage – JS click failed, trying regular click");
                        try {
                            nextBtn.click();
                            System.out.println("QuizPage – 'Next page' clicked (regular)");
                        } catch (Exception e2) {
                            System.out.println("QuizPage – both click methods failed: " + e2.getMessage());
                            break;
                        }
                    }

                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                } else {
                    System.out.println("QuizPage – 'Next page' button exists but not displayed");
                    break;
                }
            } else {
                System.out.println("QuizPage – no 'Next page' button found; on last question page or summary");
                break;
            }
        }

        System.out.println("QuizPage – finished answering all pages (looped " + safetyCounter + " times)");
        System.out.println("QuizPage – current URL: " + driver.getCurrentUrl());
    }

    /**
     * Click finish attempt
     */
    public void clickFinishAttempt() {
        String currentUrl = driver.getCurrentUrl();
        System.out.println("QuizPage – clickFinishAttempt() called, current URL: " + currentUrl);

        if (currentUrl.contains("/mod/quiz/summary.php")) {
            System.out.println("QuizPage – already on summary page, skipping clickFinishAttempt");
            return;
        }

        if (currentUrl.contains("/mod/quiz/attempt.php")) {
            System.out.println("QuizPage – on attempt page, looking for 'Finish attempt' button");

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(finishAttemptBtn));
                System.out.println("QuizPage – 'Finish attempt' button found");

                WebElement finishBtn = driver.findElement(finishAttemptBtn);

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                                "window.scrollBy(0, -150);",
                        finishBtn);

                try { Thread.sleep(500); } catch (InterruptedException ignored) {}

                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].click();",
                            finishBtn);
                    System.out.println("QuizPage – 'Finish attempt' clicked (JS)");
                } catch (Exception e1) {
                    finishBtn.click();
                    System.out.println("QuizPage – 'Finish attempt' clicked (regular)");
                }

                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            } catch (TimeoutException e) {
                System.out.println("QuizPage – 'Finish attempt' button not found after 20 seconds");
                System.out.println("QuizPage – current URL: " + driver.getCurrentUrl());
                throw e;
            }
        }

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/quiz/summary.php"),
                ExpectedConditions.urlContains("/mod/quiz/review.php")
        ));

        System.out.println("QuizPage – now on: " + driver.getCurrentUrl());
    }

    /**
     * Submit all and finish
     */
    public void submitAllAndFinish() {
        wait.until(ExpectedConditions.urlContains("/mod/quiz/summary.php"));
        System.out.println("QuizPage – on summary page, looking for submit button");

        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        try {
            WebElement submitBtn = wait.until(
                    ExpectedConditions.presenceOfElementLocated(submitAllBtn));

            System.out.println("QuizPage – submit button found, preparing to click");

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                            "window.scrollBy(0, -150);",
                    submitBtn);

            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector(".modal-backdrop, .fade.in")));
                System.out.println("QuizPage – modal backdrop cleared");
            } catch (TimeoutException e) {
                System.out.println("QuizPage – no modal backdrop detected (OK)");
            }

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            try {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();",
                        submitBtn);
                System.out.println("QuizPage – 'Submit all and finish' clicked (JS)");
            } catch (Exception e1) {
                System.out.println("QuizPage – JS click failed, trying regular click");
                wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
                submitBtn.click();
                System.out.println("QuizPage – 'Submit all and finish' clicked (regular)");
            }

        } catch (TimeoutException e) {
            System.out.println("QuizPage – submit button not found after timeout");
            throw e;
        }

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        try {
            System.out.println("QuizPage – waiting for confirmation modal...");
            WebElement confirmBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(confirmSubmitBtn));

            System.out.println("QuizPage – confirmation button found, clicking");

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                    confirmBtn);

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            try {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();",
                        confirmBtn);
                System.out.println("QuizPage – confirmation clicked (JS)");
            } catch (Exception e) {
                confirmBtn.click();
                System.out.println("QuizPage – confirmation clicked (regular)");
            }

            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        } catch (TimeoutException e) {
            System.out.println("QuizPage – no confirmation modal found (skipped)");
        }

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/quiz/review.php"),
                ExpectedConditions.urlContains("/mod/quiz/view.php")
        ));

        System.out.println("QuizPage – submission complete, now on: " + driver.getCurrentUrl());
    }

    /**
     * Check if on result page
     */
    public boolean isOnResultPage() {
        String url = driver.getCurrentUrl();
        return url.contains("/mod/quiz/review.php")
                || url.contains("/mod/quiz/view.php");
    }

    /**
     * Check if on review page
     */
    public boolean isOnReviewPage() {
        return driver.getCurrentUrl().contains("/mod/quiz/review.php");
    }

    /**
     * Get result summary rows
     */
    public List<String> getResultSummaryRows() {
        return driver.findElements(gradeSummary).stream()
                .map(WebElement::getText)
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Get result page heading
     */
    public String getResultPageHeading() {
        wait.until(ExpectedConditions.presenceOfElementLocated(reviewHeading));
        return driver.findElement(reviewHeading).getText().trim();
    }

    /**
     * Get quiz score
     */
    public String getQuizScore() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(scoreDisplay));
            String scoreText = driver.findElement(scoreDisplay).getText();
            System.out.println("QuizPage – score/grade extracted");
            return scoreText;
        } catch (TimeoutException e) {
            System.out.println("QuizPage – score display not found");
            return "Score not displayed";
        }
    }

    /**
     * Get quiz result info
     */
    public QuizResult getQuizResultInfo() {
        String heading = getResultPageHeading();
        String score = getQuizScore();
        List<String> summaryRows = getResultSummaryRows();

        return new QuizResult(heading, score, summaryRows);
    }

    /**
     * Get question count
     */
    public int getQuestionCount() {
        List<WebElement> questions = driver.findElements(questionReview);
        return questions.size();
    }

    /**
     * Get review page info
     */
    public ReviewPageInfo getReviewPageInfo() {
        String heading = getResultPageHeading();
        String score = getQuizScore();
        int questionCount = getQuestionCount();

        return new ReviewPageInfo(heading, score, questionCount);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Data class for quiz intro information
     */
    public static class QuizInfo {
        public final String title;
        public final String description;
        public final boolean attemptVisible;

        public QuizInfo(String title, String description, boolean attemptVisible) {
            this.title = title;
            this.description = description;
            this.attemptVisible = attemptVisible;
        }

        @Override
        public String toString() {
            return "QuizInfo{" +
                    "title='" + title + '\'' +
                    ", description length=" + description.length() +
                    ", attemptVisible=" + attemptVisible +
                    '}';
        }
    }

    /**
     * Data class for quiz result information
     */
    public static class QuizResult {
        public final String heading;
        public final String score;
        public final List<String> summaryRows;

        public QuizResult(String heading, String score, List<String> summaryRows) {
            this.heading = heading;
            this.score = score;
            this.summaryRows = summaryRows;
        }

        @Override
        public String toString() {
            return "QuizResult{" +
                    "heading='" + heading + '\'' +
                    ", score (first 100 chars)='" + (score.length() > 100 ? score.substring(0, 100) + "..." : score) + '\'' +
                    ", summaryRows=" + summaryRows.size() +
                    '}';
        }
    }

    /**
     * Data class for review page information
     */
    public static class ReviewPageInfo {
        public final String heading;
        public final String score;
        public final int questionCount;

        public ReviewPageInfo(String heading, String score, int questionCount) {
            this.heading = heading;
            this.score = score;
            this.questionCount = questionCount;
        }

        @Override
        public String toString() {
            return "ReviewPageInfo{" +
                    "heading='" + heading + '\'' +
                    ", score (first 100 chars)='" + (score.length() > 100 ? score.substring(0, 100) + "..." : score) + '\'' +
                    ", questionCount=" + questionCount +
                    '}';
        }
    }

    /**
     * Scroll element into view
     */
    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});",
                        element);
    }
}