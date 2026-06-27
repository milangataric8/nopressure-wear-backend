# Feature: Adjustable Delivery Cost (Admin Settings → Delivery)

NoPressure Wear — let the admin configure delivery cost from a **Delivery** section in AdminSettings: a flat delivery fee, an optional free-shipping threshold, and an on/off switch. The fee flows through the order total, the Stripe charge, the confirmation email, and the invoice PDF.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, React + Vite + Tailwind, Stripe, react-i18next (EN/SR). Base currency: **RSD** (display converted per language; Stripe charges base currency). Package root: `rs.nopressurewear`.

> **Model (assumption — adjust in §13):** flat delivery fee + free-shipping threshold.
> - `delivery_enabled` ON/OFF — whether any fee applies.
> - `delivery_fee` — flat amount in base currency (RSD).
> - `free_shipping_threshold` — if subtotal ≥ this, delivery is free; `0`/empty = no free-shipping rule.
> By-region / by-weight is a later extension (noted in §13). Flat + threshold covers most webshops.

---

## 1. Settings — Flyway migration

```sql
INSERT INTO store_setting (key, value, label) VALUES
    ('delivery_enabled', 'true', 'Delivery Enabled'),
    ('delivery_fee', '400', 'Delivery Fee'),
    ('free_shipping_threshold', '5000', 'Free Shipping Threshold');
```

Amounts are in **base currency (RSD)**. Defaults shown are examples — admin edits them. `free_shipping_threshold = 0` disables free shipping.

---

## 2. Order entity — snapshot the delivery fee

The fee charged must be stored on the order so historical orders keep their value even if the admin later changes the setting.

```sql
-- Flyway
ALTER TABLE orders ADD COLUMN delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 0;
```
```java
// Order.java
@Column(name = "delivery_fee", nullable = false)
private BigDecimal deliveryFee = BigDecimal.ZERO;
```

> `total_amount` will now = items subtotal (after discounts/coupon) **+ delivery_fee**. If you currently store only `totalAmount`, that's fine — just make sure it includes delivery (see §5). Optionally also store a `subtotal` column if you want both broken out on the invoice.

---

## 3. Backend — compute the delivery fee

Add a small service (or method in `OrderService`) that reads the settings and computes the fee for a given subtotal. Uses your existing `StoreSettingsRepository.findByKey(...)`.

```java
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final StoreSettingsRepository storeSettingsRepository;

    public BigDecimal calculateDeliveryFee(BigDecimal subtotal) {
        boolean enabled = getBool("delivery_enabled", true);
        if (!enabled) return BigDecimal.ZERO;

        BigDecimal threshold = getDecimal("free_shipping_threshold", BigDecimal.ZERO);
        if (threshold.compareTo(BigDecimal.ZERO) > 0
                && subtotal.compareTo(threshold) >= 0) {
            return BigDecimal.ZERO;   // qualifies for free shipping
        }
        return getDecimal("delivery_fee", BigDecimal.ZERO);
    }

    public BigDecimal getDeliveryFeeSetting() {
        return getDecimal("delivery_fee", BigDecimal.ZERO);
    }

    public BigDecimal getFreeShippingThreshold() {
        return getDecimal("free_shipping_threshold", BigDecimal.ZERO);
    }

    public boolean isDeliveryEnabled() {
        return getBool("delivery_enabled", true);
    }

    private boolean getBool(String key, boolean def) {
        return storeSettingsRepository.findByKey(key)
                .map(s -> !"false".equalsIgnoreCase(s.getValue()))
                .orElse(def);
    }

    private BigDecimal getDecimal(String key, BigDecimal def) {
        return storeSettingsRepository.findByKey(key)
                .map(s -> {
                    try { return new BigDecimal(s.getValue().trim()); }
                    catch (Exception e) { return def; }
                })
                .orElse(def);
    }
}
```

---

## 4. AdminSettings — Delivery section

### 4.1 Add the section
```jsx
{ key: 'delivery', title: t('settings.delivery'),
  keys: ['delivery_enabled', 'delivery_fee', 'free_shipping_threshold'] }
```

### 4.2 Toggle key
Add `'delivery_enabled'` to the boolean-toggle `.includes(setting.key)` render-chain list (same list as `contact_enabled`, `add_to_cart_enabled`, etc.).

### 4.3 Numeric inputs for fee + threshold
`delivery_fee` and `free_shipping_threshold` are numbers, not toggles — add a render branch (place it before the toggle branch, alongside your other special inputs like `auth_bg_color`):

```jsx
{['delivery_fee', 'free_shipping_threshold'].includes(setting.key) ? (
    <div className="flex-1 flex items-center gap-2">
        <input
            type="number"
            min="0"
            step="1"
            value={setting.value}
            onChange={async (e) => {
                const val = e.target.value;
                try {
                    await updateSettings(setting.id, val);
                    setSettings(prev => prev.map(s => s.id === setting.id ? { ...s, value: val } : s));
                    window.dispatchEvent(new Event('settings-updated'));
                } catch (err) {
                    toast.error(err.response?.data?.message || t('messages.failedToUpdate'));
                }
            }}
            onBlur={() => { toast.success(t('messages.settingUpdated')); fetchSettings(); }}
            className="w-32 border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black"
        />
        <span className="text-xs text-gray-400">RSD</span>
        {setting.key === 'free_shipping_threshold' && (
            <span className="text-xs text-gray-400 ml-1">{t('settings.freeShippingHint')}</span>
        )}
    </div>
) : /* ...existing branches (auth_bg_color, toggles, etc.)... */ }
```

> Reminder: every key must be in BOTH the `sections` array AND its render branch (toggle list or the numeric branch above), or it falls through to a plain text field.

---

## 5. OrderService — apply delivery at checkout

In both `checkout` and `guestCheckout`, after computing the items subtotal (post-discount, post-coupon) and before saving:

```java
BigDecimal subtotal = /* sum of item priceAtPurchase * qty, after coupon */;
BigDecimal deliveryFee = deliveryService.calculateDeliveryFee(subtotal);

order.setDeliveryFee(deliveryFee);
order.setTotalAmount(subtotal.add(deliveryFee));   // total now includes delivery
```

Inject `DeliveryService` into `OrderService`. Keep the existing coupon logic — apply coupon to the subtotal first, then compute delivery on the discounted subtotal (so a coupon can push the order over the free-shipping threshold).

---

## 6. StripeService — charge subtotal + delivery

Stripe must charge the same total the customer sees. Wherever you build the PaymentIntent amount, add the delivery fee:

```java
BigDecimal subtotal = /* discounted subtotal */;
BigDecimal deliveryFee = deliveryService.calculateDeliveryFee(subtotal);
BigDecimal total = subtotal.add(deliveryFee);

// convert to Stripe's smallest unit as you already do, using `total`
long amount = total.multiply(BigDecimal.valueOf(100)).longValueExact();
```

> Stripe still charges in **base currency** (display conversion is separate). Ensure the PaymentIntent amount = subtotal + delivery so the webhook-confirmed charge matches the order total.

---

## 7. Confirmation email — show real delivery (replace hardcoded "Free")

Your order email currently hardcodes `Delivery: Free`. Pass the real value through and render it.

In the email builder (`sendOrderStatusEmail` / `buildOrderItemRowsHtml` caller), accept the delivery fee + subtotal and replace the summary block:

```java
String deliveryLabel = deliveryFee.compareTo(BigDecimal.ZERO) == 0
        ? t.get("orderFree")                       // "Free" / "Besplatno"
        : deliveryFee.toPlainString() + " RSD";

// summary HTML:
<div class="summary-row"><span>%s</span><span>%s RSD</span></div>   <!-- Subtotal -->
<div class="summary-row"><span>%s</span><span>%s</span></div>       <!-- Delivery -->
<div class="summary-total"><span>%s</span><span>%s RSD</span></div> <!-- Total -->
```
formatted with subtotal, the delivery label, and total. Use the translation keys `orderSubtotal`, `orderDelivery`, `orderFree`, `orderTotal` (added in the email-translations spec). The order already stores `deliveryFee` and `totalAmount`, so derive subtotal as `total - deliveryFee` if you don't store it separately.

---

## 8. Invoice PDF — delivery line

In `ReportService.generateInvoicePdf(order, lang)`, add a Delivery row to the totals section, between Subtotal and Total:

```java
BigDecimal delivery = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
BigDecimal subtotal = order.getTotalAmount().subtract(delivery);

// Subtotal row: subtotal
// Delivery row: delivery == 0 ? translations.get("free") : delivery + " RSD"
// Total row: order.getTotalAmount()
```
Use the invoice translation map (`getInvoiceTranslations`) for the labels so EN/SR both render.

---

## 9. Cart / Checkout frontend — show delivery + threshold nudge

The cart order-summary should display the delivery line and the running total, and ideally a "spend X more for free shipping" nudge. The **backend is the source of truth**; the frontend shows an estimate that mirrors the same rules.

Read the settings (you already use `getSettingsMap`):
```jsx
const [delivery, setDelivery] = useState({ enabled: true, fee: 0, threshold: 0 });

useEffect(() => {
    getSettingsMap().then(r => setDelivery({
        enabled: r.data.delivery_enabled !== 'false',
        fee: parseFloat(r.data.delivery_fee || '0'),
        threshold: parseFloat(r.data.free_shipping_threshold || '0'),
    })).catch(() => {});
}, []);
```

Compute (mirror the backend):
```jsx
const subtotal = /* cart subtotal after coupon */;
const deliveryFee = !delivery.enabled ? 0
    : (delivery.threshold > 0 && subtotal >= delivery.threshold) ? 0
    : delivery.fee;
const total = subtotal + deliveryFee;
const remainingForFree = delivery.threshold > 0 ? Math.max(delivery.threshold - subtotal, 0) : 0;
```

Order summary (use your `useCurrency().format()` so it displays in the active currency):
```jsx
<div className="flex justify-between text-sm"><span>{t('cart.subtotal')}</span><span>{format(subtotal)}</span></div>
<div className="flex justify-between text-sm">
    <span>{t('cart.delivery')}</span>
    <span className={deliveryFee === 0 ? 'text-green-600' : ''}>
        {deliveryFee === 0 ? t('cart.free') : format(deliveryFee)}
    </span>
</div>
{remainingForFree > 0 && (
    <p className="text-xs text-gray-500 mt-1">
        {t('cart.freeShippingNudge', { amount: format(remainingForFree) })}
    </p>
)}
<div className="flex justify-between text-sm font-bold border-t border-gray-200 pt-2 mt-2">
    <span>{t('cart.total')}</span><span>{format(total)}</span>
</div>
```

---

## 10. Currency note

`delivery_fee` and `free_shipping_threshold` are stored in **base currency (RSD)**, like product prices. The frontend `useCurrency().format()` converts them for display the same way it converts prices. Stripe charges base currency. No special handling beyond reusing `format()`.

---

## 11. i18n keys (en.json / sr.json)

```jsonc
// en.json
"settings": {
    "delivery": "Delivery",
    "delivery_enabled": "Delivery Enabled",
    "delivery_fee": "Delivery Fee",
    "free_shipping_threshold": "Free Shipping Threshold",
    "freeShippingHint": "Order total above this ships free (0 = off)"
},
"cart": {
    "subtotal": "Subtotal",
    "delivery": "Delivery",
    "free": "Free",
    "total": "Total",
    "freeShippingNudge": "Add {{amount}} more for free shipping"
}

// sr.json
"settings": {
    "delivery": "Dostava",
    "delivery_enabled": "Dostava omogućena",
    "delivery_fee": "Cena dostave",
    "free_shipping_threshold": "Prag za besplatnu dostavu",
    "freeShippingHint": "Iznos porudžbine iznad ovoga ima besplatnu dostavu (0 = isključeno)"
},
"cart": {
    "subtotal": "Međuzbir",
    "delivery": "Dostava",
    "free": "Besplatno",
    "total": "Ukupno",
    "freeShippingNudge": "Dodajte još {{amount}} za besplatnu dostavu"
}
```

(Email/invoice use `orderSubtotal`, `orderDelivery`, `orderFree`, `orderTotal` from the email-translations spec.)

---

## 12. Edge cases & notes

- **Snapshot on the order:** always store `delivery_fee` on the order; never recompute from current settings for past orders.
- **Coupon order of operations:** apply coupon to subtotal first, THEN compute delivery — a coupon can tip the order over the free-shipping threshold.
- **Backend is source of truth:** the frontend shows an estimate; recompute the fee server-side at checkout and charge that via Stripe. Never trust a client-sent delivery amount.
- **Stripe amount must match:** PaymentIntent amount = subtotal + delivery, or the charge won't equal the order total.
- **Delivery off:** `delivery_enabled = false` → fee always 0; cart shows "Free".
- **Threshold = 0:** no free-shipping rule; flat fee always applies (unless delivery disabled).
- **COD orders:** delivery fee applies the same way (it's part of the total the customer pays on delivery).
- **Guest checkout:** identical logic — `guestCheckout` computes and stores the fee too.
- **Numeric setting validation:** guard against non-numeric setting values (the `getDecimal` try/catch falls back to default).

---

## 13. Optional extensions (later)

- **By region/country:** a `delivery_zone` table (country/region → fee); pick the fee by the shipping address. Bigger change to `DeliveryService` + a zones admin screen.
- **By weight:** add weight to products, tiered fees by total weight.
- **Multiple methods:** standard vs express, customer-selected at checkout (each a fee), stored on the order.
Start with flat + threshold; add these only if the business needs them.

---

## 14. Checklist

- [ ] Flyway: `delivery_enabled`, `delivery_fee`, `free_shipping_threshold` settings
- [ ] Flyway: `orders.delivery_fee` column
- [ ] `Order.deliveryFee` field
- [ ] `DeliveryService.calculateDeliveryFee(subtotal)` (+ getters)
- [ ] AdminSettings: Delivery section — toggle + two numeric inputs (in sections AND render branches)
- [ ] OrderService checkout + guestCheckout: compute fee, set on order, total = subtotal + fee
- [ ] StripeService: PaymentIntent amount includes delivery
- [ ] Confirmation email: real delivery line (not hardcoded Free), uses translations
- [ ] Invoice PDF: delivery line
- [ ] Cart frontend: delivery line + free-shipping nudge + total, via useCurrency().format()
- [ ] i18n keys (EN + SR)
- [ ] Verified: fee applies, threshold gives free shipping, coupon interaction, Stripe charge matches total, email + invoice show correct delivery
