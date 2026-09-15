package com.sarvadalabs.automation.pages;

import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.core.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.Optional;

public class ProductPage extends BasePage {

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    public ProductPage open(String handle) {
        openPath(ConfigManager.path("path.product", handle));
        return this;
    }

    public boolean isLoaded() {
        return currentUrl().contains("/products/") && isVisible("product.title", Duration.ofSeconds(10));
    }

    public String title() {
        return text("product.title");
    }

    public String priceText() {
        return text("product.price");
    }

    public boolean isAddToCartEnabled() {
        WebElement button = find("product.addToCart");
        return button.isEnabled() && button.getAttribute("disabled") == null;
    }

    /** Selects a variant option by its displayed value, supporting radio swatches and select lists. */
    public void selectVariant(String optionValue) {
        log.info("Selecting variant '{}'", optionValue);
        Optional<WebElement> label = tryFind("product.variantRadioLabel", Duration.ofSeconds(3), optionValue);
        if (label.isPresent()) {
            click(label.get(), "variant label " + optionValue);
            waitUntil(d -> {
                Optional<WebElement> radio = tryFindAny("product.variantRadio", optionValue);
                return radio.map(WebElement::isSelected).orElse(true);
            }, "variant '" + optionValue + "' to be selected");
            waitForVariantRender();
            return;
        }
        for (WebElement select : findAll("product.variantSelect", Duration.ofSeconds(3))) {
            Select dropdown = new Select(select);
            boolean hasOption = dropdown.getOptions().stream().anyMatch(o -> o.getText().trim().equalsIgnoreCase(optionValue));
            if (hasOption) {
                dropdown.selectByVisibleText(optionValue);
                waitForVariantRender();
                return;
            }
        }
        throw new IllegalStateException("Variant option '" + optionValue + "' not found on " + currentUrl());
    }

    /**
     * Shopify themes re-render the product form after a variant change (button disabled, section
     * fetched, DOM replaced). Wait until the add-to-cart button is enabled and stable so later
     * interactions do not hit elements that are about to be replaced.
     */
    private void waitForVariantRender() {
        sleep(300);
        waitUntil(d -> isAddToCartEnabled(), Duration.ofSeconds(15), "product form to finish re-rendering");
        waitUntil(d -> {
            WebElement before = find("product.addToCart");
            sleep(500);
            WebElement after = find("product.addToCart");
            return before.equals(after) && isAddToCartEnabled();
        }, Duration.ofSeconds(15), "product form DOM to become stable");
    }

    /** Hidden radio inputs are never "displayed", so look them up without the visibility filter. */
    private Optional<WebElement> tryFindAny(String key, String... args) {
        return findAll(key, Duration.ofSeconds(1), args).stream().findFirst();
    }

    public void setQuantity(int quantity) {
        log.info("Setting quantity to {}", quantity);
        WebElement input = find("product.quantityInput");
        replaceValue(input, String.valueOf(quantity));
        if (!String.valueOf(quantity).equals(input.getAttribute("value"))) {
            log.warn("Typed quantity was not accepted, setting it through JS");
            js("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                    input, String.valueOf(quantity));
        }
    }

    public void addToCart() {
        log.info("Adding '{}' to cart", title());
        click("product.addToCart");
    }

    public boolean isAddedConfirmationShown() {
        return isVisible("product.addedNotification", Duration.ofSeconds(15));
    }

    public String addedConfirmationText() {
        return text("product.addedNotificationHeading");
    }
}
