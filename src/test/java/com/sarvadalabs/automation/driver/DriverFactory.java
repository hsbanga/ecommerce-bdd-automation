package com.sarvadalabs.automation.driver;

import com.sarvadalabs.automation.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Creates WebDriver instances from configuration (browser, headless, grid.url, window size, timeouts). */
public final class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver create() {
        String browser = ConfigManager.get("browser", "chrome").toLowerCase();
        boolean headless = ConfigManager.getBoolean("headless", false);
        String gridUrl = ConfigManager.get("grid.url", "");
        int width = ConfigManager.getInt("window.width", 1920);
        int height = ConfigManager.getInt("window.height", 1080);

        MutableCapabilities options = switch (browser) {
            case "chrome" -> chromeOptions(headless, width, height);
            case "edge" -> edgeOptions(headless, width, height);
            case "firefox" -> firefoxOptions(headless, width, height);
            default -> throw new IllegalArgumentException("Unsupported browser '" + browser + "'. Use chrome, edge or firefox.");
        };

        WebDriver driver;
        if (!gridUrl.isBlank()) {
            LOG.info("Starting remote {} session on grid {} (headless={})", browser, gridUrl, headless);
            driver = new RemoteWebDriver(toUrl(gridUrl), options);
        } else {
            LOG.info("Starting local {} session (headless={})", browser, headless);
            driver = switch (browser) {
                case "chrome" -> new ChromeDriver((ChromeOptions) options);
                case "edge" -> new EdgeDriver((EdgeOptions) options);
                default -> new FirefoxDriver((FirefoxOptions) options);
            };
        }

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigManager.getInt("timeout.pageLoad", 60)));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(ConfigManager.getInt("timeout.script", 30)));
        // Explicit waits only; implicit waits interfere with the fallback-locator strategy.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        if (!headless) {
            driver.manage().window().maximize();
        }
        return driver;
    }

    private static ChromeOptions chromeOptions(boolean headless, int width, int height) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=" + width + "," + height,
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-notifications",
                "--disable-infobars",
                "--disable-blink-features=AutomationControlled",
                "--lang=en-US");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false));
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless, int width, int height) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=" + width + "," + height, "--disable-notifications", "--lang=en-US");
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless, int width, int height) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=" + width, "--height=" + height);
        options.addPreference("dom.webnotifications.enabled", false);
        return options;
    }

    private static URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid grid.url: " + url, e);
        }
    }
}
