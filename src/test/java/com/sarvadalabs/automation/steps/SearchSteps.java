package com.sarvadalabs.automation.steps;

import com.sarvadalabs.automation.config.TestData;
import com.sarvadalabs.automation.context.ScenarioContext;
import com.sarvadalabs.automation.pages.HomePage;
import com.sarvadalabs.automation.pages.SearchResultsPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchSteps {

    private static final String SEARCH_TERM = "searchTerm";

    private final ScenarioContext context;

    public SearchSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I search for {string}")
    public void iSearchFor(String term) {
        context.set(SEARCH_TERM, term);
        context.page(HomePage.class).searchFor(term);
    }

    @When("I search for the configured {string} term")
    public void iSearchForTheConfiguredTerm(String dataKey) {
        iSearchFor(TestData.get("search." + dataKey));
    }

    @Then("I should see search results")
    public void iShouldSeeSearchResults() {
        SearchResultsPage results = context.page(SearchResultsPage.class);
        assertThat(results.productCount()).as("number of search results").isGreaterThan(0);
        assertThat(results.productTitles()).as("result titles").isNotEmpty();
    }

    @Then("the search results page should reference the search term")
    public void theSearchResultsPageShouldReferenceTheSearchTerm() {
        String term = context.get(SEARCH_TERM, String.class);
        SearchResultsPage results = context.page(SearchResultsPage.class);
        assertThat(results.pageTitle() + " " + results.currentUrl()).containsIgnoringCase(term);
    }

    @Then("I should see a no results message")
    public void iShouldSeeANoResultsMessage() {
        SearchResultsPage results = context.page(SearchResultsPage.class);
        assertThat(results.hasNoResultsMessage()).as("no-results message shown").isTrue();
        assertThat(results.productCount()).isZero();
    }

    @When("I open the first search result")
    public void iOpenTheFirstSearchResult() {
        context.page(SearchResultsPage.class).openFirstProduct();
    }
}
