package com.sarvadalabs.automation.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * JUnit 5 suite that runs every feature under src/test/resources/features.
 * Remaining Cucumber options live in junit-platform.properties and can be overridden with -D.
 *
 * <pre>
 *   mvn test                                        # everything, default store, headed Chrome
 *   mvn test -Dcucumber.filter.tags="@smoke"        # only smoke scenarios
 *   mvn test -Dstore=my-client -Dheadless=true      # another store profile, headless
 * </pre>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.sarvadalabs.automation")
public class TestRunner {
}
