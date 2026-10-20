package com.lmstest.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class CoursePage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By heading    = By.cssSelector("h1");
    private final By sections   = By.cssSelector("li.section, [data-for='section']");
    private final By activities = By.cssSelector("li.activity, [data-for='cmitem']");
    // "Page" activities open as normal HTML, so they are preferred for the open-resource check
    private final By pageLinks        = By.cssSelector("li.activity.modtype_page a.aalink");
    private final By anyActivityLinks = By.cssSelector("li.activity a.aalink");
    // Course index drawer (closed by default) and its section links
    // Must be the "Open course index" button; the drawer's own "close" button shares the same data-target
    private final By courseIndexToggle = By.cssSelector(".drawer-left-toggle button[data-action='toggle']");
    private final By sectionIndexLinks = By.cssSelector("#courseindex a[data-for='section_title']");

    public CoursePage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void waitUntilLoaded() {
        wait.until(ExpectedConditions.urlContains("/course/view.php"));
        wait.until(ExpectedConditions.presenceOfElementLocated(heading));
    }

    public String getTitle() {
        return driver.findElement(heading).getText().trim();
    }

    public int getSectionCount() {
        wait.until(ExpectedConditions.presenceOfElementLocated(sections));
        return driver.findElements(sections).size();
    }

    public int getActivityCount() {
        wait.until(ExpectedConditions.presenceOfElementLocated(activities));
        return driver.findElements(activities).size();
    }

    /**
     * Opens one page/resource of the course. Navigates by the link's href so it also works
     * when the section is collapsed. Returns the URL that was opened.
     */
    public String openFirstResource() {
        List<WebElement> links = driver.findElements(pageLinks);
        if (links.isEmpty()) {
            links = driver.findElements(anyActivityLinks);
        }
        if (links.isEmpty()) {
            throw new AssertionError("No activity links found on the course page");
        }
        String href = links.get(0).getAttribute("href");
        driver.get(href);
        return href;
    }

    /** Opens the course index drawer (if closed), clicks the section link at the given position, returns its name. */
    public String openSection(int index) {
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(sectionIndexLinks, index));
        WebElement link = driver.findElements(sectionIndexLinks).get(index);
        // The drawer is either already open (link visible) or closed (open button visible)
        wait.until(d -> link.isDisplayed() || d.findElement(courseIndexToggle).isDisplayed());
        if (!link.isDisplayed()) {
            driver.findElement(courseIndexToggle).click();
        }
        wait.until(ExpectedConditions.visibilityOf(link));
        String name = link.getText().trim();
        wait.until(ExpectedConditions.elementToBeClickable(link)).click();
        return name;
    }

    /** True once the browser has moved to a section (#section-N anchor or section.php page). */
    public boolean isSectionOpened() {
        return wait.until(ExpectedConditions.urlContains("section"));
    }

    public boolean isResourceOpened() {
        wait.until(ExpectedConditions.urlContains("/mod/"));
        boolean hasHeading = !driver.findElements(heading).isEmpty()
                && !driver.findElement(heading).getText().trim().isEmpty();
        boolean hasError = !driver.findElements(By.cssSelector(".errorbox, .alert-danger")).isEmpty();
        return hasHeading && !hasError;
    }
}
