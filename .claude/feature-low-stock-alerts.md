# Feature: Low-Stock Admin Alerts

NoPressure Wear — automatically notify the admin when a product's stock drops below a threshold, so you restock before selling out. Turns the existing low-stock *report* (pull) into a *push* alert.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, existing `EmailService` + notification/broadcast infrastructure. Package root: `rs.nopressurewear`.

> **Trigger model (assumption — adjust in §6):** check at the moment stock is decremented (checkout). When a product (or size variant, if sizes implemented) crosses from above the threshold to at/below it, fire **one** alert (don't spam on every subsequent order while it stays low). Threshold is admin-configurable.

---

## 1. Settings — threshold + toggle (Flyway)

```sql
INSERT INTO store_setting (key, value, label) VALUES
    ('low_stock_alerts_enabled', 'true', 'Low-Stock Alerts'),
    ('low_stock_threshold', '5', 'Low-Stock Threshold');
```

`low_stock_threshold` = stock level at or below which an alert fires.

---

## 2. Track "already alerted" to avoid spam

Add a flag so each low-stock crossing alerts only once until restocked above the threshold.

```sql
-- Flyway (product-level stock)
ALTER TABLE product ADD COLUMN low_stock_alerted BOOLEAN NOT NULL DEFAULT FALSE;

-- If SIZE variants are implemented, track per variant instead/also:
-- ALTER TABLE product_variant ADD COLUMN low_stock_alerted BOOLEAN NOT NULL DEFAULT FALSE;
```

Logic: when stock drops to ≤ threshold and `low_stock_alerted = false` → send alert, set `true`. When stock is restocked above threshold (admin update) → reset to `false`, so a future drop alerts again.

---

## 3. Service — check on stock decrement

In `OrderService` checkout/guestCheckout (and the size-variant decrement if applicable), after reducing stock:

```java
@RequiredArgsConstructor
@Service
public class LowStockService {

    private final StoreSettingsRepository settingsRepository;
    private final EmailService emailService;
    private final ProductRepository productRepository;

    @Value("${spring.mail.username}")
    private String adminEmail;   // or a dedicated app.admin-email

    public void checkAndAlert(Product product) {
        if (!alertsEnabled()) return;
        int threshold = threshold();
        Integer stock = product.getStockQuantity();   // or sum of variants if sized
        if (stock == null) return;

        if (stock <= threshold && !product.isLowStockAlerted()) {
            sendAlert(product, stock, threshold);
            product.setLowStockAlerted(true);
            productRepository.save(product);
        } else if (stock > threshold && product.isLowStockAlerted()) {
            product.setLowStockAlerted(false);   // restocked → re-arm
            productRepository.save(product);
        }
    }

    private void sendAlert(Product product, int stock, int threshold) {
        String subject = "Low stock: " + product.getName();
        String body = """
            <p>Stock alert for <strong>%s</strong> (SKU: %s).</p>
            <p>Current stock: <strong>%d</strong> (threshold: %d).</p>
            <p>Consider restocking soon.</p>
            """.formatted(product.getName(), product.getSku(), stock, threshold);
        emailService.sendAdminAlert(adminEmail, subject, body);
    }

    private boolean alertsEnabled() {
        return settingsRepository.findByKey("low_stock_alerts_enabled")
                .map(s -> !"false".equalsIgnoreCase(s.getValue())).orElse(true);
    }
    private int threshold() {
        return settingsRepository.findByKey("low_stock_threshold")
                .map(s -> { try { return Integer.parseInt(s.getValue().trim()); } catch (Exception e) { return 5; } })
                .orElse(5);
    }
}
```

Call `lowStockService.checkAndAlert(product)` right after each stock decrement at checkout.

> The `re-arm` step (reset the flag when restocked) is best done in `ProductService.update` when an admin raises stock above the threshold — call `checkAndAlert` there too, or reset the flag directly.

---

## 4. EmailService — a simple admin alert method

```java
public void sendAdminAlert(String to, String subject, String bodyHtml) {
    String html = """
        <div style="font-family: Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 24px;">
            <h2 style="font-size: 18px; color: #111;">%s</h2>
            <div style="font-size: 14px; color: #333; line-height: 1.6;">%s</div>
        </div>
        """.formatted(subject, bodyHtml);
    sendHtmlEmail(to, subject, html);   // reuses your central sender (+ signature footer)
}
```

> Recipient: a dedicated admin/ops email is better than the SMTP `username`. Add `app.admin-email` config and use it.

---

## 5. Optional — in-app admin notification (instead of / in addition to email)

If you want a bell/notification list in the admin UI rather than email:

```sql
CREATE TABLE admin_notification (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,        -- 'LOW_STOCK'
    message VARCHAR(500) NOT NULL,
    product_id BIGINT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```
- Insert a row in `sendAlert` instead of (or alongside) email.
- Admin endpoint `GET /api/admin/notifications?read=false` + a bell badge in the admin header.
- Mark-as-read endpoint.

Email is the simplest for launch; in-app is nicer long-term. Start with email.

---

## 6. Decisions & notes

- **One alert per crossing:** the `low_stock_alerted` flag prevents an alert on every order while stock stays low; it re-arms when restocked above threshold.
- **Sized products:** if SIZE variants are implemented, decide whether the threshold is per-size (alert when any size is low) or on the total. Per-size is more useful for apparel (you can run out of M while L is fine) — track the flag on `product_variant` then.
- **Trigger point:** checkout decrement is the natural trigger. You could also run a daily scheduled sweep as a backstop (find all products ≤ threshold not yet alerted) — optional.
- **Recipient:** use a real ops/admin email (`app.admin-email`), not the SMTP login.
- **Threshold + toggle** are admin-editable in Settings (reuse the numeric-input render branch pattern from the delivery spec).
- **Don't block checkout:** wrap the alert in try/catch — a mail failure must never fail the order.

---

## 7. Checklist

- [ ] Flyway: `low_stock_alerts_enabled`, `low_stock_threshold` settings
- [ ] Flyway: `low_stock_alerted` flag on product (or product_variant if sized)
- [ ] `LowStockService.checkAndAlert` (send once per crossing, re-arm on restock)
- [ ] Called after stock decrement at checkout/guestCheckout
- [ ] Re-arm on admin restock (ProductService.update)
- [ ] `EmailService.sendAdminAlert` + real admin-email config
- [ ] Settings UI: threshold input + toggle
- [ ] (Optional) in-app admin_notification table + bell
- [ ] Alert wrapped in try/catch (never fails the order)
- [ ] Verified: crossing threshold alerts once; restock re-arms; toggle/threshold respected
