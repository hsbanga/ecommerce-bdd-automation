package com.sarvadalabs.automation.core;

import com.sarvadalabs.automation.config.ConfigManager;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Locator repository. Baseline locators live in locators/default.properties; each store
 * profile may override any key in stores/&lt;store&gt;/locators.properties.
 *
 * <p>Value syntax: {@code strategy=value || strategy=value ...} where strategy is one of
 * css (default), xpath, id, name, linkText, partialLinkText, class, tag. The {@code ||}
 * separated alternatives are tried in order until one matches a visible element.
 * Placeholders {@code {0}}, {@code {1}}... are substituted with runtime arguments.
 */
public final class Locators {

    private static final Properties LOCATORS = new Properties();

    static {
        ConfigManager.loadInto(LOCATORS, "locators/default.properties", true);
        ConfigManager.loadInto(LOCATORS, "stores/" + ConfigManager.store() + "/locators.properties", false);
    }

    private Locators() {
    }

    public static boolean isDefined(String key) {
        String raw = LOCATORS.getProperty(key);
        return raw != null && !raw.isBlank();
    }

    public static List<By> of(String key, String... args) {
        String raw = LOCATORS.getProperty(key);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("No locator defined for key '" + key + "' (store '" + ConfigManager.store() + "')");
        }
        List<By> result = new ArrayList<>();
        for (String candidate : raw.split("\\|\\|")) {
            String expression = candidate.trim();
            if (expression.isEmpty()) {
                continue;
            }
            for (int i = 0; i < args.length; i++) {
                expression = expression.replace("{" + i + "}", args[i]);
            }
            result.add(parse(expression));
        }
        return result;
    }

    static By parse(String expression) {
        int eq = expression.indexOf('=');
        String strategy = eq > 0 ? expression.substring(0, eq).trim() : "";
        String value = eq > 0 ? expression.substring(eq + 1).trim() : expression;
        return switch (strategy) {
            case "css" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "linkText" -> By.linkText(value);
            case "partialLinkText" -> By.partialLinkText(value);
            case "class" -> By.className(value);
            case "tag" -> By.tagName(value);
            default -> expression.startsWith("/") || expression.startsWith("(")
                    ? By.xpath(expression)
                    : By.cssSelector(expression);
        };
    }
}
