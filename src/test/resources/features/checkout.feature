@checkout
Feature: Checkout
  As a shopper
  I want to move from my cart into checkout and enter my details
  So that I can complete my purchase

  # The framework stops before payment: no order is ever placed.

  @regression
  Scenario: Proceed from the cart to checkout and enter contact and delivery details
    Given I open the "inStock" product
    And I select the configured variant
    And I add the product to the cart
    And I should see the added to cart confirmation
    When I open the cart page
    And I proceed to checkout
    Then the checkout page should be displayed
    When I enter the configured contact and shipping details
    Then the checkout email field should contain the configured email
    And the checkout firstName field should contain the configured value
    And the checkout lastName field should contain the configured value
