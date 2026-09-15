package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.core.BasePage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class HomePage extends BasePage {

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        openPath(ConfigManager.path("path.home"));
        return this;
    }

    public boolean isLogoVisible() {
        return isVisible("header.logo", Duration.ofSeconds(10));
    }

    /**
     * Searches through the header search UI. Falls back to the search URL when the
     * store has no visible search box (some themes only expose a search page).
     */
    public SearchResultsPage searchFor(String term) {
        log.info("Searching for '{}'", term);
        if (tryFind("header.searchInput", Duration.ofSeconds(1)).isEmpty()) {
            tryFind("header.searchIcon", Duration.ofSeconds(5)).ifPresent(icon -> click(icon, "header.searchIcon"));
        }
        if (tryFind("header.searchInput", Duration.ofSeconds(5)).isPresent()) {
            WebElement input = find("header.searchInput");
            input.clear();
            input.sendKeys(term);
            input.sendKeys(Keys.ENTER);
        } else {
            log.warn("No search input found, navigating to the search URL directly");
            openPath(ConfigManager.path("path.search", term));
        }
        waitForUrlContains(ConfigManager.pathPrefix("path.search"));
        waitForPageReady();
        return new SearchResultsPage(driver);
    }
}
