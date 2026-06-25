# NoPressure Wear — Go-Live Readiness Audit & Roadmap

A prioritized review of what's built, what's missing, and what to do before launching to real customers.
Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, React + Vite + Tailwind, Stripe, OAuth2 (Google/Facebook), Mailtrap → (prod SMTP), i18n EN/SR.

---

## Verdict

**Feature-rich, but not yet go-live ready.** The gaps are mostly production-hardening (payments reliability, storage, security, legal) rather than missing features. Clear the **Tier 0 blockers** and you have a defensible launch.

---

## What's already built (strong foundation)

- Catalog: products CRUD, categories, **color** variants (explicit linking), discounts, material, featured/similar, rich-text descriptions, images + video
- Cart: authenticated + guest, coupons
- Checkout: Stripe card + Cash on Delivery, guest checkout
- Orders: status lifecycle, stock restore on cancel, order-item snapshots (safe product deletion), invoice PDF on CONFIRMED
- Email: order confirmation + status updates, password reset, contact, broadcast/notifications, signature footer, product images inline, translations (being wired)
- Auth: JWT, OAuth2 (Google/Facebook), password reset, disable registration/login + staff access (spec'd)
- Admin: dashboard, reports (PDF/Excel), settings toggles, banners, popups, notifications, store availability / find-in-store, user management (spec'd)
- i18n EN/SR, currency conversion EUR/RSD, reviews/ratings, favorites

---

## TIER 0 — Go-live blockers (do these first)

### 0.1 Stripe webhooks (payment confirmation reliability) ⚠️ highest priority
**Problem:** if orders are confirmed only via the client-side success callback, a customer who pays then closes the tab before redirect leaves you with a charged card and an unconfirmed order (or an order marked paid that wasn't). The browser is not a reliable source of truth.
**Fix:** add a Stripe webhook endpoint that listens for `payment_intent.succeeded` (and `payment_intent.payment_failed`) and confirms/fails the order server-side.
- Endpoint: `POST /api/payments/webhook` — must be `permitAll` AND verify the Stripe signature (`Webhook.constructEvent(payload, sigHeader, endpointSecret)`).
- Store `paymentIntentId` on the order at creation; on `succeeded`, mark that order PAID/CONFIRMED.
- Use the raw request body (do not let Spring deserialize it first) for signature verification.
- Config: `STRIPE_WEBHOOK_SECRET` env var.
- Make order confirmation **idempotent** (webhook can fire more than once).

### 0.2 Production file storage ⚠️
**Problem:** images upload to local disk (`app.upload.dir`). Most PaaS hosts have ephemeral filesystems — uploaded product images, banners, signatures vanish on every redeploy. Email inline images also read from local files.
**Fix (pick one):**
- **Object storage** (recommended): Cloudinary (free tier, built-in resize + CDN) or S3 / Backblaze B2. Upload returns a public URL stored in DB.
- **Persistent volume**: if the host supports a mounted disk that survives deploys.
**Bonus:** object storage gives you a CDN + image transformations for free, improving performance.

### 0.3 SIZE variants (apparel essential) — CONFIRM
**Problem:** this is a clothing shop; customers must choose a size (S/M/L/XL), and stock should track per size. Color variants exist, but size handling was never built in our sessions.
**If missing, add:**
- `product_size` table or a `size` + `stock` structure per product (e.g. `product_variant(product_id, size, stock_quantity, sku)`).
- Size selector on the product page (required before add-to-cart).
- Per-size stock decrement at checkout; cart stores chosen size.
- Order item snapshot includes size.
> **Open question — confirm whether sizes already exist.** If they do, skip this.

### 0.4 Rich-text XSS sanitization ⚠️
**Problem:** HTML is rendered via `dangerouslySetInnerHTML` (descriptions, popups, broadcasts). Admin content is medium risk; any customer-submitted HTML (e.g. reviews) is high risk for stored XSS.
**Fix:** sanitize on the backend before storing/returning HTML — OWASP Java HTML Sanitizer or Jsoup with a strict allowlist (allow `b/i/u/strong/em/p/ul/ol/li/a/h1-3/br`, strip scripts/handlers/iframes). Never trust client HTML.

### 0.5 Legal & GDPR pages — CONFIRM jurisdiction
**Problem:** taking real money + personal data triggers legal requirements.
**Add (required if selling to EU/Serbia):**
- Privacy Policy, Terms of Service, Return/Refund Policy, company Imprint/contact info.
- **Cookie consent banner** (GDPR) if you use analytics/non-essential cookies.
- Right to data export/deletion (GDPR) — at minimum a documented manual process.
> **Open question — where are you selling?** EU/Serbia ⇒ full GDPR set required.

### 0.6 Production security basics ⚠️
- **HTTPS/TLS** everywhere (host usually provides; force redirect HTTP→HTTPS).
- **CORS** locked to the real frontend domain — not `*`.
- **Rate limiting** on `/auth/login`, `/auth/forgot-password`, `/contact` (brute-force + spam). Bucket4j is simple.
- Secrets moved to env vars + rotate any previously committed (see config spec).
- Consider account lockout / backoff after repeated failed logins.

---

## TIER 1 — Important (soon after launch, ideally before)

- **Email verification on registration** — confirm address via token before activating (cuts fake/typo signups, reduces bounce).
- **Error monitoring** — Sentry (free tier) for backend + frontend; know about 500s before customers report them.
- **Automated DB backups** — daily snapshots; verify you can restore.
- **Health checks** — Spring Actuator `/actuator/health` for uptime monitoring.
- **Shipping cost** — emails hardcode "Delivery: Free". If that's not your model, add real shipping calculation (flat / by weight / by region) and reflect it in totals + Stripe amount + emails.
- **Tax / VAT (PDV)** — if legally required, compute and show it on order + invoice.
- **Stock race conditions** — two buyers, last item: use a transaction + row lock or conditional update (`UPDATE ... WHERE stock >= qty`) so you can't oversell.
- **Order idempotency** — guard against double-submit creating duplicate orders (you added some click guards; ensure the backend is safe too).

---

## TIER 2 — Nice to have (post-launch)

- **SEO**: per-product meta tags + Open Graph (nice share previews), `sitemap.xml`, `robots.txt`, product structured data (schema.org/Product).
- **Analytics**: Google Analytics / Plausible (privacy-friendly).
- **Abandoned cart** recovery emails.
- **CDN / image optimization** (free if you go Cloudinary for storage).
- **Caching** for catalog/filter endpoints if traffic grows.
- **Low-stock alerts** to admin (you have a low-stock report; add a notification trigger).
- **Performance**: verify DB indexes on filter/search/foreign-key columns.

---

## Recommended implementation order

1. **Stripe webhooks** (0.1) — payment integrity
2. **Production file storage** (0.2) — images survive deploys
3. **Confirm / add size variants** (0.3) — apparel requirement
4. **Sanitize rich-text** (0.4) — close XSS
5. **Legal pages + cookie banner** (0.5)
6. **HTTPS / CORS / rate limiting** (0.6)
7. Then Tier 1: email verification, monitoring, backups, shipping/tax as applicable

---

## Open questions to confirm

- [ ] **Size variants** — already implemented, or need building? (Tier 0.3)
- [ ] **Stripe webhooks** — already set up, or confirming client-side only? (Tier 0.1)
- [ ] **Jurisdiction** — EU/Serbia (full GDPR) vs Serbia-only vs not-yet-real-money? (Tier 0.5)
- [ ] **Shipping model** — genuinely free, or needs cost calculation? (Tier 1)
- [ ] **VAT/PDV** — required for your business? (Tier 1)

---

## Pre-launch smoke test (run end-to-end before opening)

- [ ] Place a real card order (Stripe live/test) → webhook confirms → confirmation email arrives → invoice PDF on CONFIRMED
- [ ] Guest checkout works and emails the guest
- [ ] COD order flow
- [ ] Cancel order → stock restored
- [ ] Out-of-stock / last-item concurrency behaves
- [ ] Password reset email link works (correct prod domain, not localhost)
- [ ] OAuth2 login (Google/Facebook) works on the prod domain + redirect URIs whitelisted
- [ ] Images load (from object storage, post-deploy)
- [ ] EN + SR both render correctly across emails and pages
- [ ] Admin can log in, manage products/orders, create staff
- [ ] HTTPS enforced; CORS allows only the prod frontend
- [ ] Mobile layout verified on real device
