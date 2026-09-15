package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.config.ConfigManager;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.List;

public class SearchResultsPage extends ProductGridPage {

    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    public SearchResultsPage open(String term) {
        openPath(ConfigManager.path("path.search", term));
        return this;
    }

    private static final List<String> NO_RESULT_PHRASES =
            List.of("no results", "no products", "nothing found", "not found", "0 results");

    public boolean hasNoResultsMessage() {
        return tryFind("search.noResults", Duration.ofSeconds(10))
                .map(el -> el.getText().toLowerCase())
                .map(text -> NO_RESULT_PHRASES.stream().anyMatch(text::contains))
                .orElse(false);
    }
}
