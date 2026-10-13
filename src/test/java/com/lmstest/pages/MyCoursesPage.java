package com.lmstest.pages;

import com.lmstest.base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;

public class MyCoursesPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By courseLinks =
            By.cssSelector("[data-region='courses-view'] a.coursename");

    public MyCoursesPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public MyCoursesPage open() {
        driver.get(BaseTest.BASE_URL + "/my/courses.php");

        wait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(
                        courseLinks, 0
                )
        );

        return this;
    }

    // Return the names of enrolled courses.
    public List<String> getCourseNames() {
        List<String> names = new ArrayList<>();

        for (WebElement link : driver.findElements(courseLinks)) {
            String name = nameOf(link);

            if (!name.isBlank()) {
                names.add(name);
            }
        }

        return names;
    }

    // Open a course by its position in the list.
    public String openCourse(int index) {
        List<WebElement> links = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(courseLinks)
        );

        if (index < 0 || index >= links.size()) {
            throw new NoSuchElementException(
                    "Course index is out of range: " + index
            );
        }

        WebElement link = links.get(index);
        String name = nameOf(link);

        clickSafely(link);

        return name;
    }

    // Open an enrolled course by its displayed name.
    public String openCourseByName(String courseName) {
        wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(courseLinks)
        );

        for (int i = 0; i < 3; i++) {
            try {
                List<WebElement> links = driver.findElements(courseLinks);

                for (int j = 0; j < links.size(); j++) {
                    // Re-fetch the element in case the page refreshed.
                    WebElement link = driver.findElements(courseLinks).get(j);
                    String name = nameOf(link);

                    if (name.equalsIgnoreCase(courseName.trim())) {
                        clickSafely(link);
                        return name;
                    }
                }

                break;

            } catch (StaleElementReferenceException e) {
                // Retry if Moodle refreshes the course list.
            }
        }

        throw new NoSuchElementException(
                "Course not found in My Courses: " + courseName
        );
    }

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

    // Read the visible course name.
    private String nameOf(WebElement link) {
        List<WebElement> labels =
                link.findElements(By.cssSelector(".multiline"));

        String text = labels.isEmpty()
                ? link.getText()
                : labels.get(0).getText();

        for (String line :
                text.replace("Course name", "").split("\\R")) {
            if (!line.trim().isEmpty()) {
                return line.trim();
            }
        }

        return "";
    }
}