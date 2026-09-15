@search
Feature: Product search
  As a shopper
  I want to search the catalogue by keyword
  So that I can quickly find products

  @smoke
  Scenario: Searching with a configured valid term returns results
    Given I am on the home page
    When I search for the configured "validTerm" term
    Then I should see search results
    And the search results page should reference the search term

  @regression
  Scenario Outline: Searching for "<term>" returns matching products
    Given I am on the home page
    When I search for "<term>"
    Then I should see search results

    Examples:
      | term   |
      | bag    |
      | wallet |

  @regression
  Scenario: Searching for a nonsense term shows a no-results message
    Given I am on the home page
    When I search for the configured "noResultsTerm" term
    Then I should see a no results message

  @regression
  Scenario: A search result opens the product page
    Given I am on the home page
    When I search for the configured "validTerm" term
    And I open the first search result
    Then the product page should be displayed
