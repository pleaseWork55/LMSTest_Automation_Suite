package com.lmstest.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for Moodle quiz interactions.
 *
 * Full navigation flow:
 *   1. Login                                     (LoginPage)
 *   2. My Courses                                (MyCoursesPage)
 *   3. Open a Course  → /course/view.php?id=XX   (MyCoursesPage.openCourse)
 *   4. Course Overview → /course/overview.php?id=XX
 *   5. Click the "Quizzes" collapsible toggle    → #quiz_overview_collapsible
 *   6. Wait for #quiz_overview panel to expand
 *   7. Collect quiz links (a.activityname)
 *   8. Navigate to first quiz /mod/quiz/view.php?id=YYY
 *   9. Attempt → Answer → Finish → Submit → Result
 */
public class QuizPage {

    private final WebDriver     driver;
    private final WebDriverWait wait;

    private static final String BASE_URL = "https://school.moodledemo.net";

    // ═════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═════════════════════════════════════════════════════════════════════════

    // ── Course Overview page (/course/overview.php?id=XX) ────────────────────

    /**
     * The "Quizzes" section toggle button.
     *
     * Stable anchors from the real HTML:
     *   - Outer div id="quiz_overview_collapsible"  (never changes)
     *   - The <a> inside it has  href="#quiz_overview"  and
     *     data-bs-toggle="collapse"
     *
     * Primary:  the <a> anchor inside the stable collapsible container
     * Fallback: any collapse-toggle whose href points to #quiz_overview
     */
    private final By quizSectionToggle = By.cssSelector(
            "#quiz_overview_collapsible a[href='#quiz_overview']"
    );

    /**
     * The collapsible content panel that holds the quiz table.
     * id="quiz_overview" — stable, never changes.
     * We wait for it to have the Bootstrap "show" class after the click.
     */
    private final By quizOverviewPanel = By.cssSelector(
            "#quiz_overview"
    );

    /**
     * The data-loading container inside the panel.
     * We wait for data-loaded="true" before reading links.
     */
    private final By quizPanelLoaded = By.cssSelector(
            "#quiz_overview [data-region='loading-icon-container'][data-loaded='true']"
    );

    /**
     * Quiz activity links inside the expanded panel.
     * Each quiz row has:
     *   <a href="…/mod/quiz/view.php?id=NNN" class="activityname">…</a>
     * inside  td[data-mdl-overview-item="name"]
     */
    private final By quizActivityLinks = By.cssSelector(
            "#quiz_overview td[data-mdl-overview-item='name'] a.activityname"
    );

    // ── Quiz intro page (/mod/quiz/view.php) ─────────────────────────────────

    private final By quizTitle      = By.cssSelector(
            "h1, .page-header-headings h1"
    );

    private final By attemptQuizBtn = By.cssSelector(
            "button[data-action='attempt-quiz-button'], " +
                    ".quizstartbuttondiv button, "                +
                    "input[value='Attempt quiz'], "               +
                    "input[value='Re-attempt quiz'], "            +
                    "[data-action='attempt-quiz-button']"
    );

    // ── Attempt page (/mod/quiz/attempt.php) ─────────────────────────────────

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

    // ── Summary page (/mod/quiz/summary.php) ─────────────────────────────────

    /**
     * Submit button on the quiz summary page.
     * Handles multiple button formats:
     *   - input[value='Submit all and finish']
     *   - button[data-action='submit-quiz']
     *   - button.btn-primary[type='submit'] (Moodle 4.x format)
     */
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

    // ── Result / Review page (/mod/quiz/review.php) ───────────────────────────

    private final By gradeSummary = By.cssSelector(
            ".quizreviewsummary td, "           +
                    "table.generaltable tbody tr td, "  +
                    ".grade"
    );

    private final By reviewHeading = By.cssSelector(
            "h1, .page-header-headings h1"
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
     * Step 4 – Extract the course ID from the current /course/view.php?id=XX
     * URL and navigate to /course/overview.php?id=XX.
     *
     * Must be called AFTER MyCoursesPage.openCourse() has landed the browser
     * on /course/view.php?id=XX.
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
     * Parses the numeric id parameter from a /course/view.php?id=XX URL.
     */
    private String extractCourseId(String url) {
        if (!url.contains("/course/view.php")) {
            throw new AssertionError(
                    "goToCourseOverview() must be called while on a course page "
                            + "(/course/view.php?id=XX). Current URL: " + url);
        }
        // Everything after '?'  →  "id=23"  or  "id=23&section=1"
        String query = url.substring(url.indexOf('?') + 1);
        for (String param : query.split("&")) {
            if (param.startsWith("id=")) {
                return param.substring(3);   // "23"
            }
        }
        throw new AssertionError(
                "Could not extract course id from URL: " + url);
    }

    /**
     * Step 5 – Click the "Quizzes" collapsible section toggle on the overview
     * page so that the quiz table becomes visible.
     *
     * The toggle is the <a> inside #quiz_overview_collapsible that has
     * href="#quiz_overview" and data-bs-toggle="collapse".
     *
     * Only clicks if the panel is not already expanded (aria-expanded="false").
     * Uses JavaScript click as fallback if regular click is intercepted.
     */
    public void openQuizSectionDropdown() {
        // Wait for the toggle to be present on the page
        wait.until(ExpectedConditions.presenceOfElementLocated(quizSectionToggle));

        WebElement toggle = driver.findElement(quizSectionToggle);

        // Aggressive scroll to center the element
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                        "window.scrollBy(0, -150);",  // Extra scroll up to avoid bottom overlap
                toggle);

        // Give the page time to settle after scrolling
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        // Check if already expanded
        String expanded = toggle.getAttribute("aria-expanded");
        System.out.println("QuizPage – quiz section aria-expanded = " + expanded);

        if ("false".equals(expanded)) {
            try {
                // First attempt: regular Selenium click
                toggle.click();
                System.out.println("QuizPage – toggle clicked (regular click)");
            } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                // Fallback: JavaScript click if regular click is intercepted
                System.out.println("QuizPage – regular click intercepted, using JavaScript click");
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();",
                        toggle);
            }
        } else {
            System.out.println("QuizPage – quiz section already expanded, skipping click");
        }

        // Wait a moment for Bootstrap collapse animation
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Wait for the Bootstrap collapse panel to receive the "show" class
        wait.until(ExpectedConditions.attributeContains(
                quizOverviewPanel, "class", "show"));
        System.out.println("QuizPage – quiz panel is now expanded (#quiz_overview has 'show')");

        // Wait for the AJAX data-loaded="true" flag so links are in the DOM
        wait.until(ExpectedConditions.presenceOfElementLocated(quizPanelLoaded));
        System.out.println("QuizPage – quiz panel data fully loaded");
    }

    /**
     * Step 6 – After the panel is expanded, collect all quiz hrefs from the
     * activity-name links inside the table.
     *
     * Returns URLs such as:
     *   https://school.moodledemo.net/mod/quiz/view.php?id=723
     *   https://school.moodledemo.net/mod/quiz/view.php?id=757
     */
    public List<String> getQuizUrlsFromOverview() {
        // Links must already be visible (openQuizSectionDropdown called first)
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
     * Master convenience method used by both TC07 and TC08.
     *
     * Precondition : browser is on /course/view.php?id=XX  (@BeforeEach)
     *
     * Full steps:
     *   4. goToCourseOverview()         → /course/overview.php?id=XX
     *   5. openQuizSectionDropdown()    → expands #quiz_overview panel
     *   6. getQuizUrlsFromOverview()    → collects a.activityname hrefs
     *   7. driver.get(firstQuizUrl)     → /mod/quiz/view.php?id=YYY
     *   8. waitUntilIntroLoaded()
     *
     * Returns the quiz title shown on the intro page.
     */
    public String navigateToFirstQuizViaOverview() {
        // Step 4
        goToCourseOverview();

        // Step 5
        openQuizSectionDropdown();

        // Step 6
        List<String> quizUrls = getQuizUrlsFromOverview();
        if (quizUrls.isEmpty()) {
            throw new AssertionError(
                    "No quiz URLs found inside #quiz_overview after expanding "
                            + "the collapsible section. "
                            + "Current URL: " + driver.getCurrentUrl());
        }

        // Step 7 – open the first quiz
        String firstQuizUrl = quizUrls.get(0);
        System.out.println("QuizPage – opening: " + firstQuizUrl);
        driver.get(firstQuizUrl);

        // Step 8
        waitUntilIntroLoaded();
        return getQuizTitle();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // QUIZ INTRO PAGE
    // ═════════════════════════════════════════════════════════════════════════

    public void waitUntilIntroLoaded() {
        wait.until(ExpectedConditions.urlContains("/mod/quiz/view.php"));
        wait.until(ExpectedConditions.presenceOfElementLocated(quizTitle));
    }

    public String getQuizTitle() {
        return driver.findElement(quizTitle).getText().trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC07  –  START ATTEMPT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Clicks the "Attempt quiz" button and handles any confirmation modal.
     * Waits for the attempt page to load.
     */
    public void clickAttemptQuiz() {
        wait.until(ExpectedConditions.elementToBeClickable(attemptQuizBtn));
        driver.findElement(attemptQuizBtn).click();
        System.out.println("QuizPage – 'Attempt quiz' button clicked");

        handleAttemptConfirmationIfPresent();
        waitUntilAttemptLoaded();
    }

    /**
     * Handles the optional confirmation modal that may appear after clicking
     * "Attempt quiz". If no modal is present within 4 seconds, continues.
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
     * Waits until the browser navigates to the attempt page (/mod/quiz/attempt.php).
     */
    public void waitUntilAttemptLoaded() {
        wait.until(ExpectedConditions.urlContains("/mod/quiz/attempt.php"));
        System.out.println("QuizPage – attempt page loaded");
    }

    /**
     * Checks if the current page is the quiz attempt page.
     */
    public boolean isOnAttemptPage() {
        return driver.getCurrentUrl().contains("/mod/quiz/attempt.php");
    }

    /**
     * Selects the first radio or checkbox answer on the current attempt page.
     * Returns true if a selectable option was found and clicked.
     *
     * Handles visibility and interactability issues by using JavaScript click
     * and ensuring the element is scrolled into full view.
     */
    public boolean selectFirstAnswer() {
        // Wait for the attempt page to fully load (not just URL)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.que")));  // Wait for ANY question div to appear

        System.out.println("QuizPage – question container found, waiting for form inputs");

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Try to find ANY radio button on the page (don't require form#responseform)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='radio']")));

        System.out.println("QuizPage – radio inputs are now available");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // ── Try radio buttons first ──────────────────────────────────────────
        List<WebElement> radios = driver.findElements(radioOptions);
        System.out.println("QuizPage – found " + radios.size() + " radio button(s)");

        if (!radios.isEmpty()) {
            WebElement firstRadio = radios.get(0);

            // Scroll into view
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                            "window.scrollBy(0, -100);",
                    firstRadio);

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            // Try JavaScript click first
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

            // Fallback: Selenium regular click
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

        // ── Fall back to checkboxes ──────────────────────────────────────────
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
     * Checks if any radio or checkbox answer is currently selected on the page.
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
     * Answers all remaining pages of the quiz by selecting the first option on
     * each page, then clicking "Next page" until the finish button appears.
     */
    public void answerAllRemainingQuestions() {
        int safetyCounter = 0;

        while (isOnAttemptPage() && safetyCounter < 30) {
            safetyCounter++;
            System.out.println("QuizPage – answering page " + safetyCounter);

            // Answer current page's question(s)
            selectFirstAnswer();

            // Look for "Next page" button
            List<WebElement> nextBtns = driver.findElements(nextPageBtn);

            if (!nextBtns.isEmpty()) {
                WebElement nextBtn = nextBtns.get(0);

                // Check if button is actually visible/displayed
                if (nextBtn.isDisplayed()) {
                    System.out.println("QuizPage – 'Next page' button found, clicking it");

                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}

                    // Scroll the button into view
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                                    "window.scrollBy(0, -150);",
                            nextBtn);

                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}

                    // Try JavaScript click first (more reliable)
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

                    // Wait for page to load
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
     * Clicks "Finish attempt …" to navigate from the last question page to the
     * attempt summary page. Also handles the case where we're already on summary.
     */
    public void clickFinishAttempt() {
        String currentUrl = driver.getCurrentUrl();
        System.out.println("QuizPage – clickFinishAttempt() called, current URL: " + currentUrl);

        // If we're already on summary page, skip this step
        if (currentUrl.contains("/mod/quiz/summary.php")) {
            System.out.println("QuizPage – already on summary page, skipping clickFinishAttempt");
            return;
        }

        // If we're on attempt page, look for the Finish button
        if (currentUrl.contains("/mod/quiz/attempt.php")) {
            System.out.println("QuizPage – on attempt page, looking for 'Finish attempt' button");

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(finishAttemptBtn));
                System.out.println("QuizPage – 'Finish attempt' button found");

                WebElement finishBtn = driver.findElement(finishAttemptBtn);

                // Scroll into view
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                                "window.scrollBy(0, -150);",
                        finishBtn);

                try { Thread.sleep(500); } catch (InterruptedException ignored) {}

                // JavaScript click
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

        // Wait for navigation to summary or review page
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/quiz/summary.php"),
                ExpectedConditions.urlContains("/mod/quiz/review.php")
        ));

        System.out.println("QuizPage – now on: " + driver.getCurrentUrl());
    }

    /**
     * Submits the quiz and handles the confirmation modal.
     * Waits for navigation to the result/review page.
     */
    public void submitAllAndFinish() {
        // First, wait for the page to be on summary page
        wait.until(ExpectedConditions.urlContains("/mod/quiz/summary.php"));
        System.out.println("QuizPage – on summary page, looking for submit button");

        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        try {
            // Wait for submit button to be present (with extended timeout)
            WebElement submitBtn = wait.until(
                    ExpectedConditions.presenceOfElementLocated(submitAllBtn));

            System.out.println("QuizPage – submit button found, preparing to click");

            // Scroll into view before clicking
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});" +
                            "window.scrollBy(0, -150);",
                    submitBtn);

            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

            // Wait for any overlay/modal to disappear
            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector(".modal-backdrop, .fade.in")));
                System.out.println("QuizPage – modal backdrop cleared");
            } catch (TimeoutException e) {
                System.out.println("QuizPage – no modal backdrop detected (OK)");
            }

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            // Use JavaScript click to bypass interception
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

        // Handle confirmation modal if present
        try {
            System.out.println("QuizPage – waiting for confirmation modal...");
            WebElement confirmBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(confirmSubmitBtn));

            System.out.println("QuizPage – confirmation button found, clicking");

            // Scroll into view
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                    confirmBtn);

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            // Use JavaScript click for confirmation too
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

        // Wait for navigation to result/review page
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/quiz/review.php"),
                ExpectedConditions.urlContains("/mod/quiz/view.php")
        ));

        System.out.println("QuizPage – submission complete, now on: " + driver.getCurrentUrl());
    }

    /**
     * Checks if the browser is on the result/review page after quiz submission.
     */
    public boolean isOnResultPage() {
        String url = driver.getCurrentUrl();
        return url.contains("/mod/quiz/review.php")
                || url.contains("/mod/quiz/view.php");
    }

    /**
     * Collects all text from the grade summary table on the result page.
     * Filters out empty rows.
     */
    public List<String> getResultSummaryRows() {
        return driver.findElements(gradeSummary).stream()
                .map(WebElement::getText)
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Gets the main heading text from the result/review page.
     */
    public String getResultPageHeading() {
        wait.until(ExpectedConditions.presenceOfElementLocated(reviewHeading));
        return driver.findElement(reviewHeading).getText().trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Helper method to scroll an element into the center of the viewport.
     */
    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});",
                        element);
    }
}