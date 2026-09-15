package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.core.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Hosted checkout (Shopify's checkout is served under /checkouts/...). The framework fills
 * contact and delivery details only; it never enters payment data or places an order.
 */
public class CheckoutPage extends BasePage {

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return currentUrl().contains("checkout") && isVisible("checkout.email", Duration.ofSeconds(30));
    }

    public void enterEmail(String email) {
        WebElement input = find("checkout.email");
        replaceValue(input, email);
    }

    public String emailValue() {
        return find("checkout.email").getAttribute("value");
    }

    /**
     * Fills whichever delivery fields exist on the page. Keys: firstName, lastName, address1,
     * city, postalCode, phone (text inputs) and country, province (select lists by visible text).
     */
    public void fillDeliveryDetails(Map<String, String> details) {
        selectIfPresent("checkout.country", details.get("country"));
        sleep(1000); // country change re-renders the address form
        typeIfPresent("checkout.firstName", details.get("firstName"));
        typeIfPresent("checkout.lastName", details.get("lastName"));
        typeIfPresent("checkout.address1", details.get("address1"));
        typeIfPresent("checkout.city", details.get("city"));
        selectIfPresent("checkout.province", details.get("province"));
        typeIfPresent("checkout.postalCode", details.get("postalCode"));
        typeIfPresent("checkout.phone", details.get("phone"));
    }

    public String valueOf(String fieldKey) {
        return tryFind(fieldKey, Duration.ofSeconds(3)).map(el -> el.getAttribute("value")).orElse("");
    }

    private void typeIfPresent(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Optional<WebElement> field = tryFind(key, Duration.ofSeconds(3));
        if (field.isPresent()) {
            replaceValue(field.get(), value);
        } else {
            log.warn("Checkout field '{}' not present on this store, skipping", key);
        }
    }

    private void selectIfPresent(String key, String visibleText) {
        if (visibleText == null || visibleText.isBlank()) {
            return;
        }
        Optional<WebElement> field = tryFind(key, Duration.ofSeconds(3));
        if (field.isPresent()) {
            scrollIntoView(field.get());
            new Select(field.get()).selectByVisibleText(visibleText);
        } else {
            log.warn("Checkout select '{}' not present on this store, skipping", key);
        }
    }
}
