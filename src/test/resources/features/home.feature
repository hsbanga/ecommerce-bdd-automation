@home
Feature: Storefront home page
  As a shopper
  I want the storefront to load with its core navigation
  So that I can start browsing products

  @smoke
  Scenario: Home page loads with header navigation
    Given I am on the home page
    Then the page title should contain the store name
    And the header should show the cart icon
    And product search should be available
    And the cart badge should show 0 items

  @regression
  Scenario: Browse a collection and open a product
    Given I am on the home page
    When I open the "all" collection
    Then I should see at least 1 product in the collection
    When I open the first product in the collection
    Then the product page should be displayed
