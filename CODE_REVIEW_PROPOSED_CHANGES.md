# Backend Code Review and Proposed Changes

Reviewed: 2026-09-03

Scope: the current backend working tree, including controllers, services, security filters, persistence mappings, Flyway configuration, external integrations, and tests. The worktree already contained uncommitted changes when this review was performed; this document reviews that current state and does not modify application code.

## Executive summary

The project has a solid baseline: layered packages, DTO validation, method security in several core services, pessimistic stock/cart locking, Flyway migrations, consistent API error DTOs, role hierarchy tests, and useful integration coverage around stock and reporting.

The most urgent work is at the authorization and payment boundaries. Several authenticated endpoints trust a `userId` supplied by the client, allowing one signed-in user to act on another user's cart, saved-card customer, favorites, reviews, addresses, or coupon calculation. Separately, checkout trusts client-supplied payment fields and can label a card order paid without proving that Stripe succeeded. These should be fixed before production use.

## Findings

### P0 — Derive the acting user from authentication instead of request IDs

**Affected code**

- `OrderService.java:51-53`: `checkout` has no ownership `@PreAuthorize`; `/api/orders/**` is merely authenticated in `SecurityConfig.java`.
- `PaymentController.java:25-32,74-90` and `StripeService.java:68-73,124-168`: payment intents, card listing, setup intents, and card deletion accept identifiers without checking ownership.
- `FavoriteController.java:19-40` / `FavoriteService.java:30-81`: every operation accepts an arbitrary user ID and has no method-level ownership check.
- `ReviewController.java:26-40` / `ReviewService.java:41-75`: callers can create a review as any user; deletion compares the review owner to a caller-provided ID rather than the authenticated principal.
- `AddressService.java:50-53,67-71`: any authenticated user can fetch an address by ID or list every address because the corresponding controller paths are admitted by the broad address security rule.
- `CouponController.java:53-57` / `CouponService.java:78-87`: coupon validation reads any user's cart via a request parameter.

**Impact**

This is an insecure direct object reference (IDOR) class of vulnerability. A customer who guesses or enumerates IDs can read or mutate another customer's data. Payment-method deletion is especially serious because `deleteCard` detaches any Stripe payment-method ID without verifying that it belongs to the current user's Stripe customer.

**Proposed change**

1. Remove customer `userId` parameters from self-service routes and DTOs. Resolve the principal once through `AuthUtil` (or an application-level `CurrentUser` abstraction).
2. For admin-on-behalf-of-user operations, create explicit `/admin/...` endpoints protected with `hasRole('ADMIN')`; do not overload customer endpoints.
3. Where IDs remain unavoidable, query by both resource ID and owner ID, e.g. `findByIdAndUserId`, so ownership is part of the database lookup.
4. Before detaching a Stripe payment method, retrieve it and confirm `paymentMethod.customer == currentUser.stripeCustomerId`.
5. Add cross-user denial tests for every resource family.

### P0 — Make Stripe the authority for card-payment state

**Affected code**

- `OrderService.java:79` and `setPaymentFields` around `OrderService.java:187-199`: a `CARD` order without a payment-intent ID is marked `PAID`.
- `OrderService.java:90-133`: guest checkout always calls `setPaymentFields(..., null)`, so a guest choosing `CARD` is immediately recorded as paid.
- `OrderService.java:51-87`: authenticated checkout accepts any client-supplied `paymentIntentId` without retrieving it from Stripe or checking status, amount, currency, customer, or metadata.
- `StripeService.java:224-242`: the webhook only updates an already-created order found by payment-intent ID; it does not protect order creation from an unrelated or underpaid intent.

**Impact**

A caller can create an unpaid order marked paid, reuse another payment-intent ID, or associate an intent for the wrong amount with an order. Inventory and coupon usage are consumed before payment is authoritative.

**Proposed change**

Adopt one coherent flow:

1. The server creates a checkout/payment record with a server-calculated immutable amount and a unique checkout ID in Stripe metadata.
2. Never set `PAID` from a checkout request. Initial card state is `PENDING_PAYMENT`.
3. On a signed, idempotently processed Stripe webhook, retrieve/correlate the checkout, verify amount, currency, customer (when present), and metadata, then create/confirm the order in one transaction.
4. Add a unique constraint on `stripe_payment_id` and persist processed Stripe event IDs to make webhook replay harmless.
5. Expire failed/abandoned reservations and restore stock. COD can continue through a separate explicit path.

At minimum, reject a card checkout without an intent and retrieve the intent server-side before accepting it.

### P0 — Align Stripe currency and monetary units

**Affected code**

- `StripeService.java:91-102,188-192,206-208` multiplies stored totals by 100 and submits them as `usd`.
- The rest of the application and order email render the same prices as RSD (for example `OrderService.java` product-row rendering).

**Impact**

If product prices are RSD, a product priced at `1,000` is sent as USD 1,000 rather than RSD 1,000. This is a direct overcharge risk and makes reports/order records disagree with Stripe.

**Proposed change**

Define one configured store currency (ISO lowercase for Stripe) and a currency-aware minor-unit converter. RSD has two minor-unit digits, but the currency must still be `rsd`. Store the currency on each order/payment so historical orders remain interpretable if store settings change. Add boundary tests for rounding and zero/negative totals.

### P1 — Correct discounted cart subtotal multiplication

**Affected code**

`CartService.java:168-172` multiplies quantity only in the regular-price branch:

```java
BigDecimal subtotal = discountPrice != null
        ? discountPrice
        : price.multiply(quantity);
```

**Impact**

Two or more units of a discounted product display and total as the price of one unit. The cart total can disagree with the payment intent and final order total.

**Proposed change**

Choose the effective unit price first, then always multiply:

```java
BigDecimal unitPrice = product.getDiscountPrice() != null
        ? product.getDiscountPrice()
        : product.getPrice();
BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
```

Add parameterized tests for regular/discounted prices with quantities 1 and greater than 1.

### P1 — Apply one coupon policy atomically everywhere

**Affected code**

- `CouponService.java:92-105` checks active status, expiration, and usage limit.
- `OrderService.java:169-183` omits the expiration check.
- `StripeService.java:197-203` also omits expiration and silently ignores invalid coupons, while checkout rejects some invalid coupons.
- `OrderService.java:178-181` increments `usageCount` with a read-modify-write and no coupon row lock.
- `CouponService.calculateTotalPrice` uses regular product price while checkout/payment logic uses discount price.

**Impact**

Expired coupons may be redeemed, concurrent checkouts can exceed usage limits, and validation/payment/order totals can disagree.

**Proposed change**

Move coupon validation and discount calculation into one transactional domain service used by preview, payment-intent creation, and checkout. Lock the coupon row (`PESSIMISTIC_WRITE`) or perform an atomic conditional update such as `usage_count = usage_count + 1 WHERE usage_count < usage_limit AND ...`. Reserve/consume usage only at the defined business milestone (preferably confirmed payment, or provide rollback/expiry semantics).

### P1 — Revalidate stock limits when carts change

**Affected code**

- `CartService.java:60-70` checks the incoming quantity but, for an existing row, stores `existing + incoming` without checking that combined amount against stock.
- `CartService.java:86-103` updates quantity without any stock check.

**Impact**

Users can place quantities above available stock into the cart, producing avoidable checkout failures and misleading availability. Checkout's pessimistic stock check is still the necessary final authority and should remain.

**Proposed change**

Validate the resulting quantity in both add and update operations. Centralize the rule and test existing-quantity accumulation, direct updates, zero/removal, and concurrent checkout behavior.

### P1 — Handle malformed JWTs as 401 responses

**Affected code**

`JwtFilter.java:36-70` calls `jwtUtil.extractUsername(token)` before a try/catch. `JwtUtil.extractUsername` parses and verifies the token and may throw `JwtException` or `IllegalArgumentException`.

**Impact**

A malformed, expired, or incorrectly signed bearer token may escape the filter as a server error instead of producing the documented JSON 401 response. Exceptions thrown before controller dispatch are not reliably handled by `@RestControllerAdvice`.

**Proposed change**

Catch JWT parsing/validation failures inside the authentication filter, clear the security context, and delegate to `JwtAuthEntryPoint` (or continue unauthenticated if the endpoint is public and that is the intended policy). Add MockMvc tests for malformed, expired, wrong-signature, and revoked-version tokens.

### P1 — Validate uploads by content and contain resolved paths

**Affected code**

- `FileUploadController.java:35-45` accepts a file when either the caller-provided content type starts with `image/` or `video/`, or its extension is allow-listed. Both values are client-controlled.
- `FileStorageService.java:27-29` incorporates the original filename and does not explicitly normalize/check that the resulting path stays below the upload root.
- `FileServeController.java:28-31` normalizes a requested filename but does not assert `resolvedPath.startsWith(uploadRoot)`.

**Impact**

An authorized uploader can store content whose bytes do not match the expected media type. Depending on container/path decoding and platform filename behavior, insufficient root containment also creates unnecessary path-traversal risk.

**Proposed change**

Generate the complete stored filename server-side, detect type from file signatures/decoded media, enforce exact allowed formats and size/dimension/duration limits, and reject active formats not explicitly needed. For every local read/write, use an absolute normalized root and require the resolved target to start with that root. Return safe response headers (`Content-Disposition`, `X-Content-Type-Options: nosniff`) where appropriate.

### P1 — Run Flyway migrations in CI and enable validation

**Affected code**

- `application-ci.yml:27-33` uses Hibernate `create-drop` and disables Flyway.
- `application.yml` and production configuration disable `validate-on-migrate`; the base configuration also ignores missing migrations.

**Impact**

CI proves that entities can generate a schema, but not that a clean database can execute the real migration history or that the migrated schema matches JPA. Deleted/renamed/edited migrations can reach production unnoticed.

**Proposed change**

Add a migration integration-test job/profile that starts with an empty PostgreSQL database, enables Flyway, uses `ddl-auto: validate`, and fails on missing or changed migrations. Enable `validate-on-migrate` in production and remove broad missing-migration ignores. Keep a separate fast unit-test profile if desired.

### P2 — Send external notifications after transaction commit

**Affected code**

- `OrderService.java:82-87` clears the cart, calls the email service, and only then explicitly saves the order.
- Status-change email sending also occurs in the same service operation as database mutation.

**Impact**

The initial email can receive a null order ID, and an email can be sent for a transaction that later rolls back. Conversely, errors are swallowed, so delivery is best-effort without durable coordination.

**Proposed change**

Save/flush the order before creating notification data, publish an application event, and handle it after commit. For reliable delivery, use an outbox table processed asynchronously, with retry and idempotency. Pass escaped structured values to the template rather than assembling HTML in the order domain service.

### P2 — Bound and harden the in-memory rate limiter

**Affected code**

`RateLimitFilter.java:33-80` keeps a permanent nested map per attacker-controlled IP and trusts `X-Forwarded-For` unconditionally.

**Impact**

The map can grow without bound, limits are per application instance, and clients can spoof IP values unless a trusted proxy sanitizes the header.

**Proposed change**

Use a bounded expiring cache (Caffeine is already included) for single-instance deployments or Redis/gateway rate limiting for multiple instances. Only consume forwarded headers when running behind explicitly trusted proxies. Return the common `ApiError` shape and consider `Retry-After`.

### P2 — Improve error classification and operational logging

**Affected code**

- Multiple business failures throw plain `RuntimeException`; `GlobalExceptionHandler.java:185-194` maps every such failure to HTTP 400.
- The catch-all `RuntimeException` handler does not log or send unexpected failures to Sentry.
- `UserService.java:59-61` and `LocalStorageService.java:40` write directly to standard output.
- Several email catches log only a concatenated message and discard the stack trace.

**Impact**

Unexpected programming/infrastructure failures can be mislabeled as client errors and disappear from monitoring. Standard-output debugging also bypasses structured logging controls.

**Proposed change**

Use specific domain exceptions for validation, conflict, forbidden, not found, and external dependency failures. Treat unknown runtime failures as 500, log them with stack traces and a request/correlation ID, and capture them in Sentry. Replace `System.out` and string-concatenated exception logs with parameterized SLF4J calls.

## Recommended implementation order

1. Close all IDOR paths and add cross-user security tests.
2. Redesign/repair card checkout, webhook idempotency, and currency handling.
3. Fix cart subtotal, stock accumulation, and centralized coupon rules.
4. Add migration-backed CI and strict Flyway validation.
5. Harden JWT failure handling, uploads, rate limiting, and logging.
6. Move email/notification delivery to after-commit or an outbox.

## Suggested regression-test matrix

- Customer A cannot read or mutate Customer B's cart, addresses, favorites, reviews, orders, Stripe customer, setup intent, or payment methods.
- Admin-only on-behalf-of-user endpoints remain available and audited.
- Card checkout rejects missing, failed, wrong-currency, wrong-amount, wrong-customer, unrelated, and reused intents.
- Duplicate Stripe webhook delivery produces exactly one state transition/order.
- Guest and authenticated checkout calculate identical totals from the same items, discounts, coupon, delivery rule, and currency.
- Discounted cart line totals multiply by quantity.
- Coupon expiration and usage-limit races are enforced at the database/transaction boundary.
- Existing cart quantity plus added quantity cannot exceed stock; direct cart updates cannot exceed stock.
- Malformed/expired/revoked JWTs return the expected JSON 401, not 500.
- Flyway migrates an empty PostgreSQL database and Hibernate validation passes.
- Uploaded spoofed/polyglot content and path traversal attempts are rejected.

## Verification notes

Static review was completed against the current working tree. Automated tests could not be executed in this environment: `mvnw.cmd test` failed inside the wrapper script with `Cannot index into a null array`, and a system `mvn` executable is not installed. CI is configured to run `mvn clean verify`, but its CI profile currently bypasses Flyway as described above.
