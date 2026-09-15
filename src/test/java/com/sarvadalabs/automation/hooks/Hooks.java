package com.sarvadalabs.automation.hooks;

import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.context.ScenarioContext;
import com.sarvadalabs.automation.driver.DriverFactory;
import com.sarvadalabs.automation.driver.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Browser lifecycle per scenario plus screenshot capture on failure (and optionally every step). */
public class Hooks {

    private static final Logger LOG = LogManager.getLogger(Hooks.class);
    private static final Path SCREENSHOT_DIR = Path.of("target", "screenshots");

    private final ScenarioContext context;

    public Hooks(ScenarioContext context) {
        this.context = context;
    }

    @Before(order = 0)
    public void startBrowser(Scenario scenario) {
        LOG.info("==== Scenario: {} {}", scenario.getName(), scenario.getSourceTagNames());
        WebDriver driver = DriverFactory.create();
        DriverManager.set(driver);
        context.setScenario(scenario);
    }

    @AfterStep
    public void afterStep(Scenario scenario) {
        if (ConfigManager.getBoolean("screenshot.everyStep", false) && DriverManager.isActive()) {
            attachScreenshot(scenario, "step");
        }
    }

    @After(order = 0)
    public void stopBrowser(Scenario scenario) {
        try {
            if (DriverManager.isActive()) {
                if (scenario.isFailed()) {
                    LOG.error("Scenario FAILED: {} (url: {})", scenario.getName(), DriverManager.get().getCurrentUrl());
                    attachScreenshot(scenario, "failure");
                }
                LOG.info("==== Finished: {} -> {}", scenario.getName(), scenario.getStatus());
            }
        } finally {
            DriverManager.quit();
        }
    }

    private void attachScreenshot(Scenario scenario, String label) {
        try {
            byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
            String safeName = scenario.getName().replaceAll("[^A-Za-z0-9._-]+", "_");
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
            scenario.attach(png, "image/png", label + "-" + safeName);
            Files.createDirectories(SCREENSHOT_DIR);
            Files.write(SCREENSHOT_DIR.resolve(safeName + "-" + label + "-" + stamp + ".png"), png);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not capture screenshot: {}", e.getMessage());
        }
    }
}
