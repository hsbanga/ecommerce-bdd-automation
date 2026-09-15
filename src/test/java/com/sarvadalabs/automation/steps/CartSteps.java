package com.sarvadalabs.automation.steps;

import com.sarvadalabs.automation.api.ShopifyCartApi;
import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.config.TestData;
import com.sarvadalabs.automation.context.ScenarioContext;
import com.sarvadalabs.automation.pages.CartPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class CartSteps {

    private final ScenarioContext context;

    public CartSteps(ScenarioContext context) {
        this.context = context;
    }

    private String currentProductName() {
        return TestData.get("products." + context.get(ScenarioContext.CURRENT_PRODUCT_KEY, String.class) + ".name");
    }

    @When("I open the cart page")
    public void iOpenTheCartPage() {
        context.page(CartPage.class).open();
    }

    @Then("the cart should contain the product with quantity {int}")
    public void theCartShouldContainTheProductWithQuantity(int quantity) {
        CartPage cart = context.page(CartPage.class);
        cart.waitForQuantity(currentProductName(), quantity);
        assertThat(cart.quantityOf(currentProductName())).isEqualTo(quantity);
    }

    @Then("the cart subtotal should match the line item total")
    public void theCartSubtotalShouldMatchTheLineItemTotal() {
        CartPage cart = context.page(CartPage.class);
        BigDecimal lineTotal = cart.lineTotalOf(currentProductName());
        assertThat(cart.subtotal()).as("cart subtotal").isEqualByComparingTo(lineTotal);
    }

    @When("I change the quantity of the product to {int}")
    public void iChangeTheQuantityOfTheProductTo(int quantity) {
        context.page(CartPage.class).changeQuantity(currentProductName(), quantity);
        context.set(ScenarioContext.EXPECTED_CART_QUANTITY, quantity);
    }

    @When("I remove the product from the cart")
    public void iRemoveTheProductFromTheCart() {
        context.page(CartPage.class).removeItem(currentProductName());
    }

    @Then("the cart should be empty")
    public void theCartShouldBeEmpty() {
        CartPage cart = context.page(CartPage.class);
        cart.waitForEmpty();
        assertThat(cart.isEmpty()).as("empty-cart message").isTrue();
        assertThat(cart.lineItemCount()).isZero();
    }

    @Then("the cart API should report {int} item(s)")
    public void theCartApiShouldReportItems(int expected) {
        assertThat(ConfigManager.platform())
                .as("the storefront cart API step only applies to Shopify stores")
                .isEqualTo("shopify");
        ShopifyCartApi api = new ShopifyCartApi(context.driver());
        // The storefront may still be persisting the last UI change; give it a few seconds.
        int actual = api.itemCount();
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (actual != expected && System.nanoTime() < deadline) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            actual = api.itemCount();
        }
        assertThat(actual).as("item_count from /cart.js").isEqualTo(expected);
    }

    @When("I proceed to checkout")
    public void iProceedToCheckout() {
        context.page(CartPage.class).proceedToCheckout();
    }
}
