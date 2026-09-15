package com.sarvadalabs.automation.steps;

import com.sarvadalabs.automation.config.TestData;
import com.sarvadalabs.automation.context.ScenarioContext;
import com.sarvadalabs.automation.pages.ProductPage;
import com.sarvadalabs.automation.util.Money;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductSteps {

    private final ScenarioContext context;

    public ProductSteps(ScenarioContext context) {
        this.context = context;
    }

    /** Resolves the product's test-data prefix, e.g. products.inStock */
    private String data(String field) {
        return TestData.get("products." + context.get(ScenarioContext.CURRENT_PRODUCT_KEY, String.class) + "." + field);
    }

    @Given("I open the {string} product")
    public void iOpenTheProduct(String dataKey) {
        context.set(ScenarioContext.CURRENT_PRODUCT_KEY, dataKey);
        context.page(ProductPage.class).open(data("handle"));
    }

    @Then("the product title should match the expected name")
    public void theProductTitleShouldMatchTheExpectedName() {
        assertThat(context.page(ProductPage.class).title()).isEqualToIgnoringCase(data("name"));
    }

    @Then("the product price should be displayed")
    public void theProductPriceShouldBeDisplayed() {
        String priceText = context.page(ProductPage.class).priceText();
        BigDecimal price = Money.parse(priceText);
        assertThat(price).as("price parsed from '%s'", priceText).isPositive();
        String key = "products." + context.get(ScenarioContext.CURRENT_PRODUCT_KEY, String.class) + ".price";
        if (TestData.has(key)) {
            assertThat(price).isEqualByComparingTo(new BigDecimal(TestData.get(key)));
        }
    }

    @Then("the add to cart button should be {word}")
    public void theAddToCartButtonShouldBe(String state) {
        boolean enabled = context.page(ProductPage.class).isAddToCartEnabled();
        switch (state.toLowerCase()) {
            case "enabled" -> assertThat(enabled).as("add to cart enabled").isTrue();
            case "disabled" -> assertThat(enabled).as("add to cart enabled").isFalse();
            default -> throw new IllegalArgumentException("Expected 'enabled' or 'disabled' but got '" + state + "'");
        }
    }

    @When("I select the configured variant")
    public void iSelectTheConfiguredVariant() {
        String key = "products." + context.get(ScenarioContext.CURRENT_PRODUCT_KEY, String.class) + ".variant";
        if (TestData.has(key)) {
            context.page(ProductPage.class).selectVariant(TestData.get(key));
        }
    }

    @When("I set the quantity to {int}")
    public void iSetTheQuantityTo(int quantity) {
        context.page(ProductPage.class).setQuantity(quantity);
        context.set(ScenarioContext.EXPECTED_CART_QUANTITY, quantity);
    }

    @When("I add the product to the cart")
    public void iAddTheProductToTheCart() {
        if (!context.has(ScenarioContext.EXPECTED_CART_QUANTITY)) {
            context.set(ScenarioContext.EXPECTED_CART_QUANTITY, 1);
        }
        context.page(ProductPage.class).addToCart();
    }

    @Then("I should see the added to cart confirmation")
    public void iShouldSeeTheAddedToCartConfirmation() {
        ProductPage product = context.page(ProductPage.class);
        assertThat(product.isAddedConfirmationShown()).as("added-to-cart notification").isTrue();
        assertThat(product.addedConfirmationText()).containsIgnoringCase("added");
    }
}
