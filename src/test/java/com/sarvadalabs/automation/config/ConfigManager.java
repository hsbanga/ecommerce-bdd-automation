package com.sarvadalabs.automation.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Central configuration access. Resolution order for every key (highest priority first):
 * <ol>
 *   <li>JVM system property  (-Dkey=value)</li>
 *   <li>Environment variable (KEY_WITH_UNDERSCORES, e.g. BASE_URL)</li>
 *   <li>stores/&lt;store&gt;/store.properties</li>
 *   <li>config/default.properties</li>
 * </ol>
 * The active store profile itself is chosen from -Dstore, then env STORE, then default.properties.
 */
public final class ConfigManager {

    private static final Logger LOG = LogManager.getLogger(ConfigManager.class);
    private static final Properties PROPS = new Properties();
    private static final String STORE;

    static {
        loadInto(PROPS, "config/default.properties", true);
        STORE = firstNonBlank(System.getProperty("store"), System.getenv("STORE"), PROPS.getProperty("store"), "shopify-dawn");
        PROPS.setProperty("store", STORE);
        loadInto(PROPS, "stores/" + STORE + "/store.properties", true);
        LOG.info("Loaded store profile '{}' targeting {}", STORE, baseUrl());
    }

    private ConfigManager() {
    }

    public static String store() {
        return STORE;
    }

    public static String baseUrl() {
        String url = get("base.url");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String platform() {
        return get("store.platform", "generic").toLowerCase();
    }

    /** Returns the value for {@code key} or throws when it is not configured anywhere. */
    public static String get(String key) {
        String value = lookup(key);
        if (value == null) {
            throw new IllegalStateException("Missing required configuration key '" + key + "'");
        }
        return value;
    }

    public static String get(String key, String defaultValue) {
        String value = lookup(key);
        return value == null ? defaultValue : value;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = lookup(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    public static int getInt(String key, int defaultValue) {
        String value = lookup(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    /** Resolves a path template such as {@code path.product} with {0}, {1}... replaced by {@code args}. */
    public static String path(String key, String... args) {
        String template = get(key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", args[i]);
        }
        return template;
    }

    /**
     * The fixed part of a path template before its first placeholder, without the leading slash,
     * e.g. {@code path.product=/products/{0}} gives {@code products/}. Useful for URL assertions
     * that must work for any platform's URL scheme.
     */
    public static String pathPrefix(String key) {
        String template = get(key);
        int placeholder = template.indexOf('{');
        String prefix = placeholder >= 0 ? template.substring(0, placeholder) : template;
        return prefix.startsWith("/") ? prefix.substring(1) : prefix;
    }

    private static String lookup(String key) {
        String value = firstNonBlank(System.getProperty(key), System.getenv(toEnvKey(key)), PROPS.getProperty(key));
        return value == null ? null : value.trim();
    }

    /** Loads a classpath properties resource into {@code target}; missing optional resources are ignored. */
    public static void loadInto(Properties target, String resource, boolean required) {
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Required classpath resource not found: " + resource);
                }
                LOG.debug("Optional resource {} not present, skipping", resource);
                return;
            }
            target.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            LOG.debug("Loaded {}", resource);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + resource, e);
        }
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase().replace('.', '_');
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
