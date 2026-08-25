# Feature: Email preko Brevo API-ja (umesto SMTP-a)

NoPressure Wear — zameniti SMTP slanje mejlova HTTP API pozivom ka Brevo, jer Railway blokira odlazne SMTP portove (587 i 2525 oba padaju u timeout). HTTP (443) nikad nije blokiran.

Stack: Spring Boot 4.1 / Java 25, postojeći `EmailService` sa HTML šablonima i `getEmailTranslations(lang)`.

> **Problem koji rešavamo:** `MailConnectException: Couldn't connect to host, port: sandbox.smtp.mailtrap.io, 2525` — Railway blokira SMTP kao antispam meru. Uz to, Mailtrap sandbox ionako ne isporučuje poštu pravim kupcima, pa je prelazak na pravi servis neophodan pre produkcije.

---

## Pristup: apstrakcija transporta

Isti obrazac kao `StorageProvider` (lokalni disk vs Cloudinary): interfejs sa dve implementacije, birane po profilu.

- **Lokalno (`local`)** → SMTP preko Mailtrap-a (radi, zgodno za razvoj, mejlovi u test inboxu)
- **Produkcija (`prod`)** → Brevo HTTP API

Sva logika oko HTML šablona, prevoda i sadržaja **ostaje nepromenjena** — menja se samo sloj koji šalje.

---

## 1. Brevo nalog i ključ

1. Registruj se na brevo.com (besplatan plan).
2. **Settings → SMTP & API → API Keys** → generiši **API key**.

> ⚠️ **Česta greška:** API ključ i SMTP ključ su dva različita kredencijala. API ključ se koristi za REST pozive (`POST /v3/smtp/email`), SMTP ključ za SMTP relay autentifikaciju. Korišćenje API ključa kao SMTP lozinke ne radi. Nama treba **API ključ**.

3. **Verifikuj sender adresu/domen** (Settings → Senders & Domains). Dok domen nije autentifikovan (DKIM), Brevo zamenjuje domen pošiljaoca sa `@brevosend.com`. Za početak možeš verifikovati samo email adresu; za pravu produkciju podesi DKIM za `nopressurewear.com`.

**Besplatni plan:** 300 mejlova dnevno preko API-ja, neograničeni kontakti, bez kreditne kartice. Iznad toga plaćeni planovi počinju od $9/mesec za 5.000 mejlova.

---

## 2. Interfejs

```java
package rs.nopressurewear.service.email;

public interface EmailSender {
    void send(String to, String subject, String htmlContent);
}
```

---

## 3. SMTP implementacija (lokalno)

Izdvoji postojeću logiku slanja iz `EmailService`:

```java
package rs.nopressurewear.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")                 // local, ci, sve osim prod
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

> `@Profile("!prod")` — isti trik kao kod `LocalStorageService`, pokriva i `ci` profil.

---

## 4. Brevo API implementacija (produkcija)

```java
package rs.nopressurewear.service.email;

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

@Service
@Profile("prod")
@Slf4j
public class BrevoEmailSender implements EmailSender {

    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";

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
        String body = """
            {
              "sender": {"email": %s, "name": %s},
              "to": [{"email": %s}],
              "subject": %s,
              "htmlContent": %s
            }
            """.formatted(
                jsonString(senderEmail),
                jsonString(senderName),
                jsonString(to),
                jsonString(subject),
                jsonString(htmlContent)
        );

        try {
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

    // Bezbedno escapovanje u JSON string (HTML sadrzi navodnike, nove redove...)
    private String jsonString(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }
}
```

> **JSON escapovanje je obavezno** — tvoj HTML sadrži navodnike i nove redove; bez escapovanja payload nije validan JSON. Alternativa: koristi Jackson `ObjectMapper` da serijalizuje mapu umesto ručnog string building-a (čistije ako ti je već injektovan).

### Čistija varijanta sa Jackson-om (preporučeno ako imaš ObjectMapper)
```java
private final ObjectMapper mapper;   // injectuj

Map<String, Object> payload = Map.of(
    "sender", Map.of("email", senderEmail, "name", senderName),
    "to", List.of(Map.of("email", to)),
    "subject", subject,
    "htmlContent", htmlContent
);
String body = mapper.writeValueAsString(payload);
```

---

## 5. EmailService koristi apstrakciju

U `EmailService`, zameni direktne `mailSender.send(...)` pozive sa:

```java
private final EmailSender emailSender;   // umesto JavaMailSender

private void sendHtmlEmail(String to, String subject, String html) {
    emailSender.send(to, subject, html);
}
```

Sve metode (`sendVerificationEmail`, `sendOrderStatusEmail`, `sendPasswordResetEmail`, `sendAbandonedCartEmail`, `sendAdminAlert`) i dalje grade isti HTML — samo prolaze kroz `sendHtmlEmail`.

---

## 6. Konfiguracija

### `application-prod.yml`
```yaml
app:
  brevo:
    api-key: ${BREVO_API_KEY}
    sender-email: ${BREVO_SENDER_EMAIL}
    sender-name: ${BREVO_SENDER_NAME:NoPressure Wear}
```

> **Ukloni `spring.mail.*` iz prod konfiguracije** — u produkciji se SMTP ne koristi. Ako ostavi, `JavaMailSender` bean se i dalje pravi (bezopasno), ali je čistije bez njega. Ako ga ukloniš, proveri da nijedan bean ne zavisi direktno od `JavaMailSender` u prod profilu.

### Railway env varijable
```
BREVO_API_KEY=xkeysib-...
BREVO_SENDER_EMAIL=noreply@nopressurewear.com     # ili verifikovana adresa
BREVO_SENDER_NAME=NoPressure Wear
```
Ukloni `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (osim ako ostavljaš SMTP bean).

### Lokalno (`application-local.yml`) — ostaje kako jeste
Mailtrap SMTP nastavlja da radi za razvoj.

---

## 7. Testiranje

**Brzi test API ključa** (pre deploy-a), iz Git Bash-a:
```bash
curl -X POST https://api.brevo.com/v3/smtp/email \
  -H "api-key: TVOJ_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": {"email": "verifikovana@adresa.com", "name": "NoPressure"},
    "to": [{"email": "tvoj@email.com"}],
    "subject": "Test",
    "htmlContent": "<p>Radi!</p>"
  }'
```
Uspeh vraća `201` sa `messageId`. Ako vrati `401` — pogrešan ključ; `400` — sender nije verifikovan.

**Posle deploy-a:** registruj se na sajtu → mejl bi trebalo da stigne na pravu adresu (ne u Mailtrap) → klikni verify link.

---

## 8. Napomene i zamke

- **API ključ ≠ SMTP ključ** — generiši API key u Settings → SMTP & API.
- **Sender mora biti verifikovan**, inače Brevo odbija slanje ili menja domen.
- **DKIM za domen** — dok domen nije autentifikovan, mejlovi idu sa `@brevosend.com` i lošijom isporučivošću. Podesi DNS zapise kad budeš imao domen.
- **Dnevni limit 300** na besplatnom planu — dovoljno za početak, prati potrošnju.
- **try/catch u `register()` ostaje** — ako Brevo padne, korisnik se i dalje kreira i može da traži resend.
- **Marketing mejlovi** — Brevo pokriva i newsletter/kampanje, pa ti isti nalog služi i za onaj broadcast/notifikacije deo iz admin panela. Transakcioni i marketinški mejlovi idu odvojenim tokovima radi isporučivosti.
- **Ne loguj API ključ** — tretiraj ga kao svaku drugu tajnu (env var, nikad u kodu).

---

## 9. Checklist

- [ ] Brevo nalog + **API key** (ne SMTP key)
- [ ] Sender email/domen verifikovan
- [ ] `EmailSender` interfejs
- [ ] `SmtpEmailSender` (`@Profile("!prod")`) — izdvojena postojeća SMTP logika
- [ ] `BrevoEmailSender` (`@Profile("prod")`) — HTTP POST na `/v3/smtp/email`, JSON escapovanje
- [ ] `EmailService` koristi `EmailSender` umesto `JavaMailSender`
- [ ] `application-prod.yml`: `app.brevo.*` konfiguracija
- [ ] Railway: `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `BREVO_SENDER_NAME`
- [ ] curl test ključa prošao (201)
- [ ] Registracija na produkciji šalje pravi mejl
- [ ] (Kasnije) DKIM za `nopressurewear.com`
