# NoPressure Wear — Changes & Remaining Work

---

## Done

### 1. Config & Secrets Externalization (`feature-config-env-vars-and-profiles.md`) ✅ DONE

**Files changed:**
- `nopressure-wear-backend/src/main/resources/application.yml` — stripped to shared base config (profile switch, JPA, 
   Flyway, multipart, mail SMTP properties, server port, actuator). Zero secrets.
- `nopressure-wear-backend/src/main/resources/application-local.yml` *(new, gitignored)* — all local dev values 
   (DB, Mailtrap, JWT, Stripe, removebg, Google OAuth2, frontend/base URLs, `ddl-auto: update`)
- `nopressure-wear-backend/src/main/resources/application-prod.yml` *(new)* — all values as `${ENV_VAR}` references only; 
   missing required vars fail fast at startup
- `nopressure-wear-backend/src/main/resources/application-local.yml.example` *(new, committed)* — placeholder template 
   with `CHANGE_ME` values for teammates
- `nopressure-wear-backend/.gitignore` — added `application-local.yml` and `*.env`
- `nopressure-wear-frontend/.gitignore` — added `.env.local` and `.env.*.local`

**Hardcoded URLs replaced:**
- `OAuth2AuthenticationSuccessHandler.java` — injected `@Value("${app.frontend-url}")`, replaced two hardcoded 
  `localhost:5173` redirect URLs
- `EmailService.java` — injected `@Value("${app.frontend-url}")`, replaced password-reset and order-detail hardcoded URLs
- `SecurityConfig.java` — injected `@Value("${app.frontend-url}")`, replaced hardcoded CORS allowed origins

> **Action required:** Secrets that were previously committed to git (DB password, JWT secret, Stripe key, 
                       Google OAuth credentials) must be rotated — treat them as compromised.

---

### 2. Go-Live Hardening (`go-live-readiness-audit.md`)

#### Tier 0.1 — Stripe Webhooks ✅ DONE

**Problem fixed:** Orders confirmed client-side only; a user who paid then closed the tab before redirect left a charged 
                   card with no confirmed order.

**Files changed:**
- `pom.xml` — no new dep needed (stripe-java already included)
- `application-local.yml` — added `app.stripe.webhook-secret: whsec_test_CHANGE_ME`
- `application-prod.yml` — added `app.stripe.webhook-secret: ${STRIPE_WEBHOOK_SECRET}`
- `PaymentIntentResponse.java` — added `paymentIntentId` field so frontend can pass it to checkout
- `PaymentController.java` — added `POST /api/payments/webhook` (raw `byte[]` body, no auth, Stripe-Signature verified)
- `StripeService.java` — added `handleWebhookEvent()` with signature verification; handles `payment_intent.succeeded` 
                       → PAID and `payment_intent.payment_failed` → FAILED, idempotently
- `OrderRepository.java` — added `findByStripePaymentId(String)`
- `OrderController.java` — added optional `paymentIntentId` request param to `/{userId}/checkout`
- `OrderService.java` — updated `setPaymentFields()`: when CARD + paymentIntentId provided, stores it and sets 
                        `paymentStatus = PENDING_PAYMENT` (webhook confirms); without it, keeps old `PAID` behavior for 
                        backward compat
- `SecurityConfig.java` — added `permitAll` for `POST /api/payments/webhook`

**How to activate on frontend:** After Stripe `confirmPayment()` succeeds, pass the PaymentIntent ID (returned from
                                 `/api/payments/create-payment-intent` as `paymentIntentId`) as a request param when 
                                 calling `/{userId}/checkout`. The webhook will then set the order to PAID server-side.

**Env var to add on prod host:** `STRIPE_WEBHOOK_SECRET` — get it from the Stripe Dashboard → Webhooks → your endpoint → signing secret.

---

#### Tier 0.4 — XSS Sanitization ✅ DONE

**Problem fixed:** Admin-submitted HTML (popup content, product descriptions, store tagline, broadcast messages) was 
stored and rendered raw via `dangerouslySetInnerHTML` with no server-side sanitization.

**Files changed:**
- `pom.xml` — added `jsoup 1.18.3`
- `util/HtmlSanitizer.java` *(new)* — Jsoup `Safelist.relaxed()` strips `<script>`, event handlers, iframes, 
- `javascript:` hrefs; allows standard formatting tags (b, i, u, em, strong, p, ul, ol, li, a, h1-3, br, etc.)
- `PopupService.java` — sanitize `content` on create and update
- `ProductService.java` — sanitize `description` on create and update
- `StoreSettingsService.java` — sanitize value when key is `store_tagline`
- `NotificationService.java` — sanitize `message` before saving to DB

---

#### Tier 0.6 — Rate Limiting ✅ DONE

**Problem fixed:** No brute-force protection on login, registration, password reset, or contact endpoints.

**Files changed:**
- `security/RateLimitFilter.java` *(new)* — sliding-window in-memory filter (no extra dependency):

| Endpoint                         | Limit             |
|----------------------------------|-------------------|
| `POST /api/auth/login`           | 10 req/min per IP |
| `POST /api/auth/forgot-password` | 5 req/min per IP  |
| `POST /api/auth/register`        | 20 req/min per IP |
| `POST /api/contact`              | 10 req/min per IP |

Returns HTTP 429 with JSON error when exceeded. Uses `X-Forwarded-For` for proxy-aware IP resolution.

---

#### Tier 1 — Stock Race Conditions ✅ DONE

**Problem fixed:** Two concurrent checkouts could both pass the stock check and both decrement, resulting in negative stock (oversell).

**Files changed:**
- `ProductRepository.java` — added `findByIdForUpdate(@Lock PESSIMISTIC_WRITE)` query
- `OrderService.buildOrderItems()` — re-fetches each product with a pessimistic write lock instead of using the 
                                     pre-loaded cart item reference; second concurrent transaction blocks until the first commits

---

#### Tier 1 — Order Idempotency (Cart-Level) ✅ DONE

**Problem fixed:** Two simultaneous checkout requests for the same cart could both create orders.

**Files changed:**
- `CartRepository.java` — added `findByUserIdForUpdate(@Lock PESSIMISTIC_WRITE)`
- `OrderService.checkout()` — now locks the cart row before reading; second concurrent checkout blocks, then sees an 
                              empty cart and throws `"Cart is empty"`

---

#### Tier 1 — Health Checks ✅ DONE

**Problem fixed:** No `/actuator/health` endpoint for load balancers or uptime monitors.

**Files changed:**
- `pom.xml` — added `spring-boot-starter-actuator`
- `application.yml` — configured `management.endpoints.web.exposure.include: health` with `show-details: never`
- `SecurityConfig.java` — added `permitAll` for `/actuator/health`

---

## Remaining / Not Yet Done

### Tier 0 — Go-Live Blockers

#### 0.2 Production File Storage ❌ NOT DONE
**Status:** Not implemented — requires infrastructure decision.

**Problem:** Images upload to local disk (`app.upload.dir`). Most PaaS hosts have ephemeral filesystems 
             — uploaded images vanish on every redeploy. Email inline images also read from local files.

**Options:**
- **Cloudinary** (recommended) — free tier, built-in CDN + image resize, returns a public URL. Integrate via their 
                                 Java SDK or a simple `RestTemplate`/`WebClient` call to the upload API.
- **AWS S3 / Backblaze B2** — store objects, serve via CDN URL.
- **Persistent volume** — only if your host supports a mounted disk that survives deploys (e.g. Railway volumes, 
                          Render disks, VPS with mounted storage).

**What needs changing once you pick a provider:**
- `UploadService` / `UploadController` — replace local `Files.write()` with SDK upload call; return the public CDN URL 
                                         instead of a relative path
- `EmailService` — product images in emails currently embed from local file; with object storage they become 
                   `<img src="https://cdn.example.com/...">` (no inline embedding needed)
- Remove `uploads/` directory from `.gitignore` exception; it becomes irrelevant

---

#### 0.3 Size Variants ✅ DONE

**Status:** Fully implemented.

**Files changed — Backend:**
- `V46__add_size_variants.sql` — creates `product_variant` table (product_id, size, stock_quantity, sku, 
                                 unique constraint on product+size); backfills S/M/L/XL for all existing products 
                                 splitting stock evenly; adds `size VARCHAR(10)` to `cart_item` and `order_item`
- `model/ProductSize.java` *(new)* — enum: `S, M, L, XL`
- `model/ProductVariant.java` *(new)* — `@Entity` with `@ManyToOne(LAZY) product`, `@Enumerated(STRING) size`, `stockQuantity`, `sku`
- `repository/ProductVariantRepository.java` *(new)* — includes `findWithLockByProductIdAndSize` (`@Lock PESSIMISTIC_WRITE`) 
                                                       and `deleteByProductId`
- `model/Product.java` — added `@OneToMany(mappedBy="product") variants`
- `model/CartItem.java` / `model/OrderItem.java` — added `@Enumerated(STRING) size`
- `dto/product/ProductVariantRequest.java` / `ProductVariantResponse.java` *(new)*
- `dto/product/ProductRequest.java` — added `List<ProductVariantRequest> variants`
- `dto/product/ProductResponse.java` — added `List<ProductVariantResponse> variants`, `Integer totalStock`
- `dto/cart/CartItemRequest.java` / `CartItemResponse.java` — added `ProductSize size`
- `dto/order/OrderItemResponse.java` — added `ProductSize size`
- `dto/order/GuestOrderRequest.GuestOrderItem` — added `ProductSize size`
- `repository/CartItemRepository.java` — added `findByCartIdAndProductIdAndSize`
- `service/ProductService.java` — `create`/`update` call `buildVariants()` + `saveAll()`; stock total computed as sum of variant stocks; `toResponse()` includes sorted variants + totalStock; `@Transactional` added to mutating methods
- `service/CartService.java` — `addItem()` now requires size, checks variant stock (not product stock), matches cart items by (product, size); `toItemResponse()` includes size; added `ProductVariantRepository` dependency
- `service/OrderService.java` — `ProductWithQuantity` record gains `size`; `buildOrderItems()` locks and decrements variant stock (falls back to product stock for old size-less items); `restoreStockIfOrderCanceled()` restores variant stock on cancel/un-cancel; order email rows include "Size: XL" label; `toItemResponse()` includes size; added `ProductVariantRepository` dependency
- `test/service/ProductServiceTest.java` — added `@Mock ProductVariantRepository`

**Files changed — Frontend:**
- `i18n/en.json` + `sr.json` — added `product.size`, `product.selectSizePrompt`, `product.selectSizeFirst`, `product.sizeSoldOut`; `admin.sizesAndStock`, `admin.sizesHint`, `admin.sizeSku`
- `context/GuestCartContext.jsx` — `addToGuestCart(product, qty, size)` accepts size; guest cart items keyed by (productId, size); `updateGuestCartItem`/`removeFromGuestCart` match by both
- `pages/ProductDetailPage.jsx` — `selectedSize` state; size selector buttons (sold-out sizes disabled with strikethrough); quantity selector gated on selected size; max quantity from `selectedVariant.stockQuantity`; add-to-cart button shows "Select a size" until size chosen; size sent to API and guest cart
- `pages/CartPage.jsx` — size label shown per item below SKU; guest cart item `id` is composite `productId+size`; update/remove handlers pass size
- `pages/admin/AdminProducts.jsx` — `formData.variants` replaces `stockQuantity`; 4-row size table (stock + optional SKU per size) replaces the stock quantity field; `handleEdit` populates variants from product; `handleSubmit` sends variants array; `resetForm` resets to 0-stock defaults; `stockQuantity` is computed server-side from variant sum

**Smoke test items to add:**
- [ ] Select a size → add to cart → checkout → order item shows correct size
- [ ] Out-of-stock size button is disabled on product page
- [ ] Cancel order → variant stock restored correctly
- [ ] Admin saves product with per-size stock → `totalStock` in API response equals sum of variants

---

### Tier 1 — Important (Soon After Launch)

#### Email Verification on Registration ✅ DONE
**Status:** Fully implemented (soft gate — checkout blocked, login allowed).

**Files changed — Backend:**
- `V__add_email_verification_token.sql` *(Flyway migration)* — creates `email_verification_token` table (id, token, user_id FK, expires_at, used, created_at)
- `model/EmailVerificationToken.java` *(new)* — JPA entity; `@Builder.Default` on `createdAt` so Lombok builder sets it correctly
- `model/User.java` — added `emailVerified` boolean field
- `repository/EmailVerificationTokenRepository.java` *(new)* — `findByToken`, `invalidatePreviousTokens`, `deleteExpiredBefore`
- `service/AuthService.java` — `register()` calls `sendVerificationEmail()` after save (wrapped in try/catch so email failure doesn't break registration); added `sendVerificationEmail()`, `verifyEmail()`, `resendVerification()` methods; `@Scheduled` `purgeExpiredTokens()` runs nightly at 3 AM
- `service/EmailService.java` — added `sendVerificationEmail()` method with branded HTML template (EN + SR)
- `exception/EmailNotVerifiedException.java` *(new)*
- `exception/GlobalExceptionHandler.java` — handler for `EmailNotVerifiedException` → 403; handler for `DuplicateResourceException` → 409; handler for `BadCredentialsException` → 401
- `controller/AuthController.java` — added `GET /api/auth/verify-email`, `POST /api/auth/resend-verification` endpoints
- `dto/auth/AuthResponse.java` — added `emailVerified` field
- `application-local.yml` — fixed YAML nesting bug: mail was nested under `spring.mail.mail.*` instead of `spring.mail.*`; `JavaMailSender` now auto-configures correctly

**Files changed — Frontend:**
- `pages/VerifyEmailPage.jsx` *(new)* — handles the `/verify-email?token=` callback; shows success/failure state
- `pages/RegisterPage.jsx` — after successful register, shows "Check your inbox" screen with resend button instead of navigating away; inline email-already-exists error under the email field (clears on edit); 409 status OR message-content detection
- `pages/LoginPage.jsx` — inline "Invalid email or password" error above email field (instead of toast); clears on any field change; all non-403 login errors show as inline error
- `api/authApi.js` — added `verifyEmail(token)`, `resendVerification(email)` calls
- `api/axiosInstance.js` — 401 redirect exempts `/auth/` endpoints so bad-credential 401s reach the component
- `i18n/en.json` + `sr.json` — added `auth.checkInboxTitle`, `auth.checkInbox`, `auth.resend`, `auth.verifying`, `auth.verifiedTitle`, `auth.verifiedText`, `auth.verifyFailedTitle`, `auth.verifyFailedText`, `auth.goToLogin`, `auth.notVerifiedCheckout`, `auth.emailAlreadyExists`

**Behaviour:**
- Register → verification email sent → "Check your inbox" screen with resend link
- Click link → `/verify-email?token=...` → success screen → can log in
- Checkout while unverified → blocked with translated message
- Duplicate email on register → inline field error (translated, EN + SR)
- Bad credentials on login → inline field error (translated, EN + SR)

---

#### 0.5 Legal & GDPR Pages ❌ NOT DONE
**Status:** Not implemented — requires content + design work.

**Required if selling to EU customers or Serbian residents:**
- Privacy Policy page
- Terms of Service page
- Return / Refund Policy page
- Company Imprint (legal name, address, registration number, contact)
- Cookie consent banner (if you use Google Analytics or other non-essential cookies)
- GDPR right to erasure — at minimum a documented manual process; ideally an admin "Delete customer data" action

**Implementation:** These are mostly static React pages + links in the footer. The cookie banner can be a lightweight
library (e.g. `react-cookie-consent`) or a custom component that sets a `localStorage` flag.

---

#### Error Monitoring (Sentry) ✅ DONE
**Status:** Fully implemented — unhandled backend exceptions and frontend crashes reported to Sentry automatically.

**Files changed — Backend:**
- `pom.xml` — added `sentry-spring-boot-starter-jakarta 7.14.0`
- `application.yml` — added `sentry:` block (`dsn: ${SENTRY_DSN:}` empty default = off locally, `traces-sample-rate: 0.0`, `send-default-pii: false`)
- `application-prod.yml` — added `sentry: dsn: ${SENTRY_DSN}` + `environment: prod`
- `exception/GlobalExceptionHandler.java` — added `Sentry.captureException(ex)` to the catch-all `Exception` handler only; expected business exceptions (404, validation, auth-disabled, etc.) are left unreported to avoid dashboard noise

**Files changed — Frontend:**
- `npm install @sentry/react` installed
- `main.jsx` — `Sentry.init()` (enabled only in prod builds via `import.meta.env.PROD`); `SentryApp = Sentry.withErrorBoundary(App, { fallback })` catches React render crashes and shows a "Something went wrong / Reload" fallback instead of a blank screen
- `context/AuthContext.jsx` — `Sentry.setUser({ id })` on login + initial localStorage restore; `Sentry.setUser(null)` on logout

**Env vars to add in prod:**
- `SENTRY_DSN` — backend DSN (Java project in Sentry dashboard)
- `VITE_SENTRY_DSN` — frontend DSN (React project in Sentry dashboard)
- `SENTRY_ENV=prod`, `SENTRY_RELEASE=1.0.0` (optional but useful)

**Dev behaviour:** empty DSN default + `enabled: PROD` → Sentry is completely silent during local development.

---

#### Shipping Cost ✅ DONE
**Status:** Flat delivery fee with optional free-shipping threshold — fully implemented.

**Files changed — Backend:**
- `V49__add_delivery_settings_and_fee.sql` — inserts `delivery_enabled`, `delivery_fee`, `free_shipping_threshold` into `store_settings`; adds `delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 0` column to `orders`
- `model/Order.java` — added `@Builder.Default deliveryFee = ZERO` field
- `dto/order/OrderResponse.java` — added `deliveryFee` field
- `service/DeliveryService.java` *(new)* — reads settings, computes fee: disabled → 0, subtotal ≥ threshold → 0, else flat fee; exposes `calculateDeliveryFee(subtotal)` + getters
- `service/OrderService.java` — injected `DeliveryService`; `checkout` and `guestCheckout` apply fee after coupon (`total + deliveryFee`), snapshot fee on order; email call passes `order.getDeliveryFee()`; `toResponse()` includes `deliveryFee`
- `service/StripeService.java` — injected `DeliveryService`; `createPaymentIntent` and `createGuestPaymentIntent` add delivery fee to Stripe amount so charge matches order total
- `service/EmailService.java` — added `BigDecimal deliveryFee` param; computes `subtotalDisplay = total − deliveryFee`; delivery row shows real fee (or "Free" in green); total row shows full amount
- `service/ReportService.java` — invoice PDF delivery row now uses `order.getDeliveryFee()` instead of hardcoded "Free"

**Files changed — Frontend:**
- `pages/admin/AdminSettings.jsx` — added Delivery section (toggle for `delivery_enabled`, numeric RSD inputs for `delivery_fee` and `free_shipping_threshold` with hint text)
- `pages/CartPage.jsx` — reads delivery settings from `getSettingsMap()`; mirrors backend logic to compute `deliveryFee`/`grandTotal`/`remainingForFree`; both order-summary blocks show real delivery line and free-shipping nudge
- `i18n/en.json` + `sr.json` — added `cart.freeShippingNudge`; settings keys `delivery`, `delivery_enabled`, `delivery_fee`, `free_shipping_threshold`, `freeShippingHint`

**Defaults:** delivery enabled, 400 RSD flat fee, free shipping above 5 000 RSD (admin can change all three).

---

#### Tax / VAT (PDV) ❌ NOT DONE
**Status:** Not implemented.

If legally required for your business, compute and display VAT on:
- Cart total breakdown
- Order summary
- Invoice PDF
- Stripe amount (must include tax)

---

#### Automated Database Backups ❌ NOT DONE
**Status:** Infrastructure — not a code change.

Set up daily snapshots on your PostgreSQL host. Verify you can restore from a backup before going live.

---

#### 0.6 HTTPS / TLS ❌ NOT DONE
**Status:** Infrastructure-level — not a code change.

- Force HTTP → HTTPS redirect on your host (Vercel, Render, Railway, Nginx, etc. all have a one-click toggle)
- Ensure the production `FRONTEND_URL` and `BASE_URL` env vars use `https://`
- Add `Strict-Transport-Security` header (most hosts do this automatically with HTTPS enabled)

---

#### Account Lockout After Failed Logins ❌ NOT DONE
**Status:** Not implemented.

The `RateLimitFilter` added above limits request rate per IP, but does not lock an account after N failed attempts 
against a specific email. For stronger protection, track failed attempts per email in Redis or DB and lock the account 
for a period.

---

### Tier 2 — Nice to Have (Post-Launch)

| Status | Item                                         | Notes                                                                                                      |
|--------|----------------------------------------------|------------------------------------------------------------------------------------------------------------|
| ❌     | SEO — per-product meta + Open Graph          | React Helmet or `<head>` tags per page                                                                     |
| ❌     | `sitemap.xml` + `robots.txt`                 | Static files in `public/` or generated at build                                                            |
| ❌     | Google Analytics / Plausible                 | Add after cookie consent banner is in place                                                                |
| ❌     | Abandoned cart recovery emails               | Scheduled job: find carts older than X hours with items, send reminder                                     |
| ❌     | Image optimization                           | Automatic if you use Cloudinary (see 0.2)                                                                  |
| ❌     | DB indexes on filter/search columns          | Check `product.brand`, `product.color_name`, `product.category_id`, `order.status`, `order.customer_email` |
| ❌     | Low-stock admin alerts                       | Trigger a notification when stock drops below threshold (you already have the low-stock report)            |
| ❌     | Response caching on catalog/filter endpoints | Spring Cache + Caffeine for `/api/products/active`, `/api/categories/active`, `/api/settings/map`          |

---

## Pre-Launch Smoke Test Checklist

- [ ] Place a real card order → webhook fires → order shows PAID → confirmation email arrives
- [ ] Guest checkout → order created → confirmation email sent to guest address
- [ ] COD order flow works end to end
- [ ] Cancel order → stock restored correctly (both variant stock and product total)
- [ ] Out-of-stock product/size cannot be added past available quantity
- [ ] Size selector on product page — sold-out sizes disabled, selected size sent to cart
- [ ] Cart shows size label per item; same product in different sizes = separate line items
- [ ] Admin product form: per-size stock table saves correctly, totalStock = sum of variants
- [ ] Password reset email arrives with correct prod domain (not localhost)
- [ ] Google OAuth2 login works on prod domain (redirect URI whitelisted in Google Console)
- [ ] All images load (from object storage, post-deploy)
- [ ] EN and SR emails both render correctly
- [ ] Admin can log in, manage products/orders, create staff accounts
- [ ] HTTPS enforced; HTTP redirects to HTTPS
- [ ] CORS allows only the prod frontend domain
- [ ] `/actuator/health` returns `{"status":"UP"}`
- [ ] Rate limiting returns 429 after threshold (test with curl loop)
- [ ] Mobile layout verified on a real device

---

## Production Env Vars Checklist

```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://<host>:5432/<db>
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
MAIL_HOST=...
MAIL_PORT=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
JWT_SECRET=...                        # min 32 chars, random
JWT_EXPIRATION=86400000
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...       # from Stripe Dashboard → Webhooks
REMOVEBG_API_KEY=...
SENTRY_DSN=https://...@oXXXX.ingest.sentry.io/XXXX
SENTRY_ENV=prod
SENTRY_RELEASE=1.0.0
FRONTEND_URL=https://yourdomain.com
BASE_URL=https://api.yourdomain.com
UPLOAD_DIR=/data/uploads/products     # only until object storage is set up

# Frontend (host env panel e.g. Vercel)
VITE_SENTRY_DSN=https://...@oXXXX.ingest.sentry.io/YYYY
```
