package com.lmstest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class ForumPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    /** Base URL used for session recovery. */
    private static final String BASE_URL = "https://school.moodledemo.net";
    private static final String STUDENT_USER = "student";
    private static final String STUDENT_PASS = "moodle26";

    public ForumPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    /**
     * If the current URL is a login page (session expired), re-authenticate
     * and navigate back to {@code returnUrl}.
     */
    private void recoverSessionIfNeeded(String returnUrl) {
        String url = driver.getCurrentUrl();
        if (!url.contains("/login/")) return;

        System.out.println("SESSION EXPIRED — re-logging in. Current URL: " + url);

        // Fill login form
        driver.get(BASE_URL + "/login/index.php");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")))
                .clear();
        driver.findElement(By.id("username")).sendKeys(STUDENT_USER);
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys(STUDENT_PASS);
        driver.findElement(By.id("loginbtn")).click();

        // Wait until we are off the login page
        wait.until(d -> !d.getCurrentUrl().contains("/login/"));

        // Navigate back to where we were
        if (returnUrl != null && !returnUrl.isEmpty()) {
            driver.get(returnUrl);
            // Wait for the page body to be present (not just URL)
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            wait.until(d -> !d.getCurrentUrl().contains("/login/"));
            // Give the page a moment to fully render JavaScript-driven elements
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        }

        System.out.println("SESSION RECOVERED — now at: " + driver.getCurrentUrl());
    }

    // -------------------------------------------------------------------------
    // Navigation helpers
    // -------------------------------------------------------------------------

    /**
     * Open a forum by its visible link text on the course page.
     * Also tries Moodle's data-activityname attribute as a fallback.
     */
    public void openForum(String forumName) {
        // Strip trailing dots for partial matching (e.g. "This week's discussion topic..")
        String forumNameTrimmed = forumName.replaceAll("\\.+$", "");

        // Strategy 1: exact text match via XPath
        By byExact = By.xpath(
                "//a[contains(normalize-space(.), "
                        + xpathLiteral(forumName) + ")]"
        );
        // Strategy 2: partial text match (ignores trailing punctuation differences)
        By byPartial = By.xpath(
                "//a[contains(normalize-space(.), "
                        + xpathLiteral(forumNameTrimmed) + ")]"
        );
        // Strategy 3: Moodle's data-activityname attribute on activity containers
        By byDataAttr = By.cssSelector("[data-activityname]");

        // Wait until any strategy yields at least one element
        wait.until(d -> {
            if (!d.findElements(byExact).isEmpty()) return true;
            if (!d.findElements(byPartial).isEmpty()) return true;
            for (WebElement el : d.findElements(byDataAttr)) {
                String n = el.getAttribute("data-activityname");
                if (n != null && n.contains(forumNameTrimmed)) return true;
            }
            return false;
        });

        // Try strategies in order: exact → partial → data-attr
        for (By locator : new By[]{byExact, byPartial}) {
            for (WebElement link : driver.findElements(locator)) {
                if (link.isDisplayed() && link.isEnabled()) {
                    clickSafely(link);
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.urlContains("/mod/forum/view.php"),
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("a[href*='/mod/forum/discuss.php']")
                            ),
                            ExpectedConditions.presenceOfElementLocated(
                                    By.xpath("//a[contains(normalize-space(.),'Add discussion')]"
                                            + " | //a[contains(normalize-space(.),'new discussion')]"
                                            + " | //button[contains(normalize-space(.),'Add discussion')]")
                            )
                    ));
                    return;
                }
            }
        }

        // Fallback: navigate via data-activityname link
        for (WebElement item : driver.findElements(byDataAttr)) {
            String n = item.getAttribute("data-activityname");
            if (n != null && n.contains(forumNameTrimmed)) {
                List<WebElement> anchors = item.findElements(By.tagName("a"));
                if (!anchors.isEmpty()) {
                    clickSafely(anchors.get(0));
                    wait.until(ExpectedConditions.urlContains("/mod/forum/view.php"));
                    return;
                }
            }
        }

        throw new NoSuchElementException(
                "Could not find a visible forum link: " + forumName
        );
    }

    /**
     * Open a discussion from the forum list page by clicking its link.
     * The {@code title} parameter is used only for logging.
     */
    public void openDiscussion(String title) {
        // Session may have expired during navigation — recover if needed
        recoverSessionIfNeeded(driver.getCurrentUrl());

        // Moodle renders each discussion as a row with an anchor link.
        // Use presence (not clickable) since the link may be partially obscured.
        By discussLink = By.cssSelector(
                "a[href*='/mod/forum/discuss.php']"
        );

        WebElement link = wait.until(
                ExpectedConditions.presenceOfElementLocated(discussLink)
        );

        // JS click bypasses any overlay that blocks a normal click
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", link
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);

        wait.until(ExpectedConditions.urlContains("/mod/forum/discuss.php"));

        System.out.println("Opened discussion page for: " + title);
    }

    // -------------------------------------------------------------------------
    // Visibility checks
    // -------------------------------------------------------------------------

    /** True if the discussion title is visible anywhere on the page. */
    public boolean isDiscussionDisplayed(String title) {
        By locator = By.xpath(
                "//*[self::h1 or self::h2 or self::h3 or self::a"
                        + " or contains(@class,'discussion')"
                        + " or contains(@class,'subject')]"
                        + "[normalize-space(.) = "
                        + xpathLiteral(title) + "]"
        );

        try {
            List<WebElement> elements = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(locator)
            );

            for (WebElement el : elements) {
                if (el.isDisplayed() && el.getText().trim().equals(title)) {
                    return true;
                }
            }
        } catch (TimeoutException e) {
            return false;
        }

        return false;
    }

    /** True if at least one non-empty forum post is visible on the page. */
    public boolean isPostContentDisplayed() {
        // Moodle 4.x uses .forum-post-container; older versions use .forumpost.
        List<WebElement> posts = driver.findElements(
                By.cssSelector(".forumpost, .forum-post-container")
        );

        for (WebElement post : posts) {
            if (post.isDisplayed() && !post.getText().trim().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /** True if the given text is visible somewhere on the current page. */
    public boolean isTextDisplayed(String text) {
        By locator = By.xpath(
                "//*[contains(normalize-space(.), "
                        + xpathLiteral(text) + ")]"
        );

        try {
            return wait.until(
                    ExpectedConditions.visibilityOfElementLocated(locator)
            ).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** True once the discussion title is no longer present in the DOM. */
    public boolean isDiscussionAbsent(String title) {
        By locator = By.xpath(
                "//*[normalize-space(.) = " + xpathLiteral(title) + "]"
        );

        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // TC11 – Create a forum post
    // -------------------------------------------------------------------------

    /** Click the "Add discussion topic" / "Start a new discussion" button on the forum view page. */
    public void clickAddDiscussion() {
        // Session may have expired — recover before trying to click the button
        recoverSessionIfNeeded(driver.getCurrentUrl());
        // Moodle uses several variants of this button text across versions
        By addButton = By.xpath(
                "//a[contains(normalize-space(.), 'Add discussion topic')"
                        + " or contains(normalize-space(.), 'Add discussion')"
                        + " or contains(normalize-space(.), 'Start a new discussion')"
                        + " or contains(normalize-space(.), 'new discussion')]"
                        + " | //button[contains(normalize-space(.), 'Add discussion')"
                        + " or contains(normalize-space(.), 'new discussion')]"
        );

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(addButton)
        );
        clickSafely(button);

        // Confirm that Moodle opened the new-discussion form.
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("id_subject")
        ));
    }

    /**
     * Fill in the subject + message and submit the new discussion form.
     * After this method returns the driver is on the discussion page
     * (or forum view page) showing the newly created post.
     */
    public void createDiscussion(String title, String message) {
        // 1. Subject field
        WebElement subject = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("id_subject"))
        );
        subject.clear();
        subject.sendKeys(title);

        // 2. Message body – try plain textarea first, fall back to TinyMCE
        driver.switchTo().defaultContent();
        enterMessageInEditor(message);

        // 3. Return to default context and submit
        driver.switchTo().defaultContent();

        WebElement submit = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[name='submitbutton'], input[name='submitbutton']")
                )
        );
        clickSafely(submit);

        // 4. Wait for Moodle to finish saving — also watch for session-expiry redirect
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                ExpectedConditions.urlContains("/mod/forum/view.php"),
                ExpectedConditions.urlContains("/login/"),
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".alert, .notification")
                )
        ));

        // If the demo site redirected to login, recover and retry from the forum
        String postSubmitUrl = driver.getCurrentUrl();
        if (postSubmitUrl.contains("/login/")) {
            System.out.println("SESSION EXPIRED after submit — recovering...");
            recoverSessionIfNeeded(BASE_URL + "/mod/forum/view.php?id=944");
            // Re-attempt the full create flow
            clickAddDiscussion();
            createDiscussion(title, message);
            return;
        }

        // 5. Verify the title appears on the resulting page
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(normalize-space(.), " + xpathLiteral(title) + ")]")
                )
        );

        System.out.println("Discussion created successfully: " + title);
    }

    // -------------------------------------------------------------------------
    // TC10 – Reply to a forum post
    // -------------------------------------------------------------------------

    /**
     * Click the Reply link on the first (original) post of a discussion.
     * Works for both Moodle 3.x (collapsible inline form) and 4.x (link opens a new form).
     */
    public void clickReplyOnOriginalPost() {
        driver.switchTo().defaultContent();

        // Find the Reply link — on this Moodle demo it is a plain anchor with href="...post.php?reply=N"
        // It may also have title='Reply', class='reply', or data-action='collapsible-link'
        By replyBy = By.xpath(
                "(//a[contains(@href,'reply=') or contains(@href,'post.php')"
                        + " or @title='Reply' or contains(@class,'reply')])[1]"
        );

        // Log the current URL and all reply-link candidates to help debug
        System.out.println("TC10: current URL before Reply click: " + driver.getCurrentUrl());
        List<WebElement> candidates = driver.findElements(By.xpath(
                "//a[contains(@href,'reply=') or contains(@href,'post.php')"
                        + " or @title='Reply' or contains(@class,'reply')]"));
        System.out.println("TC10: reply candidates found: " + candidates.size());
        for (WebElement c : candidates) {
            System.out.println("  href=" + c.getAttribute("href")
                    + " title=" + c.getAttribute("title")
                    + " class=" + c.getAttribute("class")
                    + " text=[" + c.getText() + "]");
        }

        WebElement reply = wait.until(
                ExpectedConditions.presenceOfElementLocated(replyBy)
        );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", reply
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reply);

        // Moodle navigates to post.php?reply=N or expands an inline form
        wait.until(d -> {
            String url = d.getCurrentUrl();
            if (url.contains("reply=") || url.contains("/post.php")) return true;
            List<WebElement> ta = d.findElements(By.cssSelector(
                    "textarea[id*='message'], textarea[name='message']"));
            for (WebElement e : ta) { if (e.isDisplayed()) return true; }
            List<WebElement> ce = d.findElements(By.cssSelector("[contenteditable='true']"));
            for (WebElement e : ce) { if (e.isDisplayed()) return true; }
            return false;
        });

        driver.switchTo().defaultContent();
    }

    /**
     * Enter text into the reply editor (inline textarea, Atto contenteditable, or TinyMCE iframe).
     */
    public void enterReply(String replyText) {
        driver.switchTo().defaultContent();

        // 1. Plain textarea (Moodle simple / plain-text editor)
        List<WebElement> textareas = driver.findElements(
                By.cssSelector("textarea[id^='id_message'], textarea[name='message']"));
        if (!textareas.isEmpty() && textareas.get(0).isDisplayed()) {
            textareas.get(0).clear();
            textareas.get(0).sendKeys(replyText);
            return;
        }

        // 2. Atto / Bootstrap inline contenteditable editor
        List<WebElement> ces = driver.findElements(By.cssSelector("[contenteditable='true']"));
        if (!ces.isEmpty() && ces.get(0).isDisplayed()) {
            WebElement editor = ces.get(0);
            editor.click();
            editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            editor.sendKeys(replyText);
            return;
        }

        // 3. TinyMCE iframe fallback
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                By.cssSelector("iframe[id$='_ifr']")));
        try {
            WebElement body = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("body#tinymce, body")));
            body.click();
            body.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            body.sendKeys(replyText);
        } finally {
            driver.switchTo().defaultContent();
        }
    }

    /**
     * Submit the reply form and wait until the page shows the posted reply.
     */
    public void submitReply(String replyText) {
        driver.switchTo().defaultContent();

        By postButton = By.xpath(
                "//button[normalize-space()='Post to forum']"
                        + " | //input[@value='Post to forum']"
                        + " | //button[contains(normalize-space(),'Post to forum')]"
                        + " | //input[@name='submitbutton']"
                        + " | //button[@name='submitbutton']"
        );

        clickSafely(wait.until(ExpectedConditions.elementToBeClickable(postButton)));

        // Wait for the page to reload and show the reply
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(normalize-space(.), "
                                + xpathLiteral(replyText) + ")]")
                )
        ));
    }

    /** @deprecated Use {@link #submitReply(String)} to enable proper wait. */
    public void submitReply() {
        submitReply("");
    }

    /** True if the submitted reply text is visible on the discussion page. */
    public boolean isReplyDisplayed(String message) {
        return isTextDisplayed(message);
    }

    // -------------------------------------------------------------------------
    // TC12 – Edit own forum post
    // -------------------------------------------------------------------------

    /**
     * Edit the post whose subject matches {@code title}, replacing its body
     * with {@code updatedMessage}.  The driver must already be on the
     * discussion page showing that post.
     */
    public void editDiscussion(String title, String updatedMessage) {
        // Recover session if it expired after the createDiscussion submit
        recoverSessionIfNeeded(driver.getCurrentUrl());
        // Moodle 4.x: Edit is inside a dropdown action-menu on the post.
        // Try to open the action menu first, then click Edit.
        By actionMenuTrigger = By.cssSelector(
                ".action-menu-trigger, button[data-toggle='dropdown'],"
                        + " a[data-toggle='dropdown']"
        );
        By editLinkBy = By.xpath(
                "//a[normalize-space()='Edit' or contains(@href,'edit=')]"
        );

        // If Edit is not directly visible, open the action-menu first
        if (driver.findElements(editLinkBy).isEmpty()) {
            List<WebElement> triggers = driver.findElements(actionMenuTrigger);
            if (!triggers.isEmpty()) {
                clickSafely(triggers.get(0));
                wait.until(ExpectedConditions.visibilityOfElementLocated(editLinkBy));
            }
        }

        WebElement editLink = wait.until(
                ExpectedConditions.elementToBeClickable(editLinkBy)
        );
        clickSafely(editLink);

        // Wait for the edit form
        WebElement subject = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("id_subject"))
        );

        // Scroll into view, click to focus, then type – avoids ElementNotInteractable
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", subject);
        subject.click();
        subject.clear();
        subject.sendKeys(title);

        // Update the message body – must handle both plain textarea and TinyMCE
        driver.switchTo().defaultContent();
        enterMessageInEditorForEdit(updatedMessage);
        driver.switchTo().defaultContent();

        // Save
        WebElement saveButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[name='submitbutton'], input[name='submitbutton']")
                )
        );
        clickSafely(saveButton);

        // Verify the updated message is displayed
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.tagName("body"), updatedMessage
                )
        );

        System.out.println("Discussion edited successfully: " + title);
    }

    // -------------------------------------------------------------------------
    // TC13 – Delete own forum post
    // -------------------------------------------------------------------------

    /**
     * Delete the post whose subject matches {@code title}.
     * The driver must already be on the discussion page showing that post.
     */
    public void deleteOwnForumPost(String title) {
        // Moodle 4.x places Delete inside a dropdown action menu on the post.
        // First try the direct anchor; if absent open the action menu first.
        By deleteDirectBy = By.xpath(
                "//a[normalize-space()='Delete' or contains(@href,'delete=')]"
        );

        List<WebElement> directLinks = driver.findElements(deleteDirectBy);

        if (directLinks.isEmpty()) {
            // Open the action menu (⋮ or "Edit post" caret)
            By actionMenuBy = By.cssSelector(
                    ".action-menu-trigger, [data-toggle='dropdown']"
            );
            List<WebElement> menus = driver.findElements(actionMenuBy);
            if (!menus.isEmpty()) {
                clickSafely(menus.get(0));
                // Wait for the Delete item to appear
                wait.until(ExpectedConditions.visibilityOfElementLocated(deleteDirectBy));
            }
        }

        WebElement deleteLink = wait.until(
                ExpectedConditions.elementToBeClickable(deleteDirectBy)
        );

        clickSafely(deleteLink);

        // Moodle shows a confirmation page (/post.php?delete=...) before deleting.
        // The confirm button is typically "Yes" — NOT the Cancel/No button.
        // Target the first submit input/button that does NOT have value="No" or "Cancel"
        By confirmYes = By.xpath(
                "(//input[@type='submit' and @value!='No' and @value!='Cancel']"
                        + " | //button[@type='submit' and not(contains(normalize-space(),'No'))"
                        + "      and not(contains(normalize-space(),'Cancel'))])[1]"
        );

        try {
            WebElement confirm = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(confirmYes));
            // Log what we're about to click
            System.out.println("TC13: clicking confirm button value=["
                    + confirm.getAttribute("value") + "] text=[" + confirm.getText() + "]");
            clickSafely(confirm);
        } catch (TimeoutException ignored) {
            // Deleted immediately without a confirmation step
        }

        // After deletion Moodle redirects to the forum view or discuss page
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/forum/view.php"),
                ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                ExpectedConditions.urlContains("/mod/forum/"),
                ExpectedConditions.invisibilityOfElementLocated(
                        By.xpath("//*[normalize-space(.) = " + xpathLiteral(title) + "]")
                )
        ));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Enter text into Moodle's message editor for new-discussion / reply forms.
     * Priority: 1) plain textarea  2) Atto contenteditable  3) TinyMCE iframe
     */
    private void enterMessageInEditor(String message) {
        writeToMoodleEditor(message);
    }

    /**
     * Same as {@link #enterMessageInEditor} but used in the edit flow.
     * The method is separate so it can be called after the edit form opens,
     * at which point we check all editor variants fresh.
     */
    private void enterMessageInEditorForEdit(String message) {
        writeToMoodleEditor(message);
    }

    /**
     * Core logic: detect which Moodle editor is present and write {@code message} into it.
     *
     * Priority:
     *  1. Atto contenteditable div  (most common on the Moodle demo site)
     *  2. Visible plain textarea
     *  3. TinyMCE iframe
     *
     * The Atto editor hides the underlying textarea in the DOM (display:none), so
     * we must never pick an invisible textarea over a visible contenteditable.
     */
    private void writeToMoodleEditor(String message) {
        driver.switchTo().defaultContent();

        // Wait for any message editor to appear — use broad selectors first, then narrow down
        // Broad: any contenteditable, any textarea whose id contains 'message', any TinyMCE iframe
        By broadAtto   = By.cssSelector("[contenteditable='true']");
        By broadTa     = By.cssSelector("textarea[id*='message'], textarea[name='message']");
        By broadIframe = By.cssSelector("iframe[id*='message'], iframe[id$='_ifr']");

        // Log what's on the form to aid debugging
        wait.until(d -> {
            List<WebElement> ce = d.findElements(broadAtto);
            List<WebElement> ta = d.findElements(broadTa);
            List<WebElement> fr = d.findElements(broadIframe);
            // Log the first editor found
            ce.stream().filter(WebElement::isDisplayed).findFirst().ifPresent(
                    e -> System.out.println("EDITOR-FOUND contenteditable id=" + e.getAttribute("id")
                            + " class=" + e.getAttribute("class")));
            ta.stream().filter(WebElement::isDisplayed).findFirst().ifPresent(
                    e -> System.out.println("EDITOR-FOUND textarea id=" + e.getAttribute("id")));
            if (!fr.isEmpty()) System.out.println("EDITOR-FOUND iframe id=" + fr.get(0).getAttribute("id"));

            for (WebElement e : ce) { if (e.isDisplayed()) return true; }
            for (WebElement e : ta) { if (e.isDisplayed()) return true; }
            return !fr.isEmpty();
        });

        // Re-query after wait
        By attoBy  = broadAtto;
        By taBy    = broadTa;
        By iframeBy = broadIframe;

        // 1. Atto editor (most common on demo site) — clear via JS, type via sendKeys
        for (WebElement ed : driver.findElements(attoBy)) {
            if (ed.isDisplayed()) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].focus(); arguments[0].innerHTML='';", ed);
                ed.sendKeys(message);
                return;
            }
        }

        // 2. Visible plain textarea (plain/text editor plugin)
        for (WebElement ta : driver.findElements(taBy)) {
            if (ta.isDisplayed()) {
                ta.clear();
                ta.sendKeys(message);
                return;
            }
        }

        // 3. TinyMCE iframe fallback
        if (!driver.findElements(iframeBy).isEmpty()) {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframeBy));
            try {
                WebElement body = wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("body#tinymce, body")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", body);
                body.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                body.sendKeys(message);
            } finally {
                driver.switchTo().defaultContent();
            }
            return;
        }

        throw new NoSuchElementException(
                "Could not locate any Moodle message editor on the current page.");
    }

    /** Build an XPath string literal that handles single and double quotes. */
    private String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }

        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        // Value contains both quote types: use concat()
        String[] parts = value.split("'", -1);
        StringBuilder result = new StringBuilder("concat(");

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(", \"'\", ");
            }
            result.append("'").append(parts[i]).append("'");
        }

        result.append(")");
        return result.toString();
    }

    /** Scroll an element into view, then click it (JS-fallback on interception). */
    private void clickSafely(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element
        );

        try {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();", element
            );
        }
    }
}
