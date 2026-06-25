# Feature: Configuration & Secrets — Environment Variables + Local/Prod Profiles

NoPressure Wear — move hardcoded credentials and URLs out of `application.yml` into environment variables, and split config into `local` vs `prod` Spring profiles.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL, Mailtrap (SMTP), Stripe, OAuth2 (Google/Facebook), remove.bg.

> **Why:** Secrets in source control are a security risk (anyone with repo access sees DB passwords, API keys). Externalizing also lets the same build run locally and in production with different values, set per environment.

---

## 1. Target file structure

```
src/main/resources/
├── application.yml          # shared config + active-profile switch (NO secrets)
├── application-local.yml    # localhost dev values
├── application-prod.yml     # production — only ${ENV_VAR} references
└── application-local.yml.example   # committed template (optional)
```

Profile-specific files **override** the base `application.yml`. Spring picks the file matching the active profile.

---

## 2. Secrets currently hardcoded (move ALL of these to env vars)

Audit `application.yml` for these and externalize each:

| Setting | Env var | Notes |
|---|---|---|
| `spring.datasource.username` | `DATABASE_USERNAME` | DB user |
| `spring.datasource.password` | `DATABASE_PASSWORD` | DB password |
| `spring.datasource.url` | `DATABASE_URL` | full JDBC URL |
| `spring.mail.username` | `MAIL_USERNAME` | Mailtrap/SMTP user |
| `spring.mail.password` | `MAIL_PASSWORD` | Mailtrap/SMTP pass |
| `spring.mail.host` | `MAIL_HOST` | SMTP host |
| `spring.mail.port` | `MAIL_PORT` | SMTP port |
| JWT secret | `JWT_SECRET` | signing key |
| JWT expiration | `JWT_EXPIRATION` | ms |
| Google client id/secret | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | OAuth2 |
| Facebook client id/secret | `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | OAuth2 |
| Stripe secret key | `STRIPE_SECRET_KEY` | `sk_...` |
| remove.bg API key | `REMOVEBG_API_KEY` | image bg removal |
| Frontend URL | `FRONTEND_URL` | see §5 |
| Backend base URL | `BASE_URL` | see §5 |
| Upload dir | `UPLOAD_DIR` | file storage path |

---

## 3. `application.yml` (base — shared, no secrets)

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}   # defaults to local; prod sets the env var

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# Anything identical across environments stays here.
```

`${SPRING_PROFILES_ACTIVE:local}` = use the env var if set, else default to `local`.

---

## 4. `application-local.yml` (localhost dev)

Local dev values are low-risk (Mailtrap sandbox, local Postgres). You may keep literal values here for convenience, OR still use env vars with local defaults — your choice. Example with literals for easy local startup:

```yaml
app:
  frontend-url: http://localhost:5173
  base-url: http://localhost:8080
  upload:
    dir: uploads/products

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nopressure
    username: postgres
    password: postgres

  mail:
    host: sandbox.smtp.mailtrap.io
    port: 587
    username: your_mailtrap_user      # local only — sandbox, low risk
    password: your_mailtrap_pass

jwt:
  secret: local-dev-secret-change-me-please-32chars-min
  expiration: 86400000

stripe:
  secret-key: sk_test_xxx

removebg:
  api-key: your_local_removebg_key

spring.security.oauth2.client.registration:
  google:
    client-id: your_google_client_id
    client-secret: your_google_client_secret
  facebook:
    client-id: your_facebook_client_id
    client-secret: your_facebook_client_secret
```

> If you'd rather keep NO secrets in git even for local, add `application-local.yml` to `.gitignore` and commit `application-local.yml.example` with placeholder values instead (see §7).

---

## 5. `application-prod.yml` (live — env vars ONLY)

```yaml
app:
  frontend-url: ${FRONTEND_URL}
  base-url: ${BASE_URL}
  upload:
    dir: ${UPLOAD_DIR:/data/uploads/products}

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}

  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}

stripe:
  secret-key: ${STRIPE_SECRET_KEY}

removebg:
  api-key: ${REMOVEBG_API_KEY}

spring.security.oauth2.client.registration:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
  facebook:
    client-id: ${FACEBOOK_CLIENT_ID}
    client-secret: ${FACEBOOK_CLIENT_SECRET}
```

`${VAR}` with no default = **required**; the app fails fast at startup if missing (good — surfaces misconfig immediately). `${VAR:default}` supplies a fallback.

---

## 6. Externalize URLs in Java (remove hardcoded localhost:5173 / :8080)

You currently hardcode the frontend URL in a few places. Replace them with the injected property so prod points to the real domain.

### 6.1 OAuth2 success handler

```java
@Value("${app.frontend-url}")
private String frontendUrl;

// was: "http://localhost:5173/login?error=login_disabled"
String errorRedirect = frontendUrl + "/login?error=login_disabled";

// was: "http://localhost:5173/oauth2/callback" + ...
String redirectUrl = frontendUrl + "/oauth2/callback" + /* ...rest... */;
```

### 6.2 EmailService — these are still hardcoded, swap them too

```java
@Value("${app.frontend-url}")
private String frontendUrl;

// password reset — was "http://localhost:5173/reset-password?token=" + token
String resetUrl = frontendUrl + "/reset-password?token=" + token;

// order status email — was "http://localhost:5173/orders/" + orderId
String orderUrl = frontendUrl + "/orders/" + orderId;
```

### 6.3 Any other hardcoded URL

Search the codebase for `localhost:5173` and `localhost:8080` and replace each with the matching injected property (`app.frontend-url` or `app.base-url`). You already use `@Value("${app.base-url:http://localhost:8080}")` and `@Value("${app.upload.dir:...}")` — apply the same pattern everywhere.

---

## 7. Keep secrets out of git

### 7.1 .gitignore

```gitignore
# Local secrets (if you choose not to commit local config)
src/main/resources/application-local.yml

# Environment files
.env
*.env
```

### 7.2 Committed template (`application-local.yml.example`)

Commit a placeholder version so teammates know what to fill in:

```yaml
app:
  frontend-url: http://localhost:5173
  base-url: http://localhost:8080
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nopressure
    username: CHANGE_ME
    password: CHANGE_ME
  mail:
    username: CHANGE_ME
    password: CHANGE_ME
jwt:
  secret: CHANGE_ME_min_32_chars
stripe:
  secret-key: sk_test_CHANGE_ME
# ...etc
```

> **If secrets were already committed**, removing them from the file is NOT enough — they remain in git history. Rotate them (generate new DB password, new JWT secret, new Stripe/remove.bg keys, regenerate OAuth secrets) and optionally scrub history with `git filter-repo` / BFG. Treat any previously-committed secret as compromised.

---

## 8. How to set the env vars

### 8.1 IntelliJ IDEA (local run)

Run → Edit Configurations → your Spring Boot app:
- **Active profiles:** `local` (or leave blank, base defaults to local)
- **Environment variables:** add any you reference, e.g. `DATABASE_PASSWORD=...;MAIL_PASSWORD=...`

Or use an **EnvFile** plugin to load a `.env` (gitignored).

### 8.2 Local shell

```bash
export SPRING_PROFILES_ACTIVE=local
export DATABASE_PASSWORD=postgres
./mvnw spring-boot:run
```

### 8.3 Production (set on the host / platform)

```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://<host>:5432/<db>
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
MAIL_HOST=...
MAIL_PORT=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
JWT_SECRET=...
STRIPE_SECRET_KEY=...
REMOVEBG_API_KEY=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
FACEBOOK_CLIENT_ID=...
FACEBOOK_CLIENT_SECRET=...
FRONTEND_URL=https://yourdomain.com
BASE_URL=https://api.yourdomain.com
UPLOAD_DIR=/data/uploads/products
```

Most hosting platforms have a "Environment / Variables / Secrets" settings panel where you paste these. Run with profile `prod`.

### 8.4 Command line (jar)

```bash
java -jar app.jar --spring.profiles.active=prod
```

---

## 9. Frontend env (Vite) — same principle

The React app also has env values. Keep them in `.env` files (Vite uses `VITE_` prefix):

```
# .env.local (gitignored)
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_xxx

# production (set on Vercel/Netlify/host env panel)
VITE_API_URL=https://api.yourdomain.com
VITE_STRIPE_PUBLISHABLE_KEY=pk_live_xxx
```

Add `.env`, `.env.local`, `.env.*.local` to the frontend `.gitignore`. The Stripe **publishable** key is safe to expose (it's public by design); the **secret** key stays backend-only.

---

## 10. Verification

1. **Local:** start with no env vars set → app boots on `local` profile with local DB/Mailtrap.
2. **Prod simulation:** set `SPRING_PROFILES_ACTIVE=prod` and the required env vars → app boots reading from env.
3. **Missing var:** unset a required `${VAR}` (no default) in prod → app should fail fast at startup with a clear "Could not resolve placeholder" error (this is the desired behavior).
4. Send a test email locally → confirm reset/order URLs use `frontend-url` (point to localhost), then in prod they point to the real domain.

---

## 11. Checklist

- [ ] `application.yml` base created (profile switch, shared config, NO secrets)
- [ ] `application-local.yml` created (local dev values)
- [ ] `application-prod.yml` created (only `${ENV_VAR}` references)
- [ ] All DB / mail / JWT / Stripe / OAuth / remove.bg secrets moved to env vars
- [ ] Hardcoded `localhost:5173` / `localhost:8080` replaced with `app.frontend-url` / `app.base-url` in OAuth2 handler + EmailService + everywhere else
- [ ] `.gitignore` updated (local config + `.env`)
- [ ] `application-local.yml.example` committed as template
- [ ] Previously-committed secrets ROTATED (treat as compromised)
- [ ] IntelliJ run config sets active profile `local` (+ any env vars)
- [ ] Production env vars set on host with `SPRING_PROFILES_ACTIVE=prod`
- [ ] Frontend `.env` files set + gitignored (VITE_ prefix)
- [ ] Verified local + prod-simulation boot; missing-var fails fast
