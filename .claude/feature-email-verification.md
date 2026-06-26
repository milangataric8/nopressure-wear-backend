# Feature: Email Verification on Registration

NoPressure Wear — require new users to confirm their email via a token link before their account is fully active. Cuts fake/typo signups and reduces bounce.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, Spring Security + JWT, React + Vite + Tailwind, react-i18next (EN/SR). Package root: `rs.nopressurewear`. Email goes through the existing `EmailService` + `getEmailTranslations(lang)`.

> **Policy decision (assumption — adjust in §7):** users **can register and log in** before verifying, but **cannot checkout** until verified ("soft gate"). The alternative — block login entirely until verified ("hard gate") — is noted where it differs. Soft gate is friendlier and avoids locking people out over a missed email; OAuth2 users are auto-verified (the provider already confirmed the address).

---

## 1. Database — Flyway migration

```sql
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE email_verification_token (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_verif_token ON email_verification_token(token);

-- Existing users predate verification — treat them as already verified
UPDATE users SET email_verified = TRUE;

-- OAuth2 users are verified by their provider; if you can identify them by provider column:
-- UPDATE users SET email_verified = TRUE WHERE provider <> 'LOCAL';
```

> The `UPDATE ... SET email_verified = TRUE` backfill prevents locking out everyone who registered before this feature.

---

## 2. Entities

### User.java — add the flag
```java
@Column(name = "email_verified", nullable = false)
private boolean emailVerified = false;
```

### EmailVerificationToken.java
```java
package rs.nopressurewear.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_token")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

---

## 3. Repository

```java
package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.nopressurewear.model.EmailVerificationToken;
import rs.nopressurewear.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    // Invalidate any previous unused tokens for a user before issuing a new one
    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidatePreviousTokens(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
```

---

## 4. Service — issue, send, verify, resend

Add to `AuthService` (or a dedicated `EmailVerificationService`).

```java
private static final long VERIFICATION_TTL_HOURS = 24;

@Transactional
public void sendVerificationEmail(User user, String lang) {
    tokenRepository.invalidatePreviousTokens(user);

    String token = UUID.randomUUID().toString();
    EmailVerificationToken vt = EmailVerificationToken.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusHours(VERIFICATION_TTL_HOURS))
            .used(false)
            .build();
    tokenRepository.save(vt);

    emailService.sendVerificationEmail(user.getEmail(), token, lang);
}

@Transactional
public void verifyEmail(String token) {
    EmailVerificationToken vt = tokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid verification link"));

    if (vt.isUsed()) throw new InvalidTokenException("This link has already been used");
    if (vt.getExpiresAt().isBefore(LocalDateTime.now()))
        throw new InvalidTokenException("This verification link has expired");

    User user = vt.getUser();
    user.setEmailVerified(true);
    userRepository.save(user);

    vt.setUsed(true);
    tokenRepository.save(vt);
}

@Transactional
public void resendVerification(String email, String lang) {
    User user = userRepository.findByEmail(email).orElse(null);
    // Don't reveal whether the email exists; only act if it does and isn't verified
    if (user != null && !user.isEmailVerified()) {
        sendVerificationEmail(user, lang);
    }
}
```

Exception:
```java
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) { super(message); }
}
```
Map to HTTP 400 in your `@RestControllerAdvice`.

### Hook into registration
In `register(...)`, after saving the new user:
```java
User saved = userRepository.save(user);   // emailVerified defaults to false
sendVerificationEmail(saved, lang);
// soft gate: still return the normal auth response / allow login
```

### OAuth2 users — auto-verify
In the OAuth2 success handler, when creating/loading the user, set `emailVerified = true` (the provider already verified the address).

---

## 5. EmailService — verification email (translated)

```java
public void sendVerificationEmail(String to, String token, String lang) {
    Map<String, String> t = getEmailTranslations(lang);
    String verifyUrl = frontendUrl + "/verify-email?token=" + token;   // use app.frontend-url

    String html = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8">
            <style>
                body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
                .container { max-width: 560px; margin: 40px auto; background: #fff; border: 1px solid #e5e5e5; }
                .header { padding: 32px 40px; border-bottom: 1px solid #e5e5e5; text-align: center; }
                .header h1 { margin: 0; font-size: 20px; font-weight: 900; text-transform: uppercase; color: #111; }
                .body { padding: 40px; }
                .body p { font-size: 14px; color: #555; line-height: 1.6; margin: 0 0 24px; }
                .button { display: inline-block; background: #111; color: #fff !important; text-decoration: none; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; padding: 14px 32px; }
                .footer { padding: 24px 40px; border-top: 1px solid #e5e5e5; text-align: center; }
                .footer p { font-size: 12px; color: #999; margin: 0; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header"><h1>NoPressure wear</h1></div>
                <div class="body">
                    <p>%s</p>
                    <a href="%s" class="button">%s</a>
                    <p style="margin-top: 32px; font-size: 13px; color: #999;">%s</p>
                </div>
                <div class="footer"><p>© 2026 NoPressure. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """.formatted(
            t.get("verifyText"),
            verifyUrl,
            t.get("verifyButton"),
            t.get("verifyExpire")
        );

    sendHtmlEmail(to, t.get("verifySubject"), html);
}
```

### Add keys to `getEmailTranslations`
```java
// EN
t.put("verifySubject", "Verify your email");
t.put("verifyText", "Welcome! Please confirm your email address to activate your account.");
t.put("verifyButton", "Verify Email");
t.put("verifyExpire", "This link expires in 24 hours. If you didn't sign up, ignore this email.");

// SR
t.put("verifySubject", "Potvrdite vaš email");
t.put("verifyText", "Dobrodošli! Molimo potvrdite vašu email adresu da biste aktivirali nalog.");
t.put("verifyButton", "Potvrdi email");
t.put("verifyExpire", "Link ističe za 24 sata. Ako se niste registrovali, ignorišite ovaj email.");
```

---

## 6. Controller

```java
@GetMapping("/verify-email")
public ResponseEntity<?> verifyEmail(@RequestParam String token) {
    authService.verifyEmail(token);
    return ResponseEntity.ok().build();
}

@PostMapping("/resend-verification")
public ResponseEntity<?> resendVerification(@RequestBody ResendRequest req,
                                            @RequestParam(defaultValue = "en") String lang) {
    authService.resendVerification(req.getEmail(), lang);
    return ResponseEntity.ok().build();   // always 200 — don't reveal account existence
}
```

Make both `permitAll` in the `@Order(1)` API security chain:
```java
.requestMatchers("/api/auth/verify-email", "/api/auth/resend-verification").permitAll()
```

---

## 7. Enforcement — where the gate lives

### Soft gate (chosen): block checkout until verified
In `OrderService.checkout` (and `guestCheckout` does NOT apply — guests have no account):
```java
User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
if (!user.isEmailVerified()) {
    throw new EmailNotVerifiedException("Please verify your email before placing an order");
}
```
Map `EmailNotVerifiedException` to **403** and surface a clear message + a "resend verification" action on the frontend.

### Hard gate (alternative): block login until verified
If you prefer this instead, in `AuthService.login`, after authenticating and resolving the user:
```java
if (!user.isEmailVerified() && user.getRole() == Role.CUSTOMER) {
    throw new EmailNotVerifiedException("Please verify your email to log in");
}
```
> Don't hard-gate ADMIN/EMPLOYEE. Pick **one** gate (soft or hard), not both.

---

## 8. Frontend

### 8.1 Verify-email page (`/verify-email`)
Reads `?token=`, calls the endpoint, shows success/error.
```jsx
import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { verifyEmail } from '../api/authApi';

const VerifyEmailPage = () => {
    const [params] = useSearchParams();
    const [status, setStatus] = useState('loading'); // loading | success | error
    const { t } = useTranslation();

    useEffect(() => {
        const token = params.get('token');
        if (!token) { setStatus('error'); return; }
        verifyEmail(token)
            .then(() => setStatus('success'))
            .catch(() => setStatus('error'));
    }, [params]);

    return (
        <div className="max-w-md mx-auto py-20 text-center">
            {status === 'loading' && <p className="text-sm text-gray-500">{t('auth.verifying')}</p>}
            {status === 'success' && (
                <>
                    <h1 className="text-xl font-black uppercase mb-3">{t('auth.verifiedTitle')}</h1>
                    <p className="text-sm text-gray-500 mb-6">{t('auth.verifiedText')}</p>
                    <Link to="/login" className="bg-black text-white text-xs font-semibold uppercase tracking-wide px-6 py-3">
                        {t('auth.goToLogin')}
                    </Link>
                </>
            )}
            {status === 'error' && (
                <>
                    <h1 className="text-xl font-black uppercase mb-3">{t('auth.verifyFailedTitle')}</h1>
                    <p className="text-sm text-gray-500">{t('auth.verifyFailedText')}</p>
                </>
            )}
        </div>
    );
};
export default VerifyEmailPage;
```
Add the route `/verify-email`.

### 8.2 After registration — tell the user to check their inbox
On successful register, show: "We sent a verification link to {email}. Please check your inbox." Optionally a "resend" button calling `/resend-verification`.

### 8.3 Soft-gate UX
If checkout returns 403 `EmailNotVerified`, show a banner: "Verify your email to place orders" + a resend button. A small "unverified" notice on the account page is also helpful.

### 8.4 API (authApi.js)
```javascript
export const verifyEmail = (token) =>
    axiosInstance.get('/auth/verify-email', { params: { token } });

export const resendVerification = (email) =>
    axiosInstance.post('/auth/resend-verification', { email }, { params: { lang: i18n.language } });
```

---

## 9. Cleanup job (optional)

Purge expired tokens periodically:
```java
@Scheduled(cron = "0 0 3 * * *")   // 3 AM daily
@Transactional
public void purgeExpiredTokens() {
    tokenRepository.deleteExpiredBefore(LocalDateTime.now());
}
```
Enable `@EnableScheduling` on a config class if not already on.

---

## 10. i18n keys (en.json / sr.json)

```jsonc
// en.json — "auth"
"verifying": "Verifying your email…",
"verifiedTitle": "Email Verified",
"verifiedText": "Your email is confirmed. You can now log in and shop.",
"verifyFailedTitle": "Verification Failed",
"verifyFailedText": "This link is invalid or has expired. Request a new one from your account.",
"goToLogin": "Go to Login",
"checkInbox": "We sent a verification link to {{email}}. Please check your inbox.",
"resend": "Resend verification email",
"notVerifiedCheckout": "Please verify your email before placing an order."

// sr.json — "auth"
"verifying": "Potvrđujemo vaš email…",
"verifiedTitle": "Email potvrđen",
"verifiedText": "Vaš email je potvrđen. Sada možete da se prijavite i kupujete.",
"verifyFailedTitle": "Potvrda neuspešna",
"verifyFailedText": "Link je nevažeći ili je istekao. Zatražite novi sa svog naloga.",
"goToLogin": "Prijavi se",
"checkInbox": "Poslali smo link za potvrdu na {{email}}. Proverite vaše sanduče.",
"resend": "Poslat je ponovo email za potvrdu",
"notVerifiedCheckout": "Molimo potvrdite vaš email pre nego što naručite."
```

---

## 11. Security & edge cases

- **One-time, expiring tokens:** mark `used = true` on success; reject used/expired. 24h TTL.
- **Invalidate old tokens on resend** so only the latest link works.
- **Don't reveal account existence:** `/resend-verification` always returns 200 whether or not the email exists/needs verification.
- **OAuth2 auto-verified:** provider already confirmed the email; set `emailVerified = true` on creation.
- **Existing users:** backfilled to verified so nobody is locked out.
- **One gate only:** soft (checkout) OR hard (login) — not both.
- **Rate limit** `/resend-verification` (reuse the auth rate-limiting from the security spec) to prevent email-bombing.
- **Frontend URL:** build the verify link from `app.frontend-url`, not a hardcoded localhost.

---

## 12. Checklist

- [ ] Flyway: `users.email_verified` + `email_verification_token` table + backfill existing users to verified
- [ ] `User.emailVerified`, `EmailVerificationToken` entity, repository
- [ ] Service: send / verify / resend (+ invalidate previous tokens)
- [ ] `register()` issues token + sends email; OAuth2 users auto-verified
- [ ] `EmailService.sendVerificationEmail` + translation keys
- [ ] Controller: `GET /verify-email`, `POST /resend-verification` (permitAll)
- [ ] Gate chosen and enforced (soft = checkout 403, or hard = login)
- [ ] Frontend: `/verify-email` page + route, post-register "check inbox" + resend, soft-gate banner
- [ ] authApi: verifyEmail + resendVerification (sends lang)
- [ ] Optional: scheduled expired-token cleanup
- [ ] i18n keys (EN + SR)
- [ ] Verified end-to-end: register → email → click → verified → can checkout; expired/used link rejected; resend works; OAuth2 auto-verified
