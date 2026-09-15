package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.core.BasePage;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        openPath(ConfigManager.path("path.login"));
        return this;
    }

    public boolean isFormDisplayed() {
        return isVisible("login.email", Duration.ofSeconds(10))
                && isVisible("login.password", Duration.ofSeconds(5))
                && isVisible("login.submit", Duration.ofSeconds(5));
    }

    public void login(String email, String password) {
        log.info("Logging in as {}", email);
        type("login.email", email);
        type("login.password", password);
        click("login.submit");
        waitForPageReady();
    }

    public String errorMessage() {
        return tryFind("login.error", Duration.ofSeconds(10)).map(el -> el.getText().trim()).orElse("");
    }

    /** True when a captcha widget is on the page; such stores cannot be exercised automatically. */
    public boolean isCaptchaPresent() {
        return findAll("login.captcha", Duration.ofSeconds(1)).stream().findAny().isPresent();
    }

    public boolean isOnAccountPage() {
        String accountPath = ConfigManager.path("path.account");
        String loginPath = ConfigManager.path("path.login");
        waitUntil(d -> d.getCurrentUrl().contains(accountPath), Duration.ofSeconds(15), "account page");
        return !currentUrl().contains(loginPath);
    }
}
