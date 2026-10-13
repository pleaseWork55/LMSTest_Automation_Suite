
package com.lmstest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class ForumPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By discussionLinks =
            By.cssSelector("a[href*='/mod/forum/discuss.php']");

    private final By forumPost =
            By.cssSelector(".forumpost");

    private final By postContent =
            By.cssSelector(".forumpost .posting");

    public ForumPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    // Open a forum by its displayed name.
    public void openForum(String forumName) {
        By forumLinks = By.cssSelector(
                "a[href*='/mod/forum/view.php']"
        );

        wait.until(d -> d.findElements(forumLinks).stream()
                .anyMatch(link -> link.isDisplayed()
                        && normalize(link.getText())
                        .contains(normalize(forumName))));

        List<WebElement> links =
                driver.findElements(forumLinks);

        for (WebElement link : links) {
            if (link.isDisplayed()
                    && normalize(link.getText())
                    .contains(normalize(forumName))) {

                String url = link.getAttribute("href");

                if (url == null || url.isBlank()) {
                    continue;
                }

                driver.navigate().to(url);

                wait.until(ExpectedConditions.urlContains(
                        "/mod/forum/view.php"
                ));
                return;
            }
        }

        throw new NoSuchElementException(
                "Forum not found: " + forumName
        );
    }

    // Open a discussion without hardcoding its ID.
    public void openDiscussion(String discussionTitle) {
        wait.until(d ->
                !d.findElements(discussionLinks).isEmpty()
        );

        String discussionUrl =
                findDiscussionUrl(discussionTitle);

        if (discussionUrl == null) {
            throw new NoSuchElementException(
                    "Could not find discussion: "
                            + discussionTitle
            );
        }

        driver.navigate().to(discussionUrl);

        wait.until(ExpectedConditions.urlContains(
                "/mod/forum/discuss.php"
        ));

        wait.until(d -> isDiscussionDisplayed(discussionTitle));
    }

    // Find the discussion URL using its surrounding text.
    private String findDiscussionUrl(String discussionTitle) {
        List<WebElement> links =
                driver.findElements(discussionLinks);

        for (WebElement link : links) {
            try {
                String href = link.getAttribute("href");

                if (href == null || href.isBlank()) {
                    continue;
                }

                WebElement container = link;

                for (int i = 0; i < 6; i++) {
                    if (normalize(container.getText())
                            .contains(normalize(discussionTitle))) {
                        return href;
                    }

                    try {
                        container = container.findElement(
                                By.xpath("..")
                        );
                    } catch (NoSuchElementException e) {
                        break;
                    }
                }
            } catch (StaleElementReferenceException ignored) {
                // Continue with the next link.
            }
        }

        // Use the only available discussion link if there is just one.
        List<WebElement> currentLinks =
                driver.findElements(discussionLinks);

        if (currentLinks.size() == 1) {
            return currentLinks.get(0).getAttribute("href");
        }

        return null;
    }

    // Check whether the discussion title is displayed.
    public boolean isDiscussionDisplayed(String discussionTitle) {
        By title = By.xpath(
                "//*[normalize-space(.)='"
                        + discussionTitle + "']"
        );

        return driver.findElements(title).stream()
                .anyMatch(WebElement::isDisplayed);
    }

    // Check whether the original post content is visible.

    public boolean isPostContentDisplayed() {
        WebElement post = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        forumPost
                )
        );

        return !post.getText().trim().isEmpty();
    }


    // Click Reply on the original post.

    public void clickReplyOnOriginalPost() {
        WebElement originalPost = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        forumPost
                )
        );

        WebElement reply = originalPost.findElement(
                By.xpath(".//a[contains(normalize-space(.), 'Reply')]")
        );

        String replyUrl = reply.getAttribute("href");

        if (replyUrl != null && !replyUrl.isBlank()) {
            driver.navigate().to(replyUrl);
        } else {
            clickSafely(reply);
        }
    }


    // Enter the reply.

    public void enterReply(String message) {
        // Wait until Moodle's rich-text editor iframe is available.
        By editorFrame = By.id("id_message_ifr");

        wait.until(
                ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                        editorFrame
                )
        );

        try {
            // The editable area is inside the iframe.
            WebElement editorBody = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.tagName("body")
                    )
            );

            editorBody.click();
            editorBody.sendKeys(message);

        } finally {
            // Always return Selenium to the main page.
            driver.switchTo().defaultContent();
        }
    }


    // Submit the reply.
    public void submitReply() {
        By submit = By.cssSelector(
                "button[name='submitbutton'], "
                        + "input[name='submitbutton']"
        );

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(submit)
        );

        clickSafely(button);
    }

    // Verify that the reply appears.
    public boolean isReplyDisplayed(String message) {
        By replyText = By.xpath(
                "//*[contains(normalize-space(.), '"
                        + message + "')]"
        );

        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        replyText
                )
        ).isDisplayed();
    }

    // Scroll to an element and click it safely.
    private void clickSafely(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                element
        );

        try {
            wait.until(
                    ExpectedConditions.elementToBeClickable(element)
            ).click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();",
                    element
            );
        }
    }

    // Normalize spaces and newlines.
    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("\\s+", " ").trim();
    }
}
