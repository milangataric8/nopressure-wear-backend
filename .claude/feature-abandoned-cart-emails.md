# Feature: Abandoned Cart Recovery Emails

NoPressure Wear — a scheduled job that finds carts left with items for X hours and emails the owner a reminder. A proven way to recover a share of would-be-lost orders.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, existing `EmailService` + `getEmailTranslations`, Spring `@Scheduled`. Package root: `rs.nopressurewear`.

> **Scope:** works for **logged-in users** (we have their email). Guest carts have no email unless captured at checkout, so they're out of scope unless you capture email earlier. **One reminder per abandoned cart** (don't nag repeatedly). Default: remind after **4 hours** of inactivity, send **once**.

---

## 1. Track cart activity + reminder state (Flyway)

```sql
ALTER TABLE cart ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE cart ADD COLUMN reminder_sent_at TIMESTAMP;   -- null = not reminded
```

- `updated_at` — bumped whenever the cart changes (add/remove/update item). If you have JPA auditing (`@LastModifiedDate`) wire it; otherwise set it manually in cart-mutating service methods.
- `reminder_sent_at` — set when a reminder is sent, so each abandoned cart is emailed once.

```java
// Cart.java
@Column(name = "updated_at")
private LocalDateTime updatedAt;

@Column(name = "reminder_sent_at")
private LocalDateTime reminderSentAt;
```

Bump `updatedAt = LocalDateTime.now()` in every cart-mutating method (addItem, removeItem, updateQuantity, clear-not-needed).

---

## 2. Config

```yaml
cart:
  abandoned:
    enabled: ${ABANDONED_CART_ENABLED:true}
    after-hours: ${ABANDONED_CART_AFTER_HOURS:4}
    # only remind carts updated within this window (don't email months-old carts)
    max-age-hours: ${ABANDONED_CART_MAX_AGE_HOURS:72}
```

---

## 3. Repository query — find abandoned carts

```java
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("""
        SELECT c FROM Cart c
        WHERE c.user IS NOT NULL
          AND c.reminderSentAt IS NULL
          AND c.updatedAt < :idleBefore
          AND c.updatedAt > :notOlderThan
          AND SIZE(c.cartItems) > 0
        """)
    List<Cart> findAbandonedCarts(@Param("idleBefore") LocalDateTime idleBefore,
                                  @Param("notOlderThan") LocalDateTime notOlderThan);
}
```

- `reminderSentAt IS NULL` → not yet reminded.
- `updatedAt < idleBefore` → idle long enough (now − afterHours).
- `updatedAt > notOlderThan` → not ancient (now − maxAgeHours), so you don't email stale carts.
- `SIZE(c.cartItems) > 0` → actually has items.

---

## 4. Scheduled job

```java
@Service
@RequiredArgsConstructor
public class AbandonedCartService {

    private final CartRepository cartRepository;
    private final EmailService emailService;

    @Value("${cart.abandoned.enabled:true}")    private boolean enabled;
    @Value("${cart.abandoned.after-hours:4}")   private int afterHours;
    @Value("${cart.abandoned.max-age-hours:72}") private int maxAgeHours;

    @Scheduled(cron = "0 0 * * * *")   // hourly, on the hour
    @Transactional
    public void sendReminders() {
        if (!enabled) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime idleBefore = now.minusHours(afterHours);
        LocalDateTime notOlderThan = now.minusHours(maxAgeHours);

        List<Cart> carts = cartRepository.findAbandonedCarts(idleBefore, notOlderThan);

        for (Cart cart : carts) {
            try {
                User user = cart.getUser();
                String lang = /* user's preferred lang if stored, else "en" */ "en";
                emailService.sendAbandonedCartEmail(
                        user.getEmail(),
                        user.getFirstName(),
                        cart.getCartItems(),   // for listing items in the email
                        lang
                );
                cart.setReminderSentAt(now);
                cartRepository.save(cart);
            } catch (Exception e) {
                // log + continue; one failure shouldn't stop the batch
            }
        }
    }
}
```

Enable scheduling once on a config class:
```java
@Configuration
@EnableScheduling
public class SchedulingConfig {}
```

---

## 5. EmailService — reminder email (translated)

```java
public void sendAbandonedCartEmail(String to, String firstName, List<CartItem> items, String lang) {
    Map<String, String> t = getEmailTranslations(lang);
    String cartUrl = frontendUrl + "/cart";   // from app.frontend-url

    // build item rows (image + name + qty) similar to buildOrderItemRowsHtml
    String itemRows = /* ... */;

    String html = """
        <!DOCTYPE html><html><head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; background:#f5f5f5; margin:0; padding:0;">
            <div style="max-width:560px; margin:40px auto; background:#fff; border:1px solid #e5e5e5;">
                <div style="padding:32px 40px; border-bottom:1px solid #e5e5e5; text-align:center;">
                    <h1 style="margin:0; font-size:20px; font-weight:900; text-transform:uppercase;">NoPressure wear</h1>
                </div>
                <div style="padding:40px;">
                    <p style="font-size:14px; color:#555;">%s</p>
                    <p style="font-size:14px; color:#555;">%s</p>
                    %s
                    <a href="%s" style="display:inline-block; background:#111; color:#fff; text-decoration:none; font-size:13px; font-weight:600; text-transform:uppercase; letter-spacing:1px; padding:14px 32px; margin-top:24px;">%s</a>
                </div>
            </div>
        </body></html>
        """.formatted(
            t.get("cartHi").formatted(firstName),   // "Hi %s,"
            t.get("cartReminder"),                  // "You left items in your cart…"
            itemRows,
            cartUrl,
            t.get("cartButton")                     // "Return to cart"
        );

    sendHtmlEmail(to, t.get("cartSubject"), html);   // central sender adds signature footer
}
```

### Translation keys (add to getEmailTranslations)
```java
// EN
t.put("cartSubject", "You left something behind");
t.put("cartHi", "Hi %s,");
t.put("cartReminder", "You still have items waiting in your cart. Complete your order before they're gone.");
t.put("cartButton", "Return to cart");
// SR
t.put("cartSubject", "Ostavili ste nešto u korpi");
t.put("cartHi", "Zdravo %s,");
t.put("cartReminder", "Još uvek imate artikle u korpi. Završite porudžbinu pre nego što nestanu.");
t.put("cartButton", "Nazad u korpu");
```

---

## 6. Reset reminder when the cart changes again

If a reminded user adds/removes items later (re-engages), reset `reminderSentAt = null` so a future abandonment can remind again. Do this in the cart-mutating methods alongside the `updatedAt` bump:

```java
cart.setUpdatedAt(LocalDateTime.now());
cart.setReminderSentAt(null);   // re-arm reminders on activity
```

And clear it (or the whole cart) on successful checkout so a completed cart never gets a reminder.

---

## 7. Notes & good practice

- **Once per abandonment:** `reminderSentAt` ensures one email; re-arm on new activity (§6). Avoid nagging — one reminder is respectful and effective; a second after 24h is optional but don't overdo it.
- **Logged-in only:** guests have no stored email. If you want guest recovery, you'd capture email early (e.g. a guest-email step before the cart) — bigger change, out of scope.
- **Respect marketing consent / unsubscribe:** an abandoned-cart email is arguably transactional, but to be safe (GDPR), honor users who opted out of marketing, and include an unsubscribe/preferences link. Tie into your `notifications_*` opt-in fields.
- **Don't email ancient carts:** the `max-age-hours` bound avoids emailing someone about a cart from weeks ago.
- **Batch resilience:** wrap each send in try/catch so one failure doesn't abort the run; log failures.
- **Frequency:** hourly job + a 4h idle window means a user gets reminded ~4–5h after abandoning. Tune via env.
- **Checkout clears it:** ensure successful checkout empties the cart (or sets a flag) so it's never flagged abandoned.

---

## 8. Checklist

- [ ] Flyway: `cart.updated_at` + `cart.reminder_sent_at`
- [ ] Bump `updatedAt` (and reset `reminderSentAt`) on every cart mutation
- [ ] `findAbandonedCarts` repository query (idle, not-ancient, has items, user present, not reminded)
- [ ] `AbandonedCartService` scheduled job (`@EnableScheduling`)
- [ ] `EmailService.sendAbandonedCartEmail` + translation keys (EN/SR)
- [ ] Config: enabled / after-hours / max-age-hours (env-tunable)
- [ ] Re-arm reminder on new cart activity; clear on checkout
- [ ] Respect marketing consent + unsubscribe link
- [ ] try/catch per send
- [ ] Verified: idle cart triggers one email; activity re-arms; checkout prevents reminder; toggle works
