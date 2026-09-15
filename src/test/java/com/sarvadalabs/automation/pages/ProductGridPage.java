package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.core.BasePage;
import com.sarvadalabs.automation.core.Locators;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Shared behaviour for pages that list products in a grid: collections and search results. */
public abstract class ProductGridPage extends BasePage {

    protected ProductGridPage(WebDriver driver) {
        super(driver);
    }

    public int productCount() {
        return findAll("grid.items", Duration.ofSeconds(10)).size();
    }

    /** Visible product titles, one per grid card. */
    public List<String> productTitles() {
        List<String> titles = new ArrayList<>();
        for (WebElement item : findAll("grid.items", Duration.ofSeconds(10))) {
            // Themes often render the heading twice per card (one hidden for hover effects);
            // take the first one that actually has visible text.
            for (By by : Locators.of("grid.itemTitle")) {
                Optional<String> title = item.findElements(by).stream()
                        .map(el -> el.getText().trim())
                        .filter(t -> !t.isEmpty())
                        .findFirst();
                if (title.isPresent()) {
                    titles.add(title.get());
                    break;
                }
            }
        }
        return titles;
    }

    public ProductPage openFirstProduct() {
        click("grid.firstItemLink");
        waitForUrlContains("/products/");
        waitForPageReady();
        return new ProductPage(driver);
    }
}
