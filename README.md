# E-commerce BDD Automation Framework

Reusable UI test automation for Shopify and other e-commerce storefronts, built with
**Java 21 · Cucumber 7 (Gherkin BDD) · Selenium 4 · JUnit 5 · Maven**.

The framework ships with two working example profiles: the public Shopify *Dawn* demo store and
a public WooCommerce practice store (AskOmDch). The same feature files run against both.
Onboarding a new client store means adding a folder of configuration, not writing new Java.

| Profile (`-Dstore=`)     | Platform    | Target                                  |
|--------------------------|-------------|-----------------------------------------|
| `shopify-dawn` (default) | Shopify     | https://theme-dawn-demo.myshopify.com   |
| `woocommerce-askomdch`   | WooCommerce | https://askomdch.com                    |

## Quick start

```bash
mvn test                                              # all scenarios, default store, headed Chrome
mvn test -Dcucumber.filter.tags="@smoke"              # smoke suite only
mvn test -Dheadless=true -Dbrowser=edge               # headless Edge
mvn test -Dstore=my-client -Dcucumber.filter.tags="@regression and not @requires-account"
mvn test -Dcucumber.execution.parallel.enabled=true   # 3 scenarios in parallel (one browser each)
mvn test -Dcucumber.features=@target/cucumber-reports/rerun.txt   # re-run only failed scenarios
```

Requirements: JDK 21, Maven 3.9, and Chrome, Edge or Firefox installed. Selenium Manager downloads
the matching driver automatically. Reports land in `target/cucumber-reports/cucumber.html`,
failure screenshots in `target/screenshots/`, logs in `target/logs/test-run.log`.

## What is covered

| Feature file        | Scenarios                                                                 | Tags                       |
|---------------------|---------------------------------------------------------------------------|----------------------------|
| `home.feature`      | Home page loads, header nav, browse collection, open product              | `@home @smoke @regression` |
| `search.feature`    | Keyword search, outline with several terms, no-results, open a result     | `@search`                  |
| `product.feature`   | Title/price, sold-out state, variant select, add to cart, cart badge      | `@product`                 |
| `cart.feature`      | Line items and totals, change quantity, remove item, cart API cross-check | `@cart @shopify`           |
| `checkout.feature`  | Cart to checkout, fill contact + delivery form (never places an order)    | `@checkout`                |
| `login.feature`     | Login form, invalid credentials error, valid login (skipped without creds)| `@account @requires-account` |

## Architecture

```
src/test
├── java/com/sarvadalabs/automation
│   ├── runners/TestRunner.java        JUnit 5 suite that discovers features/
│   ├── hooks/Hooks.java               browser per scenario, screenshots on failure
│   ├── steps/*Steps.java              Gherkin glue, thin: delegate to page objects
│   ├── pages/*Page.java               page objects, one per storefront page
│   ├── core/BasePage.java             fallback-locator lookup, resilient click, waits
│   ├── core/Locators.java             locator repository (default + per-store override)
│   ├── config/ConfigManager.java      layered configuration
│   ├── config/TestData.java           dotted-path access to testdata.json
│   ├── context/ScenarioContext.java   per-scenario state + page cache (PicoContainer DI)
│   ├── driver/Driver{Factory,Manager} Chrome/Edge/Firefox, local or Selenium Grid, ThreadLocal
│   ├── api/ShopifyCartApi.java        /cart.js cross-checks through the browser session
│   └── util/Money.java                "$1,234.50 CAD" -> 1234.50
└── resources
    ├── features/*.feature             store-agnostic scenarios
    ├── config/default.properties      framework defaults
    ├── locators/default.properties    Shopify Dawn baseline selectors
    ├── stores/<store>/                one folder per client store
    │   ├── store.properties           base URL, paths, store name
    │   ├── locators.properties        selector overrides (only what differs)
    │   └── testdata.json              products, search terms, checkout details
    ├── junit-platform.properties      Cucumber plugins, glue, parallelism
    └── log4j2.xml
```

### Design choices that make it reusable

- **Store profiles.** `-Dstore=<name>` selects `stores/<name>/`. Feature files never mention a URL,
  a product or a selector; they reference test-data keys such as `"inStock"` or `"validTerm"`.
- **Locator repository with fallbacks.** Each key lists several selectors separated by `||`; the
  first one matching a *visible* element wins. Themes that differ only need overrides in the store's
  `locators.properties`. Placeholders `{0}` let one locator find "the cart row for product X".
- **Layered configuration.** Command-line `-D` > environment variable > store profile > defaults.
  Secrets such as `LOGIN_EMAIL` / `LOGIN_PASSWORD` are read from the environment, never committed.
- **Thin steps, fat pages.** Step definitions only translate Gherkin to page-object calls and assert
  with AssertJ. All waiting and DOM knowledge lives in the page objects.
- **Parallel-safe.** The driver is `ThreadLocal`, scenario state is injected per scenario by
  PicoContainer, so `cucumber.execution.parallel.enabled=true` works out of the box.
- **UI + API.** On Shopify stores the AJAX cart API (`/cart.js`) is called inside the browser
  session to confirm what the UI shows.

## Configuration reference

| Key (property / env)                   | Default        | Purpose                                   |
|----------------------------------------|----------------|-------------------------------------------|
| `store` / `STORE`                      | `shopify-dawn` | Store profile folder                      |
| `browser` / `BROWSER`                  | `chrome`       | `chrome`, `edge`, `firefox`               |
| `headless` / `HEADLESS`                | `false`        | Run without a visible window              |
| `grid.url` / `GRID_URL`                | empty          | Selenium Grid / cloud provider endpoint   |
| `base.url` / `BASE_URL`                | from store     | Override the store URL (e.g. staging)     |
| `timeout.element`                      | `15`           | Seconds to wait for an element            |
| `timeout.pageLoad`                     | `60`           | Page load timeout in seconds              |
| `screenshot.everyStep`                 | `false`        | Attach a screenshot after each step       |
| `login.email` / `LOGIN_EMAIL`          | empty          | Valid customer for `@requires-account`    |
| `login.password` / `LOGIN_PASSWORD`    | empty          |                                           |
| `cucumber.filter.tags`                 | all            | Tag expression, e.g. `@smoke and not @shopify` |

## Adding a client store

See [docs/ONBOARDING_A_NEW_CLIENT.md](docs/ONBOARDING_A_NEW_CLIENT.md). In short:

1. Copy `src/test/resources/stores/_template` to `stores/<client>`.
2. Fill in `store.properties` (URL, store name) and `testdata.json` (real product handles).
3. Run `mvn test -Dstore=<client> -Dcucumber.filter.tags="@smoke"`; for every failing step add a
   selector override to the client's `locators.properties`.
4. Add client-specific feature files under `features/<client>/` when their flows go beyond the
   shared scenarios, reusing the existing step definitions where possible.

## CI

`.github/workflows/ci.yml` runs the smoke suite headless on every push and pull request and
uploads the HTML report, screenshots and log as a build artifact. Trigger it manually with a
different store profile or tag expression from the Actions tab.

## Known behaviours and platform notes

- **Verified run.** Against the Dawn demo store the full suite gives 16 passed, 2 skipped, 0 failed
  (headless Chrome, ~2 minutes sequentially).
- **Captcha on login.** Shopify may put an hCaptcha challenge in front of customer login for
  automated browsers. The invalid-login scenario detects the widget and marks itself *skipped* with
  an explanation instead of failing. The framework never attempts to solve or bypass captchas.
- **Skipped scenarios** are reported as skipped, not failed, when a precondition is missing
  (no credentials, captcha present). Look at the reason in the HTML report.
- **Windows: tag expressions with spaces.** `cmd.exe` mangles `-Dcucumber.filter.tags="@a or @b"`
  when Maven is called by full path from Git Bash. Use PowerShell:
  `& mvn "-Dcucumber.filter.tags=@cart or @account"`, or an expression without spaces.
- **Windows: loopback error.** If Selenium fails with `Unable to establish loopback connection`,
  the JDK is creating its Unix-domain socket in a temp path with an 8.3 short name. The POM already
  passes `-Djdk.net.unixdomain.tmpdir=target` to the test JVM to avoid it.
- **Selenium / Chrome versions.** A warning like `Unable to find CDP implementation matching NNN`
  is harmless; bump `selenium.version` in the POM when a newer Chrome ships.

## Conventions

- Tags: `@smoke` (fast health check), `@regression` (full suite), one feature tag per file,
  `@shopify` for platform-specific scenarios, `@requires-account` for scenarios needing credentials.
- Never enter payment details or place orders. Checkout scenarios stop at the delivery form.
- Prefer stable attributes (`name`, `id`, `data-*`) over class names when adding locators.
- Keep step wording business-facing; a client should be able to read a feature file.
