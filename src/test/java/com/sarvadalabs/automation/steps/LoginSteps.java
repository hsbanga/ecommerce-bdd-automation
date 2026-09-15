package com.sarvadalabs.automation.steps;

import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.config.TestData;
import com.sarvadalabs.automation.context.ScenarioContext;
import com.sarvadalabs.automation.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opentest4j.TestAbortedException;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginSteps {

    private final ScenarioContext context;

    public LoginSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("I open the login page")
    public void iOpenTheLoginPage() {
        context.page(LoginPage.class).open();
    }

    @Then("the login form should be displayed")
    public void theLoginFormShouldBeDisplayed() {
        assertThat(context.page(LoginPage.class).isFormDisplayed()).as("login form").isTrue();
    }

    /**
     * "invalid" credentials come from testdata.json; "valid" credentials come from configuration
     * (env LOGIN_EMAIL / LOGIN_PASSWORD or store.properties). The scenario is skipped when the
     * store profile has no valid account configured.
     */
    @When("I log in with the {string} credentials")
    public void iLogInWithTheCredentials(String kind) {
        String email;
        String password;
        if ("valid".equalsIgnoreCase(kind)) {
            email = ConfigManager.get("login.email", "");
            password = ConfigManager.get("login.password", "");
            if (email.isBlank() || password.isBlank()) {
                throw new TestAbortedException("No valid customer account configured (set LOGIN_EMAIL / LOGIN_PASSWORD)");
            }
        } else {
            email = TestData.get("login." + kind + ".email");
            password = TestData.get("login." + kind + ".password");
        }
        context.page(LoginPage.class).login(email, password);
    }

    @Then("I should see a login error message")
    public void iShouldSeeALoginErrorMessage() {
        LoginPage login = context.page(LoginPage.class);
        String error = login.errorMessage();
        if (error.isBlank() && login.isCaptchaPresent()) {
            throw new TestAbortedException(
                    "The store protects login with a captcha, so the invalid-login error cannot be verified automatically");
        }
        assertThat(error).as("login error message").isNotBlank();
        assertThat(error.toLowerCase()).containsAnyOf("incorrect", "invalid", "error", "not found", "unknown",
                "not registered", "wrong", "failed", "no account", "does not match");
    }

    @Then("I should be on the account page")
    public void iShouldBeOnTheAccountPage() {
        assertThat(context.page(LoginPage.class).isOnAccountPage()).as("redirected to account page").isTrue();
    }
}
