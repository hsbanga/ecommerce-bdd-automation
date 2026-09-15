package com.sarvadalabs.automation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Read access to stores/&lt;store&gt;/testdata.json using dotted paths, e.g.
 * {@code TestData.get("products.inStock.handle")}.
 */
public final class TestData {

    private static final JsonNode ROOT = load();

    private TestData() {
    }

    private static JsonNode load() {
        String resource = "stores/" + ConfigManager.store() + "/testdata.json";
        try (InputStream in = TestData.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Test data file not found on classpath: " + resource);
            }
            return new ObjectMapper().readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse " + resource, e);
        }
    }

    /** Returns the text at {@code dottedPath}; throws if the path is missing. */
    public static String get(String dottedPath) {
        JsonNode node = node(dottedPath);
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("No test data at '" + dottedPath + "' for store '" + ConfigManager.store() + "'");
        }
        return node.asText();
    }

    /** Returns the text at {@code dottedPath} or {@code defaultValue} when absent. */
    public static String get(String dottedPath, String defaultValue) {
        JsonNode node = node(dottedPath);
        return node.isMissingNode() || node.isNull() ? defaultValue : node.asText();
    }

    public static boolean has(String dottedPath) {
        JsonNode node = node(dottedPath);
        return !node.isMissingNode() && !node.isNull() && !node.asText().isBlank();
    }

    public static JsonNode node(String dottedPath) {
        JsonNode current = ROOT;
        for (String segment : dottedPath.split("\\.")) {
            current = current.path(segment);
        }
        return current;
    }
}
