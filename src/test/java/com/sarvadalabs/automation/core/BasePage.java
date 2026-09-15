package com.sarvadalabs.automation.core;

import com.sarvadalabs.automation.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Common page behaviour: navigation, fallback-locator element lookup, resilient clicks,
 * JS helpers and header widgets shared by every storefront page.
 */
public abstract class BasePage {

    private static final long POLL_MS = 250;

    protected final Logger log = LogManager.getLogger(getClass());
    protected final WebDriver driver;
    protected final Duration timeout;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.timeout = Duration.ofSeconds(ConfigManager.getInt("timeout.element", 15));
    }

    // ------------------------------------------------------------------ navigation

    /** Opens an absolute URL or a path relative to base.url. */
    public void openPath(String path) {
        String url = path.startsWith("http") ? path : ConfigManager.baseUrl() + (path.startsWith("/") ? path : "/" + path);
        log.info("Navigating to {}", url);
        driver.get(url);
        waitForPageReady();
        dismissPopups();
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public String pageTitle() {
        return driver.getTitle();
    }

    public void waitForUrlContains(String fragment) {
        waitUntil(d -> d.getCurrentUrl().contains(fragment), "URL to contain '" + fragment + "'");
    }

    protected void waitForPageReady() {
        new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.getInt("timeout.pageLoad", 60)))
                .until(d -> "complete".equals(js("return document.readyState")));
    }

    /** Closes a cookie / newsletter popup if the store profile defines a popup.close locator. */
    protected void dismissPopups() {
        if (Locators.isDefined("popup.close")) {
            tryFind("popup.close", Duration.ofSeconds(2)).ifPresent(el -> {
                log.info("Dismissing popup");
                el.click();
            });
        }
    }

    // ------------------------------------------------------------------ element lookup

    /** Finds the first visible element for the locator key, waiting up to the default timeout. */
    protected WebElement find(String key, String... args) {
        return find(key, timeout, args);
    }

    protected WebElement find(String key, Duration wait, String... args) {
        return tryFind(key, wait, args).orElseThrow(() -> new NoSuchElementException(
                "No visible element for locator key '" + key + "' within " + wait.getSeconds() + "s. Tried: " + Locators.of(key, args)));
    }

    /** Tries each fallback locator in turn until a visible element appears or the wait elapses. */
    protected Optional<WebElement> tryFind(String key, Duration wait, String... args) {
        List<By> candidates = Locators.of(key, args);
        long deadline = System.nanoTime() + wait.toNanos();
        do {
            for (By by : candidates) {
                for (WebElement el : driver.findElements(by)) {
                    try {
                        if (el.isDisplayed()) {
                            return Optional.of(el);
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // DOM re-rendered while iterating; retry on next poll
                    }
                }
            }
            if (System.nanoTime() >= deadline) {
                break;
            }
            sleep(POLL_MS);
        } while (true);
        return Optional.empty();
    }

    /** Returns all elements for the first fallback locator that matches anything (visible or not). */
    protected List<WebElement> findAll(String key, String... args) {
        return findAll(key, timeout, args);
    }

    protected List<WebElement> findAll(String key, Duration wait, String... args) {
        List<By> candidates = Locators.of(key, args);
        long deadline = System.nanoTime() + wait.toNanos();
        do {
            for (By by : candidates) {
                List<WebElement> found = driver.findElements(by);
                if (!found.isEmpty()) {
                    return found;
                }
            }
            sleep(POLL_MS);
        } while (System.nanoTime() < deadline);
        return List.of();
    }

    /**
     * Searches within a parent element using the fallback locators of {@code key}. Visible matches
     * are preferred because themes often render the same value twice for different breakpoints.
     */
    protected Optional<WebElement> findWithin(WebElement parent, String key, String... args) {
        WebElement hiddenFallback = null;
        for (By by : Locators.of(key, args)) {
            for (WebElement el : parent.findElements(by)) {
                try {
                    if (el.isDisplayed()) {
                        return Optional.of(el);
                    }
                } catch (StaleElementReferenceException ignored) {
                    continue;
                }
                if (hiddenFallback == null) {
                    hiddenFallback = el;
                }
            }
        }
        return Optional.ofNullable(hiddenFallback);
    }

    protected boolean isVisible(String key, Duration wait, String... args) {
        return tryFind(key, wait, args).isPresent();
    }

    protected boolean isVisible(String key, String... args) {
        return isVisible(key, timeout, args);
    }

    // ------------------------------------------------------------------ interactions

    protected void click(String key, String... args) {
        click(find(key, args), key);
    }

    protected void click(WebElement element, String description) {
        scrollIntoView(element);
        try {
            element.click();
        } catch (ElementNotInteractableException e) {
            log.warn("Native click on '{}' was intercepted, falling back to JS click", description);
            js("arguments[0].click();", element);
        }
    }

    protected void type(String key, CharSequence text, String... args) {
        WebElement el = find(key, args);
        scrollIntoView(el);
        el.clear();
        el.sendKeys(text);
    }

    /** Replaces the current value of an input the way a user would (select all, type). */
    protected void replaceValue(WebElement input, String value) {
        scrollIntoView(input);
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
        input.sendKeys(value);
    }

    protected String text(String key, String... args) {
        return find(key, args).getText().trim();
    }

    // ------------------------------------------------------------------ waits & JS

    protected <T> T waitUntil(Function<WebDriver, T> condition, String description) {
        return waitUntil(condition, timeout, description);
    }

    protected <T> T waitUntil(Function<WebDriver, T> condition, Duration wait, String description) {
        return new WebDriverWait(driver, wait)
                .ignoring(StaleElementReferenceException.class)
                .withMessage("Timed out waiting for " + description)
                .until(condition);
    }

    protected Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    protected void scrollIntoView(WebElement element) {
        js("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
    }

    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ header widgets

    /** Number shown in the header cart badge; 0 when the badge is absent. */
    public int headerCartCount() {
        return tryFind("header.cartCount", Duration.ofSeconds(1))
                .map(el -> parseLeadingInt(el.getText()))
                .orElse(0);
    }

    public void waitForHeaderCartCount(int expected) {
        waitUntil(d -> headerCartCount() == expected, "header cart count to become " + expected);
    }

    public boolean isHeaderCartIconVisible() {
        return isVisible("header.cartIcon", Duration.ofSeconds(5));
    }

    public boolean isHeaderSearchVisible() {
        return isVisible("header.searchIcon", Duration.ofSeconds(5));
    }

    protected static int parseLeadingInt(String text) {
        String digits = text.trim().replaceAll("(?s)^[^0-9]*([0-9]+).*$", "$1");
        return digits.matches("[0-9]+") ? Integer.parseInt(digits) : 0;
    }
}
