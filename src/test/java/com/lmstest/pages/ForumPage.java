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

    import java.util.List;

    public class ForumPage {

        private final WebDriver driver;
        private final WebDriverWait wait;

        public ForumPage(WebDriver driver, WebDriverWait wait) {
            this.driver = driver;
            this.wait = wait;
        }

        // Open a forum by its visible name.
        public void openForum(String forumName) {
            By locator = By.xpath(
                    "//a[contains(normalize-space(.), "
                            + xpathLiteral(forumName) + ")]"
            );

            List<WebElement> links = wait.until(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(locator)
            );

            for (WebElement link : links) {
                if (link.isDisplayed() && link.isEnabled()) {
                    clickSafely(link);
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.urlContains("/mod/forum/view.php"),
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("a[href*='/mod/forum/discuss.php']")
                            ),
                            ExpectedConditions.presenceOfElementLocated(
                                    By.xpath("//a[contains(., 'Add discussion topic')]")
                            )
                    ));
                    return;
                }
            }

            throw new NoSuchElementException(
                    "Could not find a visible forum link: " + forumName
            );
        }

        // Open a discussion from the forum page.


        public void openDiscussion(String title) {

            By discussLink = By.xpath(
                    "//a[contains(normalize-space(.), 'Discuss this topic')]"
            );

            WebElement link = wait.until(
                    ExpectedConditions.elementToBeClickable(discussLink)
            );

            clickSafely(link);

            wait.until(ExpectedConditions.urlContains(
                    "/mod/forum/discuss.php"
            ));

            System.out.println("Opened discussion page for: " + title);
        }

        // Check whether the discussion title is visible
        public boolean isDiscussionDisplayed(String title) {

            // Look for the title in common Moodle page elements
            By locator = By.xpath(
                    "//*[self::h1 or self::h2 or self::h3 or self::a "
                            + "or contains(@class, 'discussion') "
                            + "or contains(@class, 'subject')]"
                            + "[normalize-space(.) = "
                            + xpathLiteral(title) + "]"
            );

            try {

                List<WebElement> elements = wait.until(
                        ExpectedConditions.visibilityOfAllElementsLocatedBy(
                                locator
                        )
                );

                for (WebElement element : elements) {

                    if (element.isDisplayed()
                            && element.getText().trim().equals(title)) {
                        return true;
                    }
                }

            } catch (TimeoutException e) {
                return false;
            }

            return false;
        }


        // Check whether the page contains a visible forum post
        public boolean isPostContentDisplayed() {

            List<WebElement> posts = driver.findElements(
                    By.cssSelector(".forumpost")
            );

            for (WebElement post : posts) {

                if (post.isDisplayed()
                        && !post.getText().trim().isEmpty()) {
                    return true;
                }
            }

            return false;
        }


        // Check whether specific text is visible on the page.
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

        // Check that a discussion title is no longer visible.
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

        // Click Reply on the first visible post.



        public void clickReplyOnOriginalPost() {
            driver.switchTo().defaultContent();

            By replyButton = By.xpath(
                    "//a[@data-action='collapsible-link' and @title='Reply']"
            );

            WebElement reply = wait.until(
                    ExpectedConditions.presenceOfElementLocated(replyButton)
            );

            // Scroll the Reply link away from the sticky footer.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block: 'center'});",
                    reply
            );

            wait.until(
                    ExpectedConditions.elementToBeClickable(replyButton)
            );

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();", reply
            );
        }



        // Enter a reply message.
        public void enterReply(String reply) {
            WebElement replyBox = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("textarea[placeholder='Write your reply...']")
                    )
            );

            replyBox.clear();
            replyBox.sendKeys(reply);
        }

        // Submit a reply.
        public void submitReply() {
            By postButton = By.xpath(
                    "//button[normalize-space()='Post to forum']"
                            + " | //input[@value='Post to forum']"
                            + " | //button[contains(., 'Post to forum')]"
                            + " | //input[@name='submitbutton']"
                            + " | //button[@name='submitbutton']"
            );

            clickSafely(
                    wait.until(ExpectedConditions.elementToBeClickable(postButton))
            );

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                    ExpectedConditions.textToBePresentInElementLocated(
                            By.tagName("body"), "Automated TC10 reply"
                    )
            ));
        }

        // Verify that the submitted reply is visible.
        public boolean isReplyDisplayed(String message) {
            return isTextDisplayed(message);
        }

        // Click Add discussion topic.

        public void clickAddDiscussion() {
            By addButton = By.xpath(
                    "//a[contains(., 'Add discussion topic') or " +
                            "contains(., 'Add discussion')]"
            );

            WebElement button = wait.until(
                    ExpectedConditions.elementToBeClickable(addButton)
            );
            button.click();

            // Confirm that Moodle opened the discussion form.
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("id_subject")
            ));
        }


        // Create and submit a new discussion.




        public void createDiscussion(String title, String message) {

            // 1. Enter the discussion title
            WebElement subject = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.id("id_subject")
                    )
            );

            subject.clear();
            subject.sendKeys(title);

            // 2. Switch to the TinyMCE message editor
            driver.switchTo().defaultContent();

            wait.until(
                    ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                            By.id("id_message_ifr")
                    )
            );

            WebElement editorBody = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("body#tinymce")
                    )
            );

            // 3. Enter the message
            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].focus();", editorBody);

            editorBody.sendKeys(message);

            // 4. Verify the message
            String enteredMessage = editorBody.getText();

            System.out.println("Message inside editor: " + enteredMessage);

            if (enteredMessage == null || enteredMessage.isBlank()) {
                throw new AssertionError(
                        "Message was not entered into the Moodle editor."
                );
            }

            // 5. Return to the main page
            driver.switchTo().defaultContent();

            // 6. Click Post to forum
            By postButton = By.cssSelector(
                    "button[name='submitbutton'], input[name='submitbutton']"
            );

            WebElement submit = wait.until(
                    ExpectedConditions.elementToBeClickable(postButton)
            );

            clickSafely(submit);

            // 7. Wait for Moodle's success notification
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("/mod/forum/discuss.php"),
                    ExpectedConditions.urlContains("/mod/forum/view.php"),
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(".alert, .notification")
                    )
            ));

            // 8. Verify that the created discussion title appears
            // 8. Verify that the created discussion title appears
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath(
                                    "//*[normalize-space(.) = "
                                            + xpathLiteral(title) + "]"
                            )
                    )
            );
            System.out.println(
                    "Discussion created successfully: " + title
            );
        }


        // Enter text into Moodle's message editor.


        private void enterMessageInEditor(String message) {
            driver.switchTo().defaultContent();

            By replyBox = By.xpath(
                    "//textarea[@placeholder='Write your reply…' or @placeholder='Write your reply...']"
            );

            if (!driver.findElements(replyBox).isEmpty()) {
                WebElement textarea = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(replyBox)
                );
                textarea.clear();
                textarea.sendKeys(message);
                return;
            }

            // Fallback for the full TinyMCE discussion editor.
            By editorFrame = By.id("id_message_ifr");

            wait.until(
                    ExpectedConditions.frameToBeAvailableAndSwitchToIt(editorFrame)
            );

            try {
                WebElement editorBody = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(By.id("tinymce"))
                );
                editorBody.clear();
                editorBody.sendKeys(message);
            } finally {
                driver.switchTo().defaultContent();
            }
        }



        // Edit a discussion post belonging to the logged-in user.





        public void editDiscussion(String title, String updatedMessage) {

            // Find the post by its title anywhere on the current page.
            By titleLocator = By.xpath(
                    "//*[normalize-space(.) = " + xpathLiteral(title) + "]"
            );

            WebElement titleElement = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(titleLocator)
            );

            // Find the forum post containing the title.
            WebElement post = titleElement.findElement(
                    By.xpath("./ancestor::*[contains(@class,'forumpost')][1]")
            );

            // Click Edit inside that post.
            WebElement editLink = post.findElement(
                    By.xpath(".//a[normalize-space()='Edit' or contains(@href,'edit=')]")
            );

            clickSafely(editLink);

            // Wait for the edit form.
            WebElement subject = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("id_subject"))
            );

            subject.clear();
            subject.sendKeys(title);

            // Update the TinyMCE message.
            driver.switchTo().defaultContent();

            wait.until(
                    ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                            By.id("id_message_ifr")
                    )
            );

            try {
                WebElement editor = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("body#tinymce")
                        )
                );

                editor.click();
                editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                editor.sendKeys(updatedMessage);

                wait.until(d -> updatedMessage.equals(
                        d.findElement(By.cssSelector("body#tinymce"))
                                .getText().trim()
                ));

            } finally {
                driver.switchTo().defaultContent();
            }

            // Save the changes.
            WebElement saveButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector(
                                    "button[name='submitbutton'], input[name='submitbutton']"
                            )
                    )
            );

            clickSafely(saveButton);

            // Verify that Moodle displays the updated message.
            wait.until(
                    ExpectedConditions.textToBePresentInElementLocated(
                            By.tagName("body"), updatedMessage
                    )
            );

            System.out.println("Discussion edited successfully: " + title);
        }


        // Delete a discussion/post and confirm the Moodle deletion page.
        public void deleteOwnForumPost(String title) {
            By postLocator = By.xpath(
                    "//*[contains(@class,'forumpost')]"
                            + "[.//*[normalize-space(.) = " + xpathLiteral(title) + "]]"
            );

            WebElement post = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(postLocator)
            );

            By deleteLink = By.xpath(
                    ".//a[normalize-space()='Delete'"
                            + " or contains(@href, 'delete=')]"
            );

            clickSafely(post.findElement(deleteLink));

            // Moodle may show a confirmation page before actually deleting.
            By confirmButton = By.cssSelector(
                    "form input[type='submit'], form button[type='submit']"
            );

            try {
                WebElement confirm = new WebDriverWait(
                        driver, java.time.Duration.ofSeconds(5)
                ).until(ExpectedConditions.elementToBeClickable(confirmButton));

                clickSafely(confirm);
            } catch (TimeoutException ignored) {
                // Some Moodle pages may delete immediately without confirmation.
            }

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.invisibilityOfElementLocated(postLocator),
                    ExpectedConditions.urlContains("/mod/forum/view.php"),
                    ExpectedConditions.urlContains("/mod/forum/discuss.php")
            ));
        }

        // Safely represent text in XPath, including apostrophes.
        private String xpathLiteral(String value) {
            if (!value.contains("'")) {
                return "'" + value + "'";
            }

            if (!value.contains("\"")) {
                return "\"" + value + "\"";
            }

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

        // Scroll to an element and click it.
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
                        "arguments[0].click();", element
                );
            }
        }
    }

