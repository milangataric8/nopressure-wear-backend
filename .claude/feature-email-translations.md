# Feature: Localized Emails (EN / SR) by Selected Language

NoPressure Wear — wire the existing `getEmailTranslations(lang)` map into the email-sending methods.

## Problem

`EmailService.getEmailTranslations(lang)` is **defined but unused**. `sendOrderStatusEmail` and `sendPasswordResetEmail` still hardcode all text in English. This spec connects the translation map to those methods and threads a `lang` parameter from the callers.

> The status value (`CONFIRMED`, `SHIPPED`, …) must stay in its raw English enum form for the **color switch**, while a **translated label** is shown to the customer. Don't translate the value before the switch.

---

## 1. `sendOrderStatusEmail` — add `lang`, use the map

### 1.1 Signature

```java
public void sendOrderStatusEmail(String to,
                                 Long orderId,
                                 String orderCode,
                                 String status,
                                 String customerFirstName,
                                 String productRows,
                                 String subtotal,
                                 String shippingStreet,
                                 String shippingCity,
                                 String shippingPostalCode,
                                 String shippingCountry,
                                 List<String> productImageUrls,
                                 String lang) {          // ← new
```

### 1.2 Resolve translations + status label (top of method)

```java
Map<String, String> t = getEmailTranslations(lang);

// raw status drives the color; translated label is what the user sees
String statusColor = switch (status) {
    case "CONFIRMED" -> "#2563eb";
    case "SHIPPED"   -> "#7c3aed";
    case "DELIVERED" -> "#16a34a";
    case "CANCELLED" -> "#dc2626";
    default          -> "#d97706";
};
String statusLabel = t.getOrDefault(status, status);
```

### 1.3 Replace the hardcoded strings

**Greeting** — was `Dear <strong>%s</strong> ... updated to %s`:

```java
String greeting = """
    <p class="status-badge" style="color: #111; margin: 10px 0 10px;">
        %s<br><br>
        %s
        <span style="font-weight: 700; color: %s;">%s</span>.
    </p>
    """.formatted(
        t.get("orderHi").formatted(customerFirstName),   // "Hi %s," / "Zdravo %s,"
        t.get("orderStatusUpdate").formatted(orderCode),  // "Your order #%s ... updated to:"
        statusColor,
        statusLabel                                       // translated status
    );
```

**Shipping section** — replace `"Shipping Address"`:

```java
String shippingSection = (nonNull(shippingStreet) && !shippingStreet.isEmpty()) ? """
    <div style="margin-top: 24px; padding-top: 24px; border-top: 1px solid #e5e5e5;">
        <p style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; color: #111; margin: 0 0 8px;">%s</p>
        <p style="font-size: 13px; color: #555; margin: 0; line-height: 1.6;">%s<br>%s, %s<br>%s</p>
    </div>
    """.formatted(t.get("orderShipping"), shippingStreet, shippingCity, shippingPostalCode, shippingCountry) : "";
```

**In the main HTML** — replace these literals:
- `<h2 class="order-title">Order #%s</h2>` → keep `%s` for code, but the label "Order" comes from `t.get("orderTitle")` if you want it translated. Simplest: `<h2 class="order-title">%s #%s</h2>` and pass `t.get("orderTitle"), orderCode`.
- `<p class="order-date">Status updated</p>` → `t.get("orderTitle")` or a dedicated key (see §3).
- `<p class="section-title">Items</p>` → `t.get("orderItems")`
- `<span>Subtotal</span>` → `t.get("orderSubtotal")` *(new key — see §3)*
- `<span>Delivery</span>` / `Free` → `t.get("orderDelivery")` / `t.get("orderFree")` *(new keys — see §3)*
- `<div class="summary-total"><span>Total</span>` → `t.get("orderTotal")`
- `<a href="%s" class="button">View Order</a>` → `t.get("orderViewButton")`

Add the new `%s` slots to the `.formatted(...)` call in the same order they appear in the template.

### 1.4 Subject line

```java
helper.setSubject(t.get("orderSubject").formatted(orderCode, statusLabel));
// EN: "Order #%s — Status: %s"   SR: "Porudžbina #%s — Status: %s"
```

(remove the old `"Your order #" + orderCode + " is now " + status`)

---

## 2. `sendPasswordResetEmail` — add `lang`, use the map

### 2.1 Signature

```java
public void sendPasswordResetEmail(String to, String token, String lang) {   // ← new lang
    Map<String, String> t = getEmailTranslations(lang);
    String resetUrl = "http://localhost:5173/reset-password?token=" + token;
```

### 2.2 Replace hardcoded text in the template

- Body paragraph "You requested a password reset…" → `t.get("resetText")`
- "This link expires in 1 hour" → `t.get("resetExpire")`
- Button "Reset Password" → `t.get("resetButton")`
- "If you did not request this…" → `t.get("resetIgnore")`
- Optionally a title/heading → `t.get("resetTitle")`, greeting `t.get("resetHi")`

```java
.formatted(
    t.get("resetText"),
    t.get("resetExpire"),
    resetUrl,
    t.get("resetButton"),
    t.get("resetIgnore")
)
```

### 2.3 Subject

```java
sendHtmlEmail(to, t.get("resetSubject"), html);
```

---

## 3. Missing translation keys to ADD to `getEmailTranslations`

The summary section uses words not yet in the map. Add them to **both** branches:

```java
// EN branch
t.put("orderSubtotal", "Subtotal");
t.put("orderDelivery", "Delivery");
t.put("orderFree", "Free");
t.put("orderStatusUpdatedNote", "Status updated");

// SR branch
t.put("orderSubtotal", "Međuzbir");
t.put("orderDelivery", "Dostava");
t.put("orderFree", "Besplatno");
t.put("orderStatusUpdatedNote", "Status ažuriran");
```

> Existing keys already present and reusable: `orderHi`, `orderStatusUpdate`, `orderItems`, `orderShipping`, `orderTotal`, `orderViewButton`, `orderSubject`, `orderTitle`, and status labels `PENDING`/`CONFIRMED`/`SHIPPED`/`DELIVERED`/`CANCELLED`.
> Note `orderStatusUpdate` is `"Your order <strong>#%s</strong> status has been updated to:"` — it takes the order code as `%s`. Make sure the greeting passes `orderCode` to it (done in §1.2).

---

## 4. Thread `lang` from callers

### 4.1 OrderService

`updateStatus`, `checkout`, `guestCheckout` already (or should) accept `lang`. Pass it into the email call:

```java
emailService.sendOrderStatusEmail(
    order.getCustomerEmail(),
    order.getId(),
    order.getOrderCode(),
    order.getStatus().name(),
    order.getCustomerFullName(),
    productRows,
    order.getTotalAmount().toString(),
    street, city, postalCode, country,
    productImageUrls,
    lang                         // ← pass through
);
```

### 4.2 Password reset flow (AuthService / UserService)

The reset request endpoint should accept a `lang` param (query param or body) and pass it down:

```java
emailService.sendPasswordResetEmail(user.getEmail(), token, lang);
```

### 4.3 Controllers — accept `lang`

```java
@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req,
                                        @RequestParam(defaultValue = "en") String lang) {
    authService.requestPasswordReset(req.getEmail(), lang);
    return ResponseEntity.ok().build();
}
```

(order status controller already receives `lang` per the earlier status-email work; verify it forwards to `updateStatus`)

### 4.4 Frontend — send current language

In the relevant API calls (`orderApi.js`, `authApi.js`), append the active i18n language:

```javascript
import i18n from '../i18n/i18n';

// order status update
export const updateOrderStatus = (orderId, status) =>
    axiosInstance.patch(`/orders/${orderId}/status`, { status }, {
        params: { lang: i18n.language }
    });

// forgot password
export const forgotPassword = (email) =>
    axiosInstance.post('/auth/forgot-password', { email }, {
        params: { lang: i18n.language }
    });
```

---

## 5. Notes

- **Contact emails** (`sendContactEmail`, `sendContactConfirmation`) already handle `lang` inline — leave as-is, or migrate them to `getEmailTranslations` later for consistency (optional, not required).
- **Notification/broadcast email** has no translatable static body (admin writes the content), so no change needed beyond the footer.
- **Signature footer** (`buildSignatureFooter` / tagline) stays as-is; the tagline comes from `store_tagline` setting. If you want a bilingual footer headline, add keys and pass `lang` into `injectSignature` — optional.
- Keep the **status color switch** keyed on the raw English enum; only the displayed label is translated.

---

## 6. Checklist

- [ ] `sendOrderStatusEmail` takes `lang`, uses `getEmailTranslations`, translated status label, translated subject
- [ ] `sendPasswordResetEmail` takes `lang`, uses translation keys + translated subject
- [ ] New keys added: `orderSubtotal`, `orderDelivery`, `orderFree`, `orderStatusUpdatedNote` (EN + SR)
- [ ] OrderService passes `lang` into the email call (updateStatus / checkout / guestCheckout)
- [ ] Password reset flow + controller accept and forward `lang`
- [ ] Frontend sends `i18n.language` on status update + forgot-password calls
- [ ] Verified SR email renders translated text in Mailtrap
