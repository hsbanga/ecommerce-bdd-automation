package com.sarvadalabs.automation.context;

import com.sarvadalabs.automation.core.BasePage;
import com.sarvadalabs.automation.driver.DriverManager;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-scenario state shared between step definition classes. PicoContainer creates one instance
 * per scenario and injects it into every step class constructor that declares it.
 */
public class ScenarioContext {

    public static final String CURRENT_PRODUCT_KEY = "currentProductKey";
    public static final String EXPECTED_CART_QUANTITY = "expectedCartQuantity";

    private final Map<String, Object> data = new HashMap<>();
    private final Map<Class<?>, BasePage> pages = new HashMap<>();
    private Scenario scenario;

    public WebDriver driver() {
        return DriverManager.get();
    }

    /** Lazily creates and caches one page object per type for the lifetime of the scenario. */
    @SuppressWarnings("unchecked")
    public <T extends BasePage> T page(Class<T> type) {
        return (T) pages.computeIfAbsent(type, t -> {
            try {
                return (BasePage) t.getConstructor(WebDriver.class).newInstance(driver());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException("Cannot instantiate page " + t.getSimpleName(), e);
            }
        });
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            throw new IllegalStateException("Nothing stored in scenario context under '" + key + "'");
        }
        return type.cast(value);
    }

    public <T> T getOrDefault(String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        T value = (T) data.get(key);
        return value == null ? defaultValue : value;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public Scenario scenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }
}
