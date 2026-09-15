@account
Feature: Customer login
  As a returning customer
  I want to sign in to my account
  So that I can check out faster and see my orders

  @smoke
  Scenario: Login page shows the sign-in form
    Given I open the login page
    Then the login form should be displayed

  @regression
  Scenario: Signing in with invalid credentials shows an error
    Given I open the login page
    When I log in with the "invalid" credentials
    Then I should see a login error message

  # Skipped automatically unless LOGIN_EMAIL / LOGIN_PASSWORD are configured for the store.
  @regression @requires-account
  Scenario: Signing in with valid credentials opens the account page
    Given I open the login page
    When I log in with the "valid" credentials
    Then I should be on the account page
