package com.lmstest.pages;

import com.lmstest.base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CourseSearchPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Course name links in the search results list
    private final By resultLinks = By.cssSelector(".coursebox .coursename a, .course-search-result .coursename a");

    public CourseSearchPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    /** Runs a course search for the given text and waits for results to be listed. */
    public CourseSearchPage searchFor(String text) {
        String query = URLEncoder.encode(text, StandardCharsets.UTF_8);
        driver.get(BaseTest.BASE_URL + "/course/search.php?search=" + query);
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(resultLinks, 0));
        return this;
    }

    /** Names of all courses in the search results. */
    public List<String> getResultNames() {
        List<String> names = new ArrayList<>();
        for (WebElement link : driver.findElements(resultLinks)) {
            names.add(link.getText().trim());
        }
        return names;
    }
}
