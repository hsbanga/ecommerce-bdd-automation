package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.config.ConfigManager;
import org.openqa.selenium.WebDriver;

public class CollectionPage extends ProductGridPage {

    public CollectionPage(WebDriver driver) {
        super(driver);
    }

    public CollectionPage open(String handle) {
        openPath(ConfigManager.path("path.collection", handle));
        return this;
    }
}
