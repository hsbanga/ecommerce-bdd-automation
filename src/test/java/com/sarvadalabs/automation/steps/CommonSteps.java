package com.sarvadalabs.automation.steps;

import com.sarvadalabs.automation.config.ConfigManager;
import com.sarvadalabs.automation.config.TestData;
import com.sarvadalabs.automation.context.ScenarioContext;
import com.sarvadalabs.automation.core.Locators;
import com.sarvadalabs.automation.pages.CartPage;
import com.sarvadalabs.automation.pages.CollectionPage;
import com.sarvadalabs.automation.pages.HomePage;
import com.sarvadalabs.automation.pages.ProductPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {

    private final ScenarioContext context;

    public CommonSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("I am on the home page")
    public void iAmOnTheHomePage() {
        HomePage home = context.page(HomePage.class).open();
        assertThat(home.isLogoVisible()).as("store logo/heading visible").isTrue();
    }

    @Then("the page title should contain the store name")
    public void thePageTitleShouldContainTheStoreName() {
        assertThat(context.page(HomePage.class).pageTitle())
                .containsIgnoringCase(ConfigManager.get("store.name"));
    }

    @Then("the header should show the cart icon")
    public void theHeaderShouldShowTheCartIcon() {
        assertThat(context.page(HomePage.class).isHeaderCartIconVisible()).as("header cart icon").isTrue();
    }

    /** Some themes have no header search widget and expose search only through a URL. */
    @Then("product search should be available")
    public void productSearchShouldBeAvailable() {
        HomePage home = context.page(HomePage.class);
        boolean viaHeader = home.isHeaderSearchVisible();
        boolean viaUrl = !ConfigManager.get("path.search", "").isBlank();
        assertThat(viaHeader || viaUrl).as("search icon in header or path.search configured").isTrue();
    }

    /**
     * Checks the header cart badge. Stores that show no badge (header.cartCount left empty in the
     * store profile) are checked through the cart page instead, which sums the line quantities.
     */
    @Then("the cart badge should show {int} item(s)")
    public void theCartBadgeShouldShowItems(int expected) {
        if (!Locators.isDefined("header.cartCount")) {
            CartPage cart = context.page(CartPage.class).open();
            assertThat(cart.totalQuantity()).as("total quantity on the cart page (store shows no header badge)")
                    .isEqualTo(expected);
            return;
        }
        context.page(HomePage.class).waitForHeaderCartCount(expected);
        assertThat(context.page(HomePage.class).headerCartCount()).isEqualTo(expected);
    }

    @When("I open the {string} collection")
    public void iOpenTheCollection(String dataKey) {
        String handle = TestData.get("collections." + dataKey);
        context.page(CollectionPage.class).open(handle);
    }

    @Then("I should see at least {int} product(s) in the collection")
    public void iShouldSeeAtLeastProductsInTheCollection(int minimum) {
        CollectionPage collection = context.page(CollectionPage.class);
        assertThat(collection.productCount()).isGreaterThanOrEqualTo(minimum);
        assertThat(collection.productTitles()).as("product titles").isNotEmpty();
    }

    @When("I open the first product in the collection")
    public void iOpenTheFirstProductInTheCollection() {
        context.page(CollectionPage.class).openFirstProduct();
    }

    @Then("the product page should be displayed")
    public void theProductPageShouldBeDisplayed() {
        ProductPage product = context.page(ProductPage.class);
        assertThat(product.isLoaded()).as("product page loaded at %s", product.currentUrl()).isTrue();
        assertThat(product.title()).isNotBlank();
    }
}
