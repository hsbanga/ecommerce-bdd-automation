# Onboarding a new client store

Goal: run the shared scenarios against a client's storefront within an hour, then grow a
client-specific suite on top. No Java changes are needed for a Shopify store; other platforms
usually need only locator overrides.

Two worked examples ship with the repo and are the best starting points:

| Client platform | Copy this profile                   | Notes                                                   |
|-----------------|-------------------------------------|---------------------------------------------------------|
| Shopify         | `stores/shopify-dawn`               | Inherits the Dawn baseline; most themes need few overrides |
| WooCommerce     | `stores/woocommerce-askomdch`       | Classic cart/checkout selectors, `cart.updateButton`, Select2 dropdowns |
| Magento 2       | `stores/magento-hyva`               | Hyvä theme selectors with Luma fallbacks; store code in `base.url` |
| Anything else   | `stores/_template`                  | Start from the commented template                       |

Scenarios whose preconditions a store cannot meet report as **skipped** with a reason, never as
failed: no `products.soldOut` entry, no `products.inStock.variant`, a non-Shopify platform for the
cart API check, missing login credentials, or a captcha in front of login.

## 1. Create the store profile

```bash
cp -r src/test/resources/stores/_template src/test/resources/stores/acme
```

Edit `stores/acme/store.properties`:

- `store.name`: text that appears in the home page `<title>`.
- `store.platform`: `shopify` enables the cart-API scenarios; anything else skips them via tags.
- `base.url`: production or staging URL. Password-protected Shopify previews need the storefront
  password entered once; add a `popup.close` locator if a modal appears.
- `path.*`: adjust only when the platform uses different URLs (WooCommerce: `/?s={0}` for search,
  `/product/{0}` for products, `/cart/`, `/my-account/`).

## 2. Fill the test data

Open the client's site and pick:

| Key                          | What to choose                                                    |
|------------------------------|-------------------------------------------------------------------|
| `products.inStock.handle`    | URL slug of a product that is always in stock                     |
| `products.inStock.name`      | Its exact title as displayed                                      |
| `products.inStock.variant`   | A selectable option value (colour/size); delete the key if none   |
| `products.inStock.price`     | Displayed price, digits only; delete the key if prices vary       |
| `products.soldOut.handle`    | A sold-out product; delete the sold-out scenario if none exists   |
| `collections.all`            | A collection handle with several products                         |
| `search.validTerm`           | A word that returns results                                       |
| `checkout.*`                 | Country and province exactly as shown in the checkout dropdowns   |

On Shopify, `https://<store>/products.json` lists handles, variants and availability.

## 3. Run the smoke suite and fix locators

```bash
mvn test -Dstore=acme -Dcucumber.filter.tags="@smoke"
```

For every failing step, open `target/cucumber-reports/cucumber.html`, look at the attached
screenshot and the message `No visible element for locator key '<key>'`. Add that key to
`stores/acme/locators.properties` with the client's selector. Keep the `||` fallbacks so the
override survives small theme changes:

```properties
product.addToCart=css=button[data-testid='add-to-cart'] || css=form.product-form button[type='submit']
```

Repeat with `@regression` once smoke is green.

## 4. Add client-specific scenarios

Put new feature files under `src/test/resources/features/acme/` and tag them `@acme`. Reuse the
existing steps wherever the wording fits. When you need new steps:

1. Add the page interaction to the relevant page object (or a new one extending `BasePage`).
2. Add the locator keys to `locators/default.properties` if generic, or the store file if not.
3. Add the step definition in a new `steps/AcmeSteps.java` that takes `ScenarioContext` in its
   constructor.

Run only that client's scenarios with `-Dcucumber.filter.tags="@acme"`.

## 5. Credentials and secrets

Never commit passwords. Provide them at run time:

```bash
LOGIN_EMAIL=tester@acme.com LOGIN_PASSWORD=... mvn test -Dstore=acme
```

In GitHub Actions add them as repository secrets; the workflow already forwards
`LOGIN_EMAIL` and `LOGIN_PASSWORD`.

## 6. Checklist before handing over

- [ ] `mvn test -Dstore=acme -Dcucumber.filter.tags="@smoke" -Dheadless=true` passes locally
- [ ] Same command passes in CI (workflow_dispatch with `store=acme`)
- [ ] `stores/acme/README.md` (optional) notes any client quirks: popups, geo redirects, captchas
- [ ] No orders were placed and no payment data exists anywhere in the repo
