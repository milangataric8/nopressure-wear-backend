# Feature: Legal & GDPR — Policy Pages, Cookie Consent, Data Erasure

NoPressure Wear — add the legal pages, cookie consent banner, and GDPR data-erasure capability required when selling to EU customers / Serbian residents.

Stack: React + Vite + Tailwind, react-i18next (EN/SR), Spring Boot backend, PostgreSQL.

> **⚠️ Not legal advice.** This spec covers the **technical implementation**. The actual policy *text* (Privacy Policy, Terms, Returns) must be written/reviewed by a legal professional or generated from a reputable jurisdiction-specific template (Serbia / EU GDPR). The content blocks below are **skeletons with placeholders** — do not ship them as-is without legal review.

---

## Scope

1. **Static legal pages:** Privacy Policy, Terms of Service, Return/Refund Policy, Imprint (company info).
2. **Footer links** to all of them.
3. **Cookie consent banner** — gate non-essential cookies (analytics) until consent.
4. **GDPR right to erasure** — admin action to anonymize a customer's personal data while retaining legally-required order records, plus a documented manual process.

---

## PART A — Static legal pages

### A.1 Reusable layout component

`src/components/legal/LegalPage.jsx` — consistent wrapper for all policy pages:

```jsx
const LegalPage = ({ title, lastUpdated, children }) => (
    <div className="max-w-3xl mx-auto px-4 py-12">
        <h1 className="text-2xl font-black uppercase tracking-tight text-black mb-2">{title}</h1>
        {lastUpdated && (
            <p className="text-xs text-gray-400 mb-8">Last updated: {lastUpdated}</p>
        )}
        <div className="prose prose-sm max-w-none text-gray-700 leading-relaxed space-y-4">
            {children}
        </div>
    </div>
);

export default LegalPage;
```

### A.2 Pages + routes

Create one page component per policy in `src/pages/legal/`, each using `LegalPage`. Wire routes:

```jsx
import PrivacyPolicyPage from './pages/legal/PrivacyPolicyPage';
import TermsPage from './pages/legal/TermsPage';
import ReturnsPage from './pages/legal/ReturnsPage';
import ImprintPage from './pages/legal/ImprintPage';

<Route path="/privacy-policy" element={<PrivacyPolicyPage />} />
<Route path="/terms" element={<TermsPage />} />
<Route path="/returns" element={<ReturnsPage />} />
<Route path="/imprint" element={<ImprintPage />} />
```

### A.3 Content skeletons (PLACEHOLDERS — replace with legal-reviewed text)

**PrivacyPolicyPage.jsx** — what data you collect and why:
```jsx
<LegalPage title={t('legal.privacyTitle')} lastUpdated="2026-06-01">
    <h2>1. Who we are</h2>
    <p>[Company legal name], [address], contact: [email].</p>

    <h2>2. Data we collect</h2>
    <p>Account data (name, email, password hash), order data (items, address, phone),
       payment data (processed by Stripe — we do not store card numbers), and technical
       data (cookies, IP) as described in our Cookie section.</p>

    <h2>3. Why we process it</h2>
    <p>To fulfil orders, provide accounts, send transactional emails, and (with consent) marketing.</p>

    <h2>4. Third parties (processors)</h2>
    <p>Stripe (payments), [email provider], [hosting/storage], [analytics if used], Sentry (error monitoring).</p>

    <h2>5. Your rights (GDPR)</h2>
    <p>Access, rectification, erasure, portability, objection. To exercise them, contact [email].
       See "Data deletion" below.</p>

    <h2>6. Retention</h2>
    <p>Order/invoice records are retained as required by tax/accounting law ([X] years);
       account data is deleted/anonymized on request subject to those obligations.</p>

    <h2>7. Cookies</h2>
    <p>See our cookie banner / Cookie section. Non-essential cookies are set only with consent.</p>

    <h2>8. Contact</h2>
    <p>[email], [address].</p>
</LegalPage>
```

**TermsPage.jsx** — ordering, pricing, payment, delivery, liability (skeleton headings):
```jsx
<LegalPage title={t('legal.termsTitle')} lastUpdated="2026-06-01">
    <h2>1. Scope</h2><p>[...]</p>
    <h2>2. Orders & contract formation</h2><p>[...]</p>
    <h2>3. Prices & payment</h2><p>[...]</p>
    <h2>4. Delivery</h2><p>[...]</p>
    <h2>5. Right of withdrawal / returns</h2><p>See Return Policy.</p>
    <h2>6. Warranty & liability</h2><p>[...]</p>
    <h2>7. Governing law</h2><p>[jurisdiction].</p>
</LegalPage>
```

**ReturnsPage.jsx** — return window, conditions, refund method, who pays return shipping:
```jsx
<LegalPage title={t('legal.returnsTitle')} lastUpdated="2026-06-01">
    <h2>Return window</h2><p>[e.g. 14 days from delivery — EU consumer right of withdrawal].</p>
    <h2>Conditions</h2><p>[unworn, tags attached, etc.].</p>
    <h2>How to return</h2><p>[process + contact].</p>
    <h2>Refunds</h2><p>[method, timeframe, return shipping responsibility].</p>
</LegalPage>
```

**ImprintPage.jsx** — legally required company identification:
```jsx
<LegalPage title={t('legal.imprintTitle')}>
    <p><strong>[Company legal name]</strong></p>
    <p>[Street address, city, postal code, country]</p>
    <p>Registration number: [reg no.]</p>
    <p>Tax / VAT (PIB/PDV) number: [number]</p>
    <p>Email: [email] · Phone: [phone]</p>
    <p>Responsible person: [name]</p>
</LegalPage>
```

> Fill these with your real company details and legally-reviewed text. The Imprint must be accurate — it's a legal identification requirement.

---

## PART B — Footer links

Add a legal links group to the footer:

```jsx
<div className="flex flex-wrap gap-x-6 gap-y-2 text-xs text-gray-400">
    <Link to="/privacy-policy" className="hover:text-black">{t('legal.privacyTitle')}</Link>
    <Link to="/terms" className="hover:text-black">{t('legal.termsTitle')}</Link>
    <Link to="/returns" className="hover:text-black">{t('legal.returnsTitle')}</Link>
    <Link to="/imprint" className="hover:text-black">{t('legal.imprintTitle')}</Link>
</div>
```

---

## PART C — Cookie consent banner

Gate **non-essential** cookies (analytics, marketing) behind consent. Essential cookies (auth/session) don't need consent but should be disclosed.

### C.1 Option A — lightweight library

```bash
npm install react-cookie-consent
```
```jsx
import CookieConsent from 'react-cookie-consent';

// near the app root:
<CookieConsent
    location="bottom"
    buttonText={t('cookie.accept')}
    declineButtonText={t('cookie.decline')}
    enableDeclineButton
    cookieName="np_cookie_consent"
    style={{ background: '#111' }}
    buttonStyle={{ background: '#fff', color: '#111', fontSize: '13px', fontWeight: 600 }}
    declineButtonStyle={{ background: 'transparent', color: '#fff', border: '1px solid #fff', fontSize: '13px' }}
    onAccept={() => enableAnalytics()}
>
    {t('cookie.message')}{' '}
    <Link to="/privacy-policy" style={{ color: '#fff', textDecoration: 'underline' }}>
        {t('cookie.learnMore')}
    </Link>
</CookieConsent>
```

### C.2 Option B — custom banner (localStorage flag)

If you prefer no dependency, a small component that stores the choice in `localStorage` and only renders until a choice is made:

```jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const CookieBanner = () => {
    const { t } = useTranslation();
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        const choice = localStorage.getItem('np_cookie_consent');
        if (!choice) setVisible(true);
    }, []);

    const decide = (accepted) => {
        localStorage.setItem('np_cookie_consent', accepted ? 'accepted' : 'declined');
        setVisible(false);
        if (accepted) enableAnalytics();   // only load analytics on accept
    };

    if (!visible) return null;

    return (
        <div className="fixed bottom-0 inset-x-0 z-50 bg-black text-white px-6 py-4 flex flex-col sm:flex-row items-center gap-4">
            <p className="text-xs flex-1">
                {t('cookie.message')}{' '}
                <Link to="/privacy-policy" className="underline">{t('cookie.learnMore')}</Link>
            </p>
            <div className="flex gap-2">
                <button onClick={() => decide(false)} className="text-xs border border-white px-4 py-2 uppercase tracking-wide">
                    {t('cookie.decline')}
                </button>
                <button onClick={() => decide(true)} className="text-xs bg-white text-black px-4 py-2 uppercase tracking-wide font-semibold">
                    {t('cookie.accept')}
                </button>
            </div>
        </div>
    );
};

export default CookieBanner;
```

> This is your real React app, so `localStorage` is fine here (the no-localStorage rule only applies to Claude.ai artifacts).

### C.3 Gate analytics on consent

Critical: **do not load analytics until consent**. Don't put the GA/Plausible snippet unconditionally in `index.html`. Instead load it from `enableAnalytics()`:

```js
function enableAnalytics() {
    if (window.__analyticsLoaded) return;
    window.__analyticsLoaded = true;
    // inject GA/Plausible script tag here, or call its init
}
```
On app load, if consent was previously `accepted`, call `enableAnalytics()` so returning visitors keep analytics without re-prompting:
```jsx
useEffect(() => {
    if (localStorage.getItem('np_cookie_consent') === 'accepted') enableAnalytics();
}, []);
```

> If you use **no** non-essential cookies at all (no analytics/marketing), you can use a simpler "notice" banner (acknowledge-only) rather than accept/decline — but as soon as you add analytics, the accept/decline + gating is required.

---

## PART D — GDPR right to erasure (data deletion)

### Key principle: anonymize, don't hard-delete

You **cannot** simply delete a customer who has orders — order/invoice records must be retained for tax/accounting law (which is a lawful basis that overrides erasure for those records). The correct approach: **anonymize the personal data** (name, email, address, phone) while keeping the order rows for legal retention. Your order_item product snapshots already make orders self-contained; do the same for customer PII.

### D.1 Backend — anonymize service

```java
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public void anonymizeCustomer(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (user.getRole() != Role.CUSTOMER) {
        throw new RuntimeException("Only customer accounts can be anonymized");
    }

    String anonEmail = "deleted-" + user.getId() + "@anonymized.local";

    // Replace PII on the user
    user.setFirstName("Deleted");
    user.setLastName("User");
    user.setEmail(anonEmail);
    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // unusable
    user.setPhone(null);
    user.setActive(false);
    user.setEmailVerified(false);
    // clear OAuth linkage / notification opt-ins if present
    userRepository.save(user);

    // Anonymize saved addresses
    addressRepository.findByUserId(userId).forEach(addr -> {
        addr.setStreet("[removed]");
        addr.setCity("[removed]");
        addr.setPostalCode("[removed]");
        // keep country if needed for tax stats, else "[removed]"
        addressRepository.save(addr);
    });

    // Orders: keep the records (legal retention). If order stores customer name/email
    // snapshot, anonymize those snapshot fields too:
    orderRepository.findByUserId(userId).forEach(order -> {
        order.setCustomerFullName("Deleted User");
        order.setCustomerEmail(anonEmail);
        order.setCustomerPhone(null);
        orderRepository.save(order);
    });

    // Remove non-essential personal artifacts entirely:
    favoriteRepository.deleteByUserId(userId);
    reviewRepository.anonymizeByUserId(userId);   // or detach author name
    cartRepository.deleteByUserId(userId);
}
```

> Adapt to your actual entities/fields. The rule: **erase identifying data**, **retain what law requires** (invoices/orders) in anonymized form.

### D.2 Controller (admin)

```java
@DeleteMapping("/{id}/personal-data")
public ResponseEntity<Void> anonymize(@PathVariable Long id) {
    userService.anonymizeCustomer(id);
    return ResponseEntity.noContent().build();
}
```

### D.3 AdminUsers — "Delete personal data" action

In the user-management screen, add a button (ADMIN only) with a strong confirmation:

```jsx
const handleAnonymize = async (id) => {
    if (!window.confirm(t('admin.confirmAnonymize'))) return;
    try {
        await anonymizeCustomer(id);
        toast.success(t('admin.customerAnonymized'));
        fetchUsers();
    } catch (e) {
        toast.error(e.response?.data?.message || t('admin.anonymizeFailed'));
    }
};
```

> Irreversible — require an explicit confirm. Log who performed it and when (audit trail) if you can.

### D.4 Self-service request (minimum viable)

At minimum, the Privacy Policy must tell users how to request erasure (email [contact]). The admin action above fulfils the request manually. A self-service "delete my account" button on the account page is a nice later addition that calls the same anonymize logic for the logged-in user.

### D.5 Documented process

Write an internal note (even a README section) describing: how an erasure request arrives (email), how you verify the requester's identity, that you anonymize via the admin action, what's retained (orders/invoices) and why (legal retention), and the timeframe (GDPR = without undue delay, within 1 month).

---

## PART E — Other GDPR touchpoints

- **Registration consent:** a checkbox (or clear notice) linking to the Privacy Policy at signup. For newsletter/marketing, a **separate opt-in** checkbox (not pre-ticked) — marketing consent must be explicit.
- **Broadcast/newsletter unsubscribe:** every marketing email needs an unsubscribe link; honor opt-outs (you have `notifications_*` fields — wire an unsubscribe endpoint).
- **Data export (portability):** optional but ideal — an admin (or self-service) export of a user's data as JSON. Lower priority than erasure for launch.

---

## PART F — i18n keys (en.json / sr.json)

```jsonc
// en.json
"legal": {
    "privacyTitle": "Privacy Policy",
    "termsTitle": "Terms of Service",
    "returnsTitle": "Return Policy",
    "imprintTitle": "Imprint"
},
"cookie": {
    "message": "We use cookies to run the shop and, with your consent, to improve it.",
    "accept": "Accept",
    "decline": "Decline",
    "learnMore": "Learn more"
},
"admin": {
    "confirmAnonymize": "Permanently anonymize this customer's personal data? This cannot be undone.",
    "customerAnonymized": "Customer data anonymized",
    "anonymizeFailed": "Failed to anonymize customer"
}

// sr.json
"legal": {
    "privacyTitle": "Politika privatnosti",
    "termsTitle": "Uslovi korišćenja",
    "returnsTitle": "Politika povraćaja",
    "imprintTitle": "Impresum"
},
"cookie": {
    "message": "Koristimo kolačiće za rad prodavnice i, uz vašu saglasnost, za njeno poboljšanje.",
    "accept": "Prihvati",
    "decline": "Odbij",
    "learnMore": "Saznaj više"
},
"admin": {
    "confirmAnonymize": "Trajno anonimizovati lične podatke ovog kupca? Ovo se ne može poništiti.",
    "customerAnonymized": "Podaci kupca anonimizovani",
    "anonymizeFailed": "Anonimizacija nije uspela"
}
```

---

## G. Edge cases & notes

- **Anonymize, don't delete:** orders/invoices must survive for tax/accounting law; erase only identifying fields. Hard-deleting a customer with orders would break records and may violate retention law.
- **Only customers:** don't anonymize ADMIN/EMPLOYEE via this action (guard by role).
- **Irreversible + audited:** require confirmation; log who/when.
- **Analytics gated on consent:** never load non-essential cookies before the user accepts; remember the choice; re-enable for returning accepters without re-prompting.
- **Consent ≠ pre-ticked:** marketing opt-in must be an explicit, unticked checkbox.
- **Legal text needs review:** the page skeletons are placeholders — get the actual wording reviewed for Serbia/EU.
- **Footer everywhere:** legal links in the footer should appear on every page.

---

## H. Checklist

- [ ] `LegalPage` layout component
- [ ] Privacy Policy, Terms, Returns, Imprint pages + routes (content placeholders to be legally reviewed)
- [ ] Footer links to all four
- [ ] Cookie consent banner (library or custom + localStorage)
- [ ] Analytics gated behind consent; remembered for returning visitors
- [ ] Backend: `anonymizeCustomer` (PII erased, orders retained-anonymized, customer-only, ADMIN-only)
- [ ] Admin endpoint + AdminUsers "Delete personal data" action with confirm
- [ ] Privacy Policy documents how to request erasure (contact)
- [ ] Documented internal erasure process
- [ ] Registration: privacy notice + separate (unticked) marketing opt-in
- [ ] Marketing emails: unsubscribe link honored
- [ ] i18n keys (EN + SR)
- [ ] Legal content reviewed by a professional before launch
