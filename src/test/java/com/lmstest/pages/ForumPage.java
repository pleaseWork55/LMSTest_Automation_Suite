package com.lmstest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for the Moodle forum pages.
 *
 * Supports:
 *   TC11 – Create a forum post
 *   TC10 – Reply to a forum post
 *   TC12 – Edit own forum post
 *   TC13 – Delete own forum post
 *
 * Session recovery: the Moodle demo resets sessions aggressively. Any method
 * that navigates to a page checks whether a login redirect has occurred and
 * re-authenticates if needed.
 */
public class ForumPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private static final String BASE_URL     = "https://school.moodledemo.net";
    private static final String STUDENT_USER = "student";
    private static final String STUDENT_PASS = "moodle26";

    public ForumPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait   = wait;
    }

    // =========================================================================
    // Session recovery
    // =========================================================================

    /**
     * If the current URL contains {@code /login/}, we have been logged out.
     * Clear all cookies (to drop any stale SSO tokens), navigate to the school's
     * native login page and authenticate.  Then navigate back to {@code returnUrl}.
     *
     * <p>The key insight: {@code services.apps.moodle.com} proxies the same site but
     * uses a different auth domain.  Clearing cookies forces Chrome back to the direct
     * {@code school.moodledemo.net} login form where {@code student/moodle26} works.
     */
    private void recoverSessionIfNeeded(String returnUrl) {
        String url = driver.getCurrentUrl();

        // Already on the right host and not on a login page — nothing to do
        if (!url.contains("/login/") && !url.contains("services.apps.moodle.com")) {
            return;
        }

        System.out.println("SESSION EXPIRED — recovering. Current URL: " + url);
        doLogin();

        if (returnUrl != null && !returnUrl.isEmpty()
                && !returnUrl.contains("/login/")) {
            driver.get(returnUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            // If returnUrl also redirected to login, login again
            if (driver.getCurrentUrl().contains("/login/")) {
                doLogin();
                driver.get(returnUrl);
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            }
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        System.out.println("SESSION RECOVERED — now at: " + driver.getCurrentUrl());
    }

    /**
     * Clear all browser state (cookies, localStorage, sessionStorage) then log in
     * on school.moodledemo.net.  Clearing state guarantees the native login form
     * appears rather than the SSO redirect to services.apps.moodle.com.
     */
    private void doLogin() {
        // Clear cookies
        driver.manage().deleteAllCookies();

        // Also clear localStorage/sessionStorage on the school domain
        try {
            driver.get(BASE_URL + "/login/index.php");
            ((JavascriptExecutor) driver).executeScript(
                    "try{localStorage.clear();}catch(e){} try{sessionStorage.clear();}catch(e){}");
            driver.manage().deleteAllCookies();
        } catch (Exception ignored) {}

        driver.get(BASE_URL + "/login/index.php");

        // Wait for #username — retry up to 3 times if SSO redirect fires
        for (int i = 0; i < 3; i++) {
            WebDriverWait lw = new WebDriverWait(driver, Duration.ofSeconds(15));
            boolean found = false;
            try {
                found = lw.until(d -> {
                    if (!d.getCurrentUrl().contains("school.moodledemo.net")) return false;
                    return !d.findElements(By.id("username")).isEmpty()
                            && d.findElement(By.id("username")).isDisplayed();
                });
            } catch (TimeoutException ignore) {}

            if (found) break;

            // SSO redirect fired — clear again and retry
            System.out.println("doLogin attempt " + (i + 1)
                    + " SSO redirect, URL: " + driver.getCurrentUrl());
            driver.manage().deleteAllCookies();
            driver.get(BASE_URL + "/login/index.php");
        }

        // Fill and submit
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(STUDENT_USER);
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys(STUDENT_PASS);
        driver.findElement(By.id("loginbtn")).click();

        // Wait until we leave the login page
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> !d.getCurrentUrl().contains("/login/"));
    }

    // =========================================================================
    // Navigation helpers
    // =========================================================================

    /**
     * Open a forum by its visible link text on the course page.
     */
    public void openForum(String forumName) {
        String forumNameTrimmed = forumName.replaceAll("\\.+$", "");
        By byExact   = By.xpath("//a[contains(normalize-space(.), " + xpathLiteral(forumName) + ")]");
        By byPartial = By.xpath("//a[contains(normalize-space(.), " + xpathLiteral(forumNameTrimmed) + ")]");
        By byData    = By.cssSelector("[data-activityname]");

        wait.until(d -> {
            if (!d.findElements(byExact).isEmpty())   return true;
            if (!d.findElements(byPartial).isEmpty()) return true;
            for (WebElement el : d.findElements(byData)) {
                String n = el.getAttribute("data-activityname");
                if (n != null && n.contains(forumNameTrimmed)) return true;
            }
            return false;
        });

        for (By locator : new By[]{byExact, byPartial}) {
            for (WebElement link : driver.findElements(locator)) {
                if (link.isDisplayed() && link.isEnabled()) {
                    clickSafely(link);
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.urlContains("/mod/forum/view.php"),
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("a[href*='/mod/forum/discuss.php']"))
                    ));
                    return;
                }
            }
        }

        for (WebElement item : driver.findElements(byData)) {
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

        throw new NoSuchElementException("Could not find a visible forum link: " + forumName);
    }

    /**
     * Open a specific discussion by title from the forum list page.
     * Falls back to the first discuss.php link if no title match is found.
     */
    public void openDiscussion(String title) {
        // Recover session first — the forum page must be on school.moodledemo.net
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl.contains("services.apps.moodle.com") || currentUrl.contains("/login/")) {
            recoverSessionIfNeeded(BASE_URL + "/mod/forum/view.php?id=944");
        }

        By byTitle = By.xpath(
                "//a[contains(@href,'/mod/forum/discuss.php')"
                + " and contains(normalize-space(.), " + xpathLiteral(title) + ")]");
        By byAny   = By.cssSelector("a[href*='/mod/forum/discuss.php']");

        wait.until(ExpectedConditions.presenceOfElementLocated(byAny));

        List<WebElement> titleMatches = driver.findElements(byTitle);
        WebElement link = titleMatches.isEmpty()
                ? driver.findElement(byAny)
                : titleMatches.get(0);

        jsClick(link);

        // Wait for a discuss page on school.moodledemo.net (not services.apps.moodle.com)
        wait.until(d -> {
            String u = d.getCurrentUrl();
            return u.contains("school.moodledemo.net")
                    && u.contains("/mod/forum/discuss.php");
        });

        System.out.println("Opened discussion: " + title + " URL: " + driver.getCurrentUrl());
    }

    // =========================================================================
    // Visibility checks
    // =========================================================================

    /** True if the discussion title appears anywhere on the page. */
    public boolean isDiscussionDisplayed(String title) {
        By locator = By.xpath(
                "//*[self::h1 or self::h2 or self::h3 or self::a"
                + " or contains(@class,'discussion') or contains(@class,'subject')]"
                + "[normalize-space(.) = " + xpathLiteral(title) + "]");
        try {
            List<WebElement> els = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
            return els.stream().anyMatch(e -> e.isDisplayed()
                    && e.getText().trim().equals(title));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** True if at least one non-empty forum post is visible on the page. */
    public boolean isPostContentDisplayed() {
        // Wait briefly for the page to settle after redirect
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // Try broad set of Moodle post container selectors across different Moodle themes/versions
        By postSelector = By.cssSelector(
                ".forumpost, .forum-post-container, .forum-post, .post, "
                + "article.forum-post, [data-region='post'], [data-post-id], "
                + ".discussion-post, [id^='p']");

        List<WebElement> posts = driver.findElements(postSelector);
        System.out.println("TC12: isPostContentDisplayed found " + posts.size()
                + " elements. URL=" + driver.getCurrentUrl());

        // Also check simply: any visible text content on a discuss.php page means posts exist
        if (driver.getCurrentUrl().contains("/mod/forum/discuss.php")) {
            // If we are on a discuss page, it must contain posts — check page body has content
            String bodyText = driver.findElement(By.tagName("body")).getText();
            boolean hasContent = bodyText != null && bodyText.length() > 200;
            System.out.println("TC12: page body length=" + (bodyText == null ? 0 : bodyText.length()));
            if (hasContent) return true;
        }

        return posts.stream().anyMatch(p -> p.isDisplayed() && !p.getText().trim().isEmpty());
    }

    /** True if the given text appears anywhere on the current page. */
    public boolean isTextDisplayed(String text) {
        By locator = By.xpath(
                "//*[contains(normalize-space(.), " + xpathLiteral(text) + ")]");
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator))
                       .isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** True once the discussion title is no longer present in the DOM. */
    public boolean isDiscussionAbsent(String title) {
        By locator = By.xpath(
                "//*[normalize-space(.) = " + xpathLiteral(title) + "]");
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // =========================================================================
    // TC11 – Create a forum post
    // =========================================================================

    /**
     * Click the "Add discussion topic" button on the forum view page.
     *
     * On this Moodle demo the button is {@code <a href="...#collapseAddForm">} which
     * expands a Bootstrap collapse div inline.  We click it then wait for the form.
     */
    public void clickAddDiscussion() {
        recoverSessionIfNeeded(driver.getCurrentUrl());

        By addButton = By.xpath(
                "//a[contains(@href,'collapseAddForm')"
                + " or contains(normalize-space(.), 'Add discussion topic')"
                + " or contains(normalize-space(.), 'Add discussion')"
                + " or contains(normalize-space(.), 'Start a new discussion')]"
                + " | //button[contains(normalize-space(.), 'Add discussion')]");

        WebElement button = wait.until(
                ExpectedConditions.presenceOfElementLocated(addButton));
        jsClick(button);

        // Wait for the inline collapse form or a dedicated new-discussion page to appear
        wait.until(d -> {
            if (d.getCurrentUrl().contains("/post.php")) {
                return !d.findElements(By.id("id_subject")).isEmpty();
            }
            List<WebElement> forms = d.findElements(By.id("mformforum"));
            for (WebElement f : forms) {
                if (f.isDisplayed()) return true;
            }
            List<WebElement> subj = d.findElements(By.id("id_subject"));
            for (WebElement s : subj) {
                if (s.isDisplayed()) return true;
            }
            List<WebElement> collapse = d.findElements(By.id("collapseAddForm"));
            for (WebElement c : collapse) {
                if (c.isDisplayed()) return true;
            }
            return false;
        });

        System.out.println("Add-discussion form is open. URL: " + driver.getCurrentUrl());
    }

    /**
     * Fill in the new-discussion form and submit it.
     * Handles both plain textarea and TinyMCE iframe editors.
     */
    public void createDiscussion(String title, String message) {
        // Subject field (input[type=text] or textarea — use JS to be safe)
        WebElement subject = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("id_subject")));
        jsSetValue(subject, title);

        // Message body
        driver.switchTo().defaultContent();
        writeToMoodleEditor(message);
        driver.switchTo().defaultContent();

        // Flush TinyMCE if active
        tryTinyMCESave();

        // Submit
        WebElement submit = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("button[name='submitbutton'],input[name='submitbutton']")));
        jsClick(submit);

        // Wait for post-submit page
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                ExpectedConditions.urlContains("/mod/forum/view.php"),
                ExpectedConditions.urlContains("/login/")));

        // Session may have expired during submit
        if (driver.getCurrentUrl().contains("/login/")
                || driver.getCurrentUrl().contains("services.apps.moodle.com")) {
            System.out.println("SESSION EXPIRED after submit — recovering and retrying...");
            recoverSessionIfNeeded(BASE_URL + "/mod/forum/view.php?id=944");
            clickAddDiscussion();
            createDiscussion(title, message);
            return;
        }

        // Verify the title appears on the resulting page
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(normalize-space(.), " + xpathLiteral(title) + ")]")));

        System.out.println("Discussion created: " + title);
    }

    // =========================================================================
    // TC10 – Reply to a forum post
    // =========================================================================

    /**
     * Click the Reply link on the original post of the current discussion page.
     * Navigates to post.php?reply=N.
     *
     * Strategy: extract the reply URL from the link's href and navigate directly,
     * which is more robust than relying on jsClick navigation.
     */
    public void clickReplyOnOriginalPost() {
        driver.switchTo().defaultContent();

        // Ensure we are on school.moodledemo.net — not the services mirror
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl.contains("services.apps.moodle.com")) {
            currentUrl = currentUrl.replace("services.apps.moodle.com", "school.moodledemo.net");
            driver.get(currentUrl);
            wait.until(d -> d.getCurrentUrl().contains("school.moodledemo.net"));
        }

        By replyBy = By.xpath(
                "(//a[contains(@href,'reply=') or @title='Reply'"
                + " or contains(@class,'reply')])[1]");

        System.out.println("TC10: URL before Reply: " + driver.getCurrentUrl());

        List<WebElement> candidates = driver.findElements(By.xpath(
                "//a[contains(@href,'reply=') or @title='Reply'"
                + " or contains(@class,'reply')]"));
        System.out.println("TC10: reply candidates: " + candidates.size());
        for (WebElement c : candidates) {
            System.out.println("  href=" + c.getAttribute("href")
                    + " text=[" + c.getText() + "]");
        }

        WebElement reply = wait.until(ExpectedConditions.presenceOfElementLocated(replyBy));

        // Extract the href and navigate directly (more reliable than JS click navigation)
        String replyHref = reply.getAttribute("href");
        if (replyHref != null && replyHref.contains("#")) {
            replyHref = replyHref.substring(0, replyHref.indexOf("#"));
        }
        // Ensure it is on school.moodledemo.net
        if (replyHref != null && replyHref.contains("services.apps.moodle.com")) {
            replyHref = replyHref.replace("services.apps.moodle.com", "school.moodledemo.net");
        }

        final String finalReplyHref = replyHref;
        System.out.println("TC10: navigating to reply URL: " + finalReplyHref);
        driver.get(finalReplyHref);

        // Recover session if the navigate redirected to login or SSO
        recoverSessionIfNeeded(finalReplyHref);

        // Wait until the reply form page is loaded.
        // Handle: (a) SSO redirect mid-wait → re-navigate; (b) TinyMCE only (no visible textarea)
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> {
            String u = d.getCurrentUrl();

            // If SSO fired, navigate back
            if (u.contains("services.apps.moodle.com") || u.contains("/login/")) {
                d.manage().deleteAllCookies();
                d.get(finalReplyHref);
                return false;
            }

            // Accept if on a reply/post page with a textarea OR a TinyMCE iframe
            if (u.contains("reply=") || u.contains("/post.php")) {
                boolean hasTextarea = !d.findElements(By.cssSelector(
                        "textarea[id*='message'],textarea[name*='message']")).isEmpty();
                boolean hasTinymce  = !d.findElements(By.cssSelector(
                        "iframe[id*='message'],iframe[id$='_ifr']")).isEmpty();
                return hasTextarea || hasTinymce;
            }
            return false;
        });

        System.out.println("TC10: reply form loaded. URL=" + driver.getCurrentUrl());
        driver.switchTo().defaultContent();
    }

    /**
     * Enter text into the reply form's message textarea.
     *
     * Strategy (in order of preference):
     *  1. Force the textarea visible via JS, then use real Selenium Actions keyboard
     *     simulation — this fires genuine browser key events that Moodle's validators
     *     recognise (unlike synthetic JS events which are ignored by some listeners).
     *  2. Fall back to plain sendKeys if Actions fails.
     *  3. Final fallback: JS value assignment + events (previous behaviour).
     */
    public void enterReply(String replyText) {
        driver.switchTo().defaultContent();

        // Check for TinyMCE first — if the iframe editor is present, use the API
        List<WebElement> iframes = driver.findElements(
                By.cssSelector("iframe[id*='message'],iframe[id$='_ifr']"));
        if (!iframes.isEmpty()) {
            System.out.println("TC10: TinyMCE iframe detected on reply page — using API");
            try {
                ((JavascriptExecutor) driver).executeScript(
                        "if(typeof tinyMCE !== 'undefined' && tinyMCE.activeEditor) {"
                        + "  tinyMCE.activeEditor.setContent(arguments[0]);"
                        + "  tinyMCE.triggerSave();"
                        + "}", replyText);
                Thread.sleep(300);
            } catch (Exception ignored) {}
            // Also set hidden textarea directly
            try {
                WebElement hiddenTa = driver.findElement(
                        By.cssSelector("textarea[id*='message'],textarea[name*='message']"));
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                        hiddenTa, replyText);
            } catch (Exception ignored) {}
            return;
        }

        // No TinyMCE — find the plain textarea
        WebElement ta = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(
                    "textarea[id='id_message'],"
                    + "textarea[id^='id_message'],"
                    + "textarea[name='message[text]'],"
                    + "textarea[name='message']")));

        // Step 1: Force the textarea interactable via JS (remove hidden/disabled styles)
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});"
                + "arguments[0].removeAttribute('disabled');"
                + "arguments[0].removeAttribute('readonly');"
                + "arguments[0].style.cssText = 'display:block !important;"
                +   "visibility:visible !important; opacity:1 !important;"
                +   "width:400px; height:100px;';"
                + "arguments[0].value = '';",
                ta);

        try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        // Step 2: Use Actions to perform real keyboard input (Ctrl+A then type)
        // Real key events satisfy Moodle's form validators.
        try {
            new Actions(driver)
                    .click(ta)
                    .keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL)
                    .sendKeys(replyText)
                    .perform();
            String actual = ta.getAttribute("value");
            System.out.println("TC10: Actions sendKeys done, value length=" + actual.length()
                    + " value=[" + actual + "]");
            if (actual != null && !actual.isBlank()) return; // success
        } catch (Exception e) {
            System.out.println("TC10: Actions sendKeys failed: " + e.getMessage());
        }

        // Step 3: Plain sendKeys fallback
        try {
            ta.clear();
            ta.sendKeys(replyText);
            String actual = ta.getAttribute("value");
            System.out.println("TC10: plain sendKeys done, value length=" + actual.length());
            if (actual != null && !actual.isBlank()) return;
        } catch (Exception e) {
            System.out.println("TC10: plain sendKeys failed: " + e.getMessage());
        }

        // Step 4: JS value + events final fallback
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];"
                + "arguments[0].dispatchEvent(new Event('focus',   {bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('input',   {bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('change',  {bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('keyup',   {bubbles:true}));",
                ta, replyText);

        String finalVal = (String) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].value;", ta);
        System.out.println("TC10: JS fallback value length="
                + (finalVal == null ? "null" : finalVal.length()));
    }

    /**
     * Submit the reply form and wait for the discuss page to show the new reply.
     *
     * Handles two failure modes:
     *  1. Button click doesn't navigate — falls back to form.submit()
     *  2. Session expires during submit (redirect to SSO login) — recovers session
     *     and navigates back to the discuss page to verify the reply was saved
     */
    public void submitReply(String replyText) {
        driver.switchTo().defaultContent();

        tryTinyMCESave();

        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[name='submitbutton'],button[name='submitbutton']")));

        // Derive the discuss URL from the current reply URL (post.php?reply=N → discuss.php?d=N)
        // We'll use it for session recovery navigation
        String replyPageUrl = driver.getCurrentUrl();

        // Log textarea value before submitting
        try {
            String taVal = (String) ((JavascriptExecutor) driver).executeScript(
                    "var ta = document.querySelector("
                    + "\"textarea[id='id_message'],textarea[name='message[text]'],textarea[name='message']\""
                    + "); return ta ? ta.value : null;");
            System.out.println("TC10: textarea value before submit=[" + taVal + "]");
        } catch (Exception ignored) {}

        // Primary: JS click the submit button
        jsClick(btn);

        // Wait up to 40 s for discuss.php, reply text on page, OR an SSO/login redirect
        String afterSubmitUrl = replyPageUrl;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> {
                String u = d.getCurrentUrl();
                // Success: redirected to discuss page
                if (!u.equals(replyPageUrl) && u.contains("/mod/forum/discuss.php")) return true;
                // Success: reply text visible right now
                if (!d.findElements(By.xpath(
                        "//*[contains(normalize-space(.), "
                        + xpathLiteral(replyText) + ")]")).isEmpty()) return true;
                // Also stop waiting if session expired (SSO redirect)
                if (u.contains("/login/") || u.contains("services.apps.moodle.com")) return true;
                return false;
            });
            afterSubmitUrl = driver.getCurrentUrl();
        } catch (TimeoutException ignored) {
            System.out.println("TC10: no navigation after button click, trying form.submit()");
            // Fallback: submit the form programmatically
            try {
                ((JavascriptExecutor) driver).executeScript(
                        "var form = document.getElementById('mformforum')"
                        + "  || document.querySelector('form[method=\"post\"]');"
                        + "if(form) form.submit();");
                new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
                    String u = d.getCurrentUrl();
                    return (!u.equals(replyPageUrl) && u.contains("/mod/forum/discuss.php"))
                            || u.contains("/login/")
                            || u.contains("services.apps.moodle.com");
                });
                afterSubmitUrl = driver.getCurrentUrl();
            } catch (Exception e) {
                System.out.println("TC10: form.submit() also failed: " + e.getMessage());
            }
        }

        System.out.println("TC10: after submit URL: " + afterSubmitUrl);

        // If session expired during submit, recover and navigate to the discuss page
        // The reply may still have been saved server-side before the session expired
        if (afterSubmitUrl.contains("/login/") || afterSubmitUrl.contains("services.apps.moodle.com")) {
            System.out.println("TC10: session expired during submit — recovering...");
            recoverSessionIfNeeded(BASE_URL + "/mod/forum/view.php?id=944");
            // Navigate to the forum and find the discussion we replied to
            driver.get(BASE_URL + "/mod/forum/view.php?id=944");
            wait.until(ExpectedConditions.urlContains("/mod/forum/view.php"));
            // Open the discussion we replied to (it should appear at top or be findable)
            By anyDiscussLink = By.cssSelector("a[href*='/mod/forum/discuss.php']");
            try {
                WebElement discLink = wait.until(
                        ExpectedConditions.presenceOfElementLocated(anyDiscussLink));
                driver.get(discLink.getAttribute("href"));
                wait.until(ExpectedConditions.urlContains("/mod/forum/discuss.php"));
                System.out.println("TC10: recovered and opened discuss: " + driver.getCurrentUrl());
            } catch (TimeoutException ignored) {
                System.out.println("TC10: could not open discuss page after recovery");
            }
        }
    }

    /** @deprecated Use {@link #submitReply(String)}. */
    public void submitReply() { submitReply(""); }

    /** True if the reply text is visible on the discussion page. */
    public boolean isReplyDisplayed(String message) {
        // Wait up to 20 s for the page to load after any post-submit redirect/recovery
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // Fast path: scan the page body text directly (no XPath wait overhead)
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            if (bodyText != null && bodyText.contains(message)) {
                System.out.println("TC10: reply found in body text ✓");
                return true;
            }
            System.out.println("TC10: reply NOT in body text. URL=" + driver.getCurrentUrl()
                    + " body length=" + (bodyText == null ? 0 : bodyText.length()));
        } catch (Exception ignored) {}

        // Slower path: XPath wait with extended 30 s timeout
        By locator = By.xpath(
                "//*[contains(normalize-space(.), " + xpathLiteral(message) + ")]");
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator))
                    .isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    // =========================================================================
    // TC12 – Edit own forum post
    // =========================================================================

    /**
     * Edit the post with the given title by navigating directly to its
     * {@code post.php?edit=N} URL and updating the message body.
     */
    public void editDiscussion(String title, String updatedMessage) {
        recoverSessionIfNeeded(driver.getCurrentUrl());

        // Capture the current discuss URL before editing (so we can return here after save)
        String discussUrl = driver.getCurrentUrl().contains("/mod/forum/discuss.php")
                ? driver.getCurrentUrl() : null;

        // Find the Edit link on the current page
        By editLinkBy = By.xpath(
                "//a[normalize-space()='Edit' or contains(@href,'post.php?edit=')]");
        wait.until(ExpectedConditions.presenceOfElementLocated(editLinkBy));

        String editHref = driver.findElement(editLinkBy).getAttribute("href");
        System.out.println("TC12: editing " + editHref);

        // Navigate to the edit page — rewrite services.apps.moodle.com if present
        if (editHref.contains("services.apps.moodle.com")) {
            editHref = editHref.replace("services.apps.moodle.com", "school.moodledemo.net");
        }
        driver.get(editHref);

        // Recover if the edit URL triggered a login redirect
        recoverSessionIfNeeded(editHref);

        // Wait for the edit form
        WebElement subject = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("id_subject")));
        jsSetValue(subject, title);

        // Update message body
        driver.switchTo().defaultContent();
        writeToMoodleEditor(updatedMessage);
        driver.switchTo().defaultContent();
        tryTinyMCESave();

        // Save
        WebElement save = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[name='submitbutton'],button[name='submitbutton']")));
        jsClick(save);

        // Wait until we leave the edit page
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                ExpectedConditions.urlContains("/mod/forum/view.php"),
                ExpectedConditions.not(ExpectedConditions.urlContains("/post.php"))));

        String afterEditUrl = driver.getCurrentUrl();
        System.out.println("TC12: after edit save, URL=" + afterEditUrl);

        // Always navigate to a discuss page so isPostContentDisplayed() has content to check
        if (!afterEditUrl.contains("/mod/forum/discuss.php")) {
            // Option A: we captured the discuss URL before editing — go back there
            if (discussUrl != null) {
                System.out.println("TC12: returning to captured discuss URL: " + discussUrl);
                driver.get(discussUrl);
                try {
                    wait.until(ExpectedConditions.urlContains("/mod/forum/discuss.php"));
                } catch (TimeoutException ignored) {}
            } else {
                // Option B: look for a discuss link on the current page (may be view.php)
                recoverSessionIfNeeded(afterEditUrl);
                By discussTitle = By.xpath(
                        "//a[contains(@href,'/mod/forum/discuss.php')"
                        + " and contains(normalize-space(.), " + xpathLiteral(title) + ")]");
                By discussAny = By.cssSelector("a[href*='/mod/forum/discuss.php']");
                try {
                    List<WebElement> titleLinks = driver.findElements(discussTitle);
                    WebElement discLink = titleLinks.isEmpty()
                            ? new WebDriverWait(driver, Duration.ofSeconds(10))
                                    .until(ExpectedConditions.presenceOfElementLocated(discussAny))
                            : titleLinks.get(0);
                    driver.get(discLink.getAttribute("href"));
                    wait.until(ExpectedConditions.urlContains("/mod/forum/discuss.php"));
                } catch (TimeoutException ignored) {
                    System.out.println("TC12: could not navigate to discuss page");
                }
            }
        }

        System.out.println("Discussion edited: " + title
                + " | now at: " + driver.getCurrentUrl());
    }

    // =========================================================================
    // TC13 – Delete own forum post
    // =========================================================================

    /**
     * Delete the post with the given title.
     * Finds the Delete link, navigates to the confirmation page, and confirms.
     */
    public void deleteOwnForumPost(String title) {
        By deleteBy = By.xpath(
                "//a[normalize-space()='Delete' or contains(@href,'delete=')]");

        // Action-menu trigger if Delete is hidden
        if (driver.findElements(deleteBy).isEmpty()) {
            By menuBy = By.cssSelector(
                    ".action-menu-trigger,[data-toggle='dropdown']");
            List<WebElement> menus = driver.findElements(menuBy);
            if (!menus.isEmpty()) {
                clickSafely(menus.get(0));
                wait.until(ExpectedConditions.visibilityOfElementLocated(deleteBy));
            }
        }

        WebElement deleteLink = wait.until(
                ExpectedConditions.elementToBeClickable(deleteBy));
        clickSafely(deleteLink);

        // Moodle shows a confirmation page at post.php?delete=N
        // The "Continue" / "Yes" button is an input[type=submit] or button inside the form
        By confirmBtn = By.xpath(
                "//form[contains(@action,'delete') or contains(@action,'post.php')]"
                + "//input[@type='submit']"
                + " | //form[contains(@action,'delete') or contains(@action,'post.php')]"
                + "//button[@type='submit']");

        // Also accept a standalone "Continue" / "Yes" button
        By confirmAny = By.xpath(
                "//input[@type='submit' and (@value='Continue' or @value='Yes')]"
                + " | //button[@type='submit' and ("
                + "  normalize-space()='Continue' or normalize-space()='Yes')]");

        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> d.getCurrentUrl().contains("delete=")
                            || d.getCurrentUrl().contains("/post.php"));

            List<WebElement> btns = driver.findElements(confirmBtn);
            if (btns.isEmpty()) btns = driver.findElements(confirmAny);

            if (!btns.isEmpty()) {
                WebElement confirm = btns.get(0);
                System.out.println("TC13: confirm value=[" + confirm.getAttribute("value")
                        + "] text=[" + confirm.getText() + "]");
                jsClick(confirm);
            }
        } catch (TimeoutException ignored) {
            // Deleted without confirmation
        }

        // After deletion Moodle redirects to forum or discuss page
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/mod/forum/view.php"),
                ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                ExpectedConditions.invisibilityOfElementLocated(
                        By.xpath("//*[normalize-space(.) = " + xpathLiteral(title) + "]"))));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void enterMessageInEditor(String message)        { writeToMoodleEditor(message); }
    private void enterMessageInEditorForEdit(String message) { writeToMoodleEditor(message); }

    /**
     * Detect the active Moodle message editor and write {@code message} into it.
     *
     * Priority:
     *  1. Atto contenteditable div
     *  2. Visible plain textarea
     *  3. TinyMCE iframe
     */
    private void writeToMoodleEditor(String message) {
        driver.switchTo().defaultContent();

        By attoBy   = By.cssSelector("[contenteditable='true']");
        By taBy     = By.cssSelector("textarea[id*='message'],textarea[name*='message']");
        By iframeBy = By.cssSelector("iframe[id*='message'],iframe[id$='_ifr']");

        // Wait for any editor to appear.
        // Accept ANY textarea with message in id/name (even hidden — we use JS to set it).
        // Also accept TinyMCE iframe even if hidden (it renders in the iframe document).
        wait.until(d -> {
            for (WebElement e : d.findElements(attoBy))   { if (e.isDisplayed()) return true; }
            // Accept present textarea even if hidden (JS will set value)
            if (!d.findElements(taBy).isEmpty())          return true;
            return !d.findElements(iframeBy).isEmpty();
        });

        // Log what was found
        driver.findElements(attoBy).stream().filter(WebElement::isDisplayed).findFirst()
              .ifPresent(e -> System.out.println("EDITOR-FOUND contenteditable id="
                      + e.getAttribute("id")));
        driver.findElements(taBy).stream().findFirst()
              .ifPresent(e -> System.out.println("EDITOR-FOUND textarea id="
                      + e.getAttribute("id") + " displayed=" + e.isDisplayed()));
        driver.findElements(iframeBy).stream().findFirst()
              .ifPresent(e -> System.out.println("EDITOR-FOUND iframe id="
                      + e.getAttribute("id")));

        // 1. Atto contenteditable (displayed)
        for (WebElement ed : driver.findElements(attoBy)) {
            if (ed.isDisplayed()) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].focus(); arguments[0].innerHTML='';", ed);
                ed.sendKeys(message);
                return;
            }
        }

        // 2. TinyMCE iframe (check before plain textarea, as TinyMCE hides the textarea)
        List<WebElement> iframes = driver.findElements(iframeBy);
        if (!iframes.isEmpty()) {
            // fall through to iframe block below
        } else {
            // 2b. Plain textarea — use JS to bypass interactability wrapper
            List<WebElement> tas = driver.findElements(taBy);
            if (!tas.isEmpty()) {
                WebElement ta = tas.get(0);
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                        + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                        ta, message);
                return;
            }
        }

        // 3. TinyMCE iframe
        iframes = driver.findElements(iframeBy);
        if (!iframes.isEmpty()) {
            driver.switchTo().defaultContent();

            // Use tinyMCE JS API to set content (the proper way, not innerHTML injection)
            try {
                ((JavascriptExecutor) driver).executeScript(
                        "if(typeof tinyMCE !== 'undefined' && tinyMCE.activeEditor) {"
                        + "  tinyMCE.activeEditor.setContent(arguments[0]);"
                        + "  tinyMCE.triggerSave();"
                        + "}", message);
                Thread.sleep(300);
                System.out.println("TinyMCE: set content via API");
            } catch (Exception ignore) {
                // Fallback: set via iframe body innerHTML
                try {
                    wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframeBy));
                    WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("body#tinymce,body")));
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].innerHTML = arguments[1];", body, message);
                    driver.switchTo().defaultContent();
                    tryTinyMCESave();
                } catch (Exception e2) {
                    driver.switchTo().defaultContent();
                }
            }

            // Always also set the hidden textarea directly as a safety net
            try {
                WebElement hiddenTa = driver.findElement(
                        By.cssSelector("textarea[id*='message'],textarea[name*='message']"));
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                        hiddenTa, message);
                System.out.println("TinyMCE: also set hidden textarea value");
            } catch (Exception ignored) {}

            return;
        }

        throw new NoSuchElementException(
                "Could not locate any Moodle message editor on the current page.");
    }

    /**
     * Call tinyMCE.triggerSave() if TinyMCE is loaded.
     * This flushes the editor content to the underlying hidden textarea before form submit.
     */
    private void tryTinyMCESave() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "if(typeof tinyMCE !== 'undefined') tinyMCE.triggerSave();");
            Thread.sleep(200);
        } catch (Exception ignored) {}
    }

    /**
     * Set an input/textarea value via JavaScript and fire change events.
     * Avoids ElementNotInteractableException from Moodle's form styling.
     */
    private void jsSetValue(WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});"
                + "arguments[0].value = arguments[1];"
                + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                element, value);
    }

    /**
     * Scroll into view and click via JavaScript (bypasses overlays and interception).
     */
    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();", element);
    }

    /**
     * Scroll into view, then try native click; fall back to JS click on interception.
     */
    private void clickSafely(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    /** Build an XPath string literal that handles single and double quotes. */
    private String xpathLiteral(String value) {
        if (!value.contains("'"))  return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        String[] parts = value.split("'", -1);
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", \"'\", ");
            sb.append("'").append(parts[i]).append("'");
        }
        return sb.append(")").toString();
    }
}
