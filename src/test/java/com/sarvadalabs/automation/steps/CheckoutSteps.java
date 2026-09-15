package com.sarvadalabs.automation.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.sarvadalabs.automation.config.TestData;
import com.sarvadalabs.automation.context.ScenarioContext;
import com.sarvadalabs.automation.pages.CheckoutPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckoutSteps {

    private final ScenarioContext context;

    public CheckoutSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("the checkout page should be displayed")
    public void theCheckoutPageShouldBeDisplayed() {
        CheckoutPage checkout = context.page(CheckoutPage.class);
        assertThat(checkout.isLoaded()).as("checkout page loaded at %s", checkout.currentUrl()).isTrue();
    }

    @When("I enter the configured contact and shipping details")
    public void iEnterTheConfiguredContactAndShippingDetails() {
        CheckoutPage checkout = context.page(CheckoutPage.class);
        checkout.enterEmail(TestData.get("checkout.email"));

        Map<String, String> details = new HashMap<>();
        JsonNode node = TestData.node("checkout");
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String field = it.next();
            details.put(field, node.get(field).asText());
        }
        checkout.fillDeliveryDetails(details);
    }

    @Then("the checkout email field should contain the configured email")
    public void theCheckoutEmailFieldShouldContainTheConfiguredEmail() {
        String email = TestData.get("checkout.email");
        assertThat(context.page(CheckoutPage.class).showsEmail(email))
                .as("checkout shows the entered email '%s' (input value or collapsed summary)", email)
                .isTrue();
    }

    @Then("the checkout {word} field should contain the configured value")
    public void theCheckoutFieldShouldContainTheConfiguredValue(String field) {
        assertThat(context.page(CheckoutPage.class).valueOf("checkout." + field))
                .isEqualTo(TestData.get("checkout." + field));
    }
}
