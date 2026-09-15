package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.core.BasePage;
import com.sarvadalabs.automation.core.Locators;
import com.sarvadalabs.automation.util.Money;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class CartPage extends BasePage {

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CartPage open() {
        openPath(ConfigManager.path("path.cart"));
        return this;
    }

    public int lineItemCount() {
        waitForCartIdle();
        return findAll("cart.items", Duration.ofSeconds(5)).size();
    }

    public boolean isEmpty() {
        waitForCartIdle();
        return isVisible("cart.emptyMessage", Duration.ofSeconds(10));
    }

    public void waitForEmpty() {
        waitUntil(d -> isVisible("cart.emptyMessage", Duration.ofSeconds(1)), Duration.ofSeconds(20), "cart to become empty");
    }

    /** Quantity of the line item whose name contains {@code productName}; 0 when absent. */
    public int quantityOf(String productName) {
        waitForCartIdle();
        return lineItem(productName)
                .flatMap(item -> findWithin(item, "cart.itemQuantityInput"))
                .map(input -> parseLeadingInt(input.getAttribute("value")))
                .orElse(0);
    }

    public void waitForQuantity(String productName, int expected) {
        waitUntil(d -> quantityOf(productName) == expected, Duration.ofSeconds(20),
                "quantity of '" + productName + "' to become " + expected);
    }

    public BigDecimal lineTotalOf(String productName) {
        WebElement item = lineItem(productName)
                .orElseThrow(() -> new IllegalStateException("'" + productName + "' is not in the cart"));
        WebElement total = findWithin(item, "cart.itemLineTotal")
                .orElseThrow(() -> new IllegalStateException("No line total shown for '" + productName + "'"));
        return Money.parse(total.getText());
    }

    public BigDecimal subtotal() {
        return Money.parse(text("cart.subtotal"));
    }

    public void changeQuantity(String productName, int quantity) {
        log.info("Changing quantity of '{}' to {}", productName, quantity);
        WebElement item = lineItem(productName)
                .orElseThrow(() -> new IllegalStateException("'" + productName + "' is not in the cart"));
        WebElement input = findWithin(item, "cart.itemQuantityInput")
                .orElseThrow(() -> new IllegalStateException("No quantity input for '" + productName + "'"));
        replaceValue(input, String.valueOf(quantity));
        input.sendKeys(Keys.TAB); // fires the change event Shopify themes listen for
        if (Locators.isDefined("cart.updateButton")) {
            // WooCommerce-style carts apply quantity changes only after "Update cart"
            tryFind("cart.updateButton", Duration.ofSeconds(3)).ifPresent(button -> {
                waitUntil(d -> button.isEnabled(), Duration.ofSeconds(5), "update cart button to become enabled");
                click(button, "cart.updateButton");
            });
        }
        waitForCartRefresh(input);
        waitForQuantity(productName, quantity);
    }

    /**
     * After a change the theme either re-renders the cart section (the old input goes stale) or
     * reloads the page. Wait for that to happen so later checks read server-side state, not the
     * value that was just typed.
     */
    private void waitForCartRefresh(WebElement changedInput) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                changedInput.getAttribute("value");
            } catch (StaleElementReferenceException e) {
                waitForCartIdle();
                return;
            }
            if (isVisible("cart.busyOverlay", Duration.ZERO)) {
                waitForCartIdle();
                return;
            }
            sleep(200);
        }
        log.warn("Cart did not visibly refresh after the quantity change; continuing");
    }

    public void removeItem(String productName) {
        log.info("Removing '{}' from the cart", productName);
        WebElement item = lineItem(productName)
                .orElseThrow(() -> new IllegalStateException("'" + productName + "' is not in the cart"));
        WebElement remove = findWithin(item, "cart.itemRemove")
                .orElseThrow(() -> new IllegalStateException("No remove control for '" + productName + "'"));
        click(remove, "remove " + productName);
        if (Locators.isDefined("cart.removeConfirm")) {
            // Some themes ask "Are you sure?" in a dialog before removing
            tryFind("cart.removeConfirm", Duration.ofSeconds(5)).ifPresent(confirm -> click(confirm, "cart.removeConfirm"));
        }
        waitUntil(d -> lineItem(productName).isEmpty(), Duration.ofSeconds(20), "'" + productName + "' to disappear from the cart");
    }

    /** Sum of all line item quantities on the cart page (0 when the cart is empty). */
    public int totalQuantity() {
        waitForCartIdle();
        if (isVisible("cart.emptyMessage", Duration.ofSeconds(2))) {
            return 0;
        }
        int total = 0;
        for (WebElement item : findAll("cart.items", Duration.ofSeconds(5))) {
            total += findWithin(item, "cart.itemQuantityInput")
                    .map(input -> parseLeadingInt(input.getAttribute("value")))
                    .orElse(1);
        }
        return total;
    }

    /**
     * Clicks through to checkout. Some themes intercept the click with an authentication popup
     * instead of navigating; in that case the checkout URL is opened directly (guest checkout).
     */
    public CheckoutPage proceedToCheckout() {
        log.info("Proceeding to checkout");
        String checkoutPrefix = ConfigManager.pathPrefix("path.checkout");
        String cartPrefix = ConfigManager.pathPrefix("path.cart");
        click("cart.checkoutButton");
        try {
            waitUntil(d -> {
                String url = d.getCurrentUrl();
                return url.contains(checkoutPrefix) && !url.contains(cartPrefix);
            }, Duration.ofSeconds(10), "navigation from cart to checkout");
        } catch (TimeoutException e) {
            log.warn("Checkout button did not navigate (auth popup?); opening the checkout URL directly");
            openPath(ConfigManager.path("path.checkout"));
        }
        return new CheckoutPage(driver);
    }

    private Optional<WebElement> lineItem(String productName) {
        List<WebElement> matches = findAll("cart.itemByName", Duration.ofSeconds(2), productName);
        return matches.stream().findFirst();
    }

    /** Shopify disables the cart table while an AJAX update is in flight. */
    private void waitForCartIdle() {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (isVisible("cart.busyOverlay", Duration.ZERO) && System.nanoTime() < deadline) {
            sleep(200);
        }
    }
}
