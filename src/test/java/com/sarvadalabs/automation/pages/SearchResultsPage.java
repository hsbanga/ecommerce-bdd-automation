package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.config.ConfigManager;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

public class SearchResultsPage extends ProductGridPage {

    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    public SearchResultsPage open(String term) {
        openPath(ConfigManager.path("path.search", term));
        return this;
    }

    public boolean hasNoResultsMessage() {
        return tryFind("search.noResults", Duration.ofSeconds(10))
                .map(el -> el.getText().toLowerCase().contains("no results"))
                .orElse(false);
    }
}
