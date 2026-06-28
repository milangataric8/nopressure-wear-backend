# Feature: Web Analytics (Google Analytics / Plausible) — Consent-Gated

NoPressure Wear — add visitor analytics to understand traffic, behavior, and conversions. **Must load only after cookie consent**, since analytics sets non-essential cookies (ties into the legal/GDPR spec).

Stack: React + Vite. Depends on the cookie consent banner from `feature-legal-gdpr.md`.

> **Prerequisite:** cookie consent banner implemented (`feature-legal-gdpr.md`). Analytics is loaded from the banner's `enableAnalytics()` on accept, and on app load if consent was previously given. **Never** put the analytics snippet unconditionally in `index.html`.

> **Choice:** **Plausible** (privacy-friendly, cookieless option, can avoid the consent requirement) vs **Google Analytics 4** (free, ubiquitous, sets cookies → requires consent). This spec covers both; pick one.

---

## Option A — Plausible (recommended for simplicity + privacy)

Plausible is cookieless and GDPR-friendly by design. If you use the cookieless mode, you may not even need consent for it (confirm with your legal review) — but loading it post-consent is still the safe default.

### A.1 Account
- Sign up at plausible.io (paid, but cheap; or self-host). Add your domain → get the script snippet.

### A.2 Load it from `enableAnalytics()`
```js
// analytics.js
let loaded = false;

export function enableAnalytics() {
    if (loaded) return;
    loaded = true;

    const s = document.createElement('script');
    s.defer = true;
    s.dataset.domain = 'yourdomain.com';
    s.src = 'https://plausible.io/js/script.js';
    document.head.appendChild(s);
}
```

### A.3 SPA pageviews
Plausible's default script auto-tracks pageviews but, for an SPA with client routing, use their **`script.tagged-events.js`** / history-aware variant, or fire manual pageviews on route change:
```jsx
// in a top-level component, on location change:
useEffect(() => {
    if (window.plausible) window.plausible('pageview');
}, [location.pathname]);
```

---

## Option B — Google Analytics 4 (GA4)

Free and feature-rich, but sets cookies → consent required.

### B.1 Account
- Create a GA4 property → get the **Measurement ID** (`G-XXXXXXX`). Set it as `VITE_GA_ID` (env, not hardcoded).

### B.2 Load gtag from `enableAnalytics()` (post-consent only)
```js
// analytics.js
let loaded = false;

export function enableAnalytics() {
    if (loaded) return;
    const id = import.meta.env.VITE_GA_ID;
    if (!id) return;
    loaded = true;

    const s = document.createElement('script');
    s.async = true;
    s.src = `https://www.googletagmanager.com/gtag/js?id=${id}`;
    document.head.appendChild(s);

    window.dataLayer = window.dataLayer || [];
    window.gtag = function () { window.dataLayer.push(arguments); };
    window.gtag('js', new Date());
    window.gtag('config', id, { send_page_view: false }); // we'll send pageviews manually for SPA
}

export function trackPageview(path) {
    if (window.gtag) window.gtag('event', 'page_view', { page_path: path });
}
```

### B.3 SPA pageviews on route change
```jsx
import { useLocation } from 'react-router-dom';
import { trackPageview } from './analytics';

// top-level:
const location = useLocation();
useEffect(() => {
    trackPageview(location.pathname + location.search);
}, [location]);
```

> `trackPageview` is a no-op until `enableAnalytics()` has run (after consent), so it's safe to call always.

---

## Wiring to consent (both options)

In the cookie banner (`feature-legal-gdpr.md`):

```jsx
import { enableAnalytics } from './analytics';

// on Accept:
onAccept={() => { localStorage.setItem('np_cookie_consent', 'accepted'); enableAnalytics(); }}

// on app load (returning visitor who already accepted):
useEffect(() => {
    if (localStorage.getItem('np_cookie_consent') === 'accepted') enableAnalytics();
}, []);
```

If the user **declines**, `enableAnalytics()` is never called → no analytics scripts, no cookies. Correct GDPR behavior.

---

## Optional — e-commerce events

Once analytics loads, you can track key funnel events (especially valuable for a shop):

**GA4 examples:**
```js
window.gtag?.('event', 'view_item', { items: [{ item_id: product.id, item_name: product.name, price: product.price }] });
window.gtag?.('event', 'add_to_cart', { items: [/* ... */] });
window.gtag?.('event', 'begin_checkout', { value: total, currency: 'RSD' });
window.gtag?.('event', 'purchase', { transaction_id: order.id, value: order.totalAmount, currency: 'RSD', items: [/* ... */] });
```
Fire `purchase` on the order-confirmation page. These power conversion + funnel reports. Add incrementally; pageviews alone are a fine start.

---

## Env vars

```
# GA4
VITE_GA_ID=G-XXXXXXX
# Plausible needs no key (domain is in the script); self-hosted needs the host URL
```

Add to the config/env-vars spec. Set only in production.

---

## Notes

- **Consent first, always.** Analytics scripts load only from `enableAnalytics()`, called on accept or for returning accepters. Declining = nothing loads.
- **Privacy policy must list it.** Whichever you choose, disclose it (and Google as a processor for GA) in the Privacy Policy and cookie banner copy.
- **Plausible vs GA:** Plausible is simpler, privacy-first, lighter, and may sidestep consent (cookieless) — but paid. GA4 is free and powerful but heavier and cookie-based. For a small shop wanting minimal compliance overhead, Plausible is attractive; for free + deep funnels, GA4.
- **Dev:** don't load analytics in dev (no consent prompt there, or guard on `import.meta.env.PROD`) so you don't pollute stats with your own testing.
- **SPA pageviews:** both need manual/history-aware pageview tracking on route change — a plain snippet only counts the first load.

---

## Checklist

- [ ] Cookie consent banner in place (prerequisite)
- [ ] Provider chosen (Plausible or GA4); account + ID/domain
- [ ] `analytics.js` with `enableAnalytics()` (loads script) + `trackPageview`
- [ ] Called from consent accept + on load for returning accepters; never before consent
- [ ] SPA pageview tracking on route change
- [ ] (Optional) e-commerce events (view_item, add_to_cart, begin_checkout, purchase)
- [ ] Env var (`VITE_GA_ID` for GA); prod only
- [ ] Disclosed in Privacy Policy + cookie banner
- [ ] Dev excluded from tracking
- [ ] Verified: decline = no scripts/cookies; accept = pageviews recorded; route changes tracked
