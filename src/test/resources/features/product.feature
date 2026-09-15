@product
Feature: Product details page
  As a shopper
  I want to view product details and add items to my cart
  So that I can buy what I need

  @regression
  Scenario: Product page shows title, price and an enabled add to cart button
    Given I open the "inStock" product
    Then the product title should match the expected name
    And the product price should be displayed
    And the add to cart button should be enabled

  @regression
  Scenario: A sold out product cannot be added to the cart
    Given I open the "soldOut" product
    Then the add to cart button should be disabled

  @smoke
  Scenario: Add a product to the cart from the product page
    Given I open the "inStock" product
    When I select the configured variant
    And I add the product to the cart
    Then I should see the added to cart confirmation
    And the cart badge should show 1 item
