package com.sarvadalabs.automation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Shopify AJAX Cart API (/cart.js, /cart/add.js, /cart/clear.js) executed inside the browser
 * session so it shares the shopper's cart cookie. Used to cross-check what the UI shows.
 */
public class ShopifyCartApi {

    private static final Logger LOG = LogManager.getLogger(ShopifyCartApi.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String FETCH_SCRIPT = """
            const callback = arguments[arguments.length - 1];
            const options = { method: arguments[0], headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' } };
            if (arguments[2]) { options.body = arguments[2]; }
            fetch(arguments[1], options)
                .then(r => r.text().then(t => callback(JSON.stringify({ status: r.status, body: t }))))
                .catch(e => callback(JSON.stringify({ status: 0, body: String(e) })));
            """;

    private final JavascriptExecutor js;

    public ShopifyCartApi(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
    }

    public JsonNode getCart() {
        return call("GET", "/cart.js", null);
    }

    public int itemCount() {
        return getCart().path("item_count").asInt();
    }

    public JsonNode add(long variantId, int quantity) {
        return call("POST", "/cart/add.js", "{\"items\":[{\"id\":" + variantId + ",\"quantity\":" + quantity + "}]}");
    }

    public JsonNode clear() {
        return call("POST", "/cart/clear.js", null);
    }

    private JsonNode call(String method, String path, String body) {
        String raw = (String) js.executeAsyncScript(FETCH_SCRIPT, method, path, body);
        try {
            JsonNode envelope = MAPPER.readTree(raw);
            int status = envelope.path("status").asInt();
            String responseBody = envelope.path("body").asText();
            LOG.debug("{} {} -> {} {}", method, path, status, responseBody);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException(method + " " + path + " failed with HTTP " + status + ": " + responseBody);
            }
            return MAPPER.readTree(responseBody);
        } catch (IOException e) {
            throw new UncheckedIOException("Unparseable response from " + path + ": " + raw, e);
        }
    }
}
