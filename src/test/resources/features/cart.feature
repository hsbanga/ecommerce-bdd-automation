@cart
Feature: Shopping cart
  As a shopper
  I want to review and change the items in my cart
  So that my order is correct before checkout

  Background:
    Given I open the "inStock" product
    And I select the configured variant
    And I set the quantity to 2
    And I add the product to the cart
    And I should see the added to cart confirmation

  @smoke
  Scenario: Cart page shows the added line item and totals
    When I open the cart page
    Then the cart should contain the product with quantity 2
    And the cart subtotal should match the line item total
    And the cart badge should show 2 items

  @regression
  Scenario: Update the quantity of a line item
    When I open the cart page
    And I change the quantity of the product to 3
    Then the cart should contain the product with quantity 3
    And the cart badge should show 3 items

  @regression
  Scenario: Remove a line item from the cart
    When I open the cart page
    And I remove the product from the cart
    Then the cart should be empty
    And the cart badge should show 0 items

  @regression @shopify
  Scenario: Cart contents are reflected by the storefront cart API
    When I open the cart page
    Then the cart API should report 2 items
    When I change the quantity of the product to 1
    Then the cart API should report 1 item
