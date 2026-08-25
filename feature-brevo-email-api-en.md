# Feature: Email via Brevo API (replacing SMTP)

NoPressure Wear — replace SMTP email sending with HTTP API calls to Brevo, because Railway blocks outbound SMTP ports (both 587 and 2525 time out). HTTP (443) is never blocked.

Stack: Spring Boot 4.1 / Java 25, existing `EmailService` with HTML templates and `getEmailTranslations(lang)`.

> **Problem being solved:** `MailConnectException: Couldn't connect to host, port: sandbox.smtp.mailtrap.io, 2525` — Railway blocks SMTP as an anti-spam measure. On top of that, Mailtrap sandbox doesn't deliver to real customers anyway, so moving to a real service is required before launch.

---

## Approach: transport abstraction

Same pattern as `StorageProvider` (local disk vs Cloudinary): one interface, two implementations, selected by profile.

- **Local (`local`)** → SMTP via Mailtrap (works, convenient for dev, mail lands in the test inbox)
- **Production (`prod`)** → Brevo HTTP API

All HTML template, translation, and content logic **stays unchanged** — only the sending layer swaps.

---

## 1. Brevo account and key

1. Sign up at brevo.com (free plan).
2. **Settings → SMTP & API → API Keys** → generate an **API key**.

> ⚠️ **Common mistake:** the API key and the SMTP key are two distinct credentials. The API key is used for REST calls (`POST /v3/smtp/email`); the SMTP key is for SMTP relay authentication. Using the API key as an SMTP password does not work. We need the **API key**.

3. **Verify a sender address/domain** (Settings → Senders & Domains). While the domain isn't authenticated (DKIM), Brevo replaces the sending domain with `@brevosend.com` to protect deliverability. To start, verifying just an email address is enough; set up DKIM for `nopressurewear.com` before real production.

**Free plan:** 300 emails per day via API, unlimited contacts, no credit card required. Beyond that, paid plans start at $9/month for 5,000 emails.

---

## 2. Interface

```java
package rs.nopressurewear.service.email;

public interface EmailSender {
    void send(String to, String subject, String htmlContent);
}
```

---

## 3. SMTP implementation (local)

Extract the existing send logic out of `EmailService`:

```java
package rs.nopressurewear.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")                 // local, ci, everything except prod
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("SMTP send failed: " + e.getMessage(), e);
        }
    }
}
```

> `@Profile("!prod")` — same trick as `LocalStorageService`, so it also covers the `ci` profile.

---

## 4. Brevo API implementation (production)

```java
package rs.nopressurewear.service.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Profile("prod")
@Slf4j
@RequiredArgsConstructor
public class BrevoEmailSender implements EmailSender {

    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";

    private final ObjectMapper mapper;

    @Value("${app.brevo.api-key}")
    private String apiKey;

    @Value("${app.brevo.sender-email}")
    private String senderEmail;

    @Value("${app.brevo.sender-name:NoPressure Wear}")
    private String senderName;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void send(String to, String subject, String htmlContent) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("email", senderEmail, "name", senderName),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );
            String body = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                log.error("Brevo send failed [{}]: {}", response.statusCode(), response.body());
                throw new RuntimeException("Brevo API error: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Brevo send failed: " + e.getMessage(), e);
        }
    }
}
```

> Using Jackson to build the JSON body handles escaping automatically — important because the HTML contains quotes and newlines that would break a hand-built JSON string.

---

## 5. EmailService uses the abstraction

In `EmailService`, replace direct `mailSender.send(...)` calls with:

```java
private final EmailSender emailSender;   // instead of JavaMailSender

private void sendHtmlEmail(String to, String subject, String html) {
    emailSender.send(to, subject, html);
}
```

All methods (`sendVerificationEmail`, `sendOrderStatusEmail`, `sendPasswordResetEmail`, `sendAbandonedCartEmail`, `sendAdminAlert`) still build the same HTML — they just route through `sendHtmlEmail`.

---

## 6. Configuration

### `application-prod.yml`
```yaml
app:
  brevo:
    api-key: ${BREVO_API_KEY}
    sender-email: ${BREVO_SENDER_EMAIL}
    sender-name: ${BREVO_SENDER_NAME:NoPressure Wear}
```

> **Remove `spring.mail.*` from prod config** — SMTP isn't used in production. If you leave it, the `JavaMailSender` bean is still created (harmless), but it's cleaner without. If you remove it, make sure no bean depends directly on `JavaMailSender` under the prod profile.

### Railway environment variables
```
BREVO_API_KEY=xkeysib-...
BREVO_SENDER_EMAIL=noreply@nopressurewear.com     # or a verified address
BREVO_SENDER_NAME=NoPressure Wear
```
Remove `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (unless keeping the SMTP bean around).

### Local (`application-local.yml`) — unchanged
Mailtrap SMTP keeps working for development.

---

## 7. Testing

**Quick API key test** (before deploying), from Git Bash:
```bash
curl -X POST https://api.brevo.com/v3/smtp/email \
  -H "api-key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": {"email": "verified@address.com", "name": "NoPressure"},
    "to": [{"email": "you@email.com"}],
    "subject": "Test",
    "htmlContent": "<p>It works!</p>"
  }'
```
Success returns `201` with a `messageId`. `401` — wrong key; `400` — sender not verified.

**After deploy:** register on the site → the email should arrive at the real address (not Mailtrap) → click the verify link.

---

## 8. Notes and gotchas

- **API key ≠ SMTP key** — generate the API key under Settings → SMTP & API.
- **Sender must be verified**, otherwise Brevo rejects the send or rewrites the domain.
- **DKIM for the domain** — until the domain is authenticated, mail goes out as `@brevosend.com` with weaker deliverability. Set up the DNS records once the domain is live.
- **300/day limit** on the free plan — enough to start; watch usage.
- **Keep the try/catch in `register()`** — if Brevo fails, the user is still created and can request a resend.
- **Marketing emails** — Brevo also covers newsletters/campaigns, so the same account serves the broadcast/notifications feature in the admin panel. Transactional and marketing mail go through separate streams to protect deliverability.
- **Never log the API key** — treat it like any other secret (env var, never in code).

---

## 9. Checklist

- [ ] Brevo account + **API key** (not the SMTP key)
- [ ] Sender email/domain verified
- [ ] `EmailSender` interface
- [ ] `SmtpEmailSender` (`@Profile("!prod")`) — existing SMTP logic extracted
- [ ] `BrevoEmailSender` (`@Profile("prod")`) — HTTP POST to `/v3/smtp/email` via Jackson-built JSON
- [ ] `EmailService` uses `EmailSender` instead of `JavaMailSender`
- [ ] `application-prod.yml`: `app.brevo.*` config
- [ ] Railway: `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `BREVO_SENDER_NAME`
- [ ] curl key test passes (201)
- [ ] Registration in production sends a real email
- [ ] (Later) DKIM for `nopressurewear.com`
