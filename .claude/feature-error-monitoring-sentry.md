# Feature: Error Monitoring with Sentry (Backend + Frontend)

NoPressure Wear — add Sentry so unhandled errors (500s, crashes, frontend exceptions) are captured and reported automatically, instead of discovered via customer complaints.

Stack: Spring Boot 3.4.5 / Java 21, React + Vite, Stripe, JWT. Sentry free tier is sufficient for launch.

---

## What this gives you

When an error happens in production, Sentry captures the full stack trace plus context (URL, user, browser/OS, request data, release version, frequency) and sends it to a dashboard that can alert you by email/Slack. You learn about a broken checkout in minutes, not days — with the exact file, line, and request that caused it.

- **Backend:** unhandled exceptions in controllers/services → captured with stack trace + request context.
- **Frontend:** React render errors + uncaught JS exceptions + failed promises → captured with component stack + browser context.

---

## 0. Prerequisite — create a Sentry project

1. Sign up at sentry.io (free tier).
2. Create **two** projects: one **Java / Spring Boot** (backend), one **React** (frontend).
3. Each gives you a **DSN** (a URL-like key). You'll set these as env vars — never hardcode them.
   - `SENTRY_DSN` (backend)
   - `VITE_SENTRY_DSN` (frontend)

---

## PART A — Backend (Spring Boot)

### A.1 Dependency (pom.xml)

```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>7.14.0</version>
</dependency>
```

> Use the **`-jakarta`** starter — Spring Boot 3 / Java 21 uses the Jakarta namespace. The non-jakarta starter is for Spring Boot 2. Check sentry.io for the latest version; 7.x is current for Boot 3.

Reload Maven after adding.

### A.2 Config — application.yml (base) + profiles

Base `application.yml`:
```yaml
sentry:
  dsn: ${SENTRY_DSN:}                 # empty in local = Sentry disabled
  environment: ${SENTRY_ENV:local}
  release: ${SENTRY_RELEASE:dev}
  # capture rate for performance tracing (0.0–1.0); errors are always captured
  traces-sample-rate: 0.0             # start at 0 (errors only); raise later if you want tracing
  send-default-pii: false             # set true only if you accept sending user IP/email to Sentry
```

`application-prod.yml`:
```yaml
sentry:
  dsn: ${SENTRY_DSN}                  # required in prod
  environment: prod
  release: ${SENTRY_RELEASE:1.0.0}
```

> **Empty DSN = Sentry off.** With `${SENTRY_DSN:}` (empty default), Sentry does nothing locally unless you set the env var — so dev noise doesn't pollute your error dashboard. Only prod (with the DSN set) reports.

### A.3 That's it for basic capture

The starter auto-registers an exception handler that captures **unhandled** exceptions thrown from controllers/services. You don't need code changes for basic 500 capture.

> **Note on your `@RestControllerAdvice`:** if your global exception handler catches everything and returns a clean response, Sentry may not see it (the exception is "handled"). For business exceptions (validation, 404, "registration disabled") that's correct — you don't want those in Sentry. But for genuine 500s / unexpected exceptions, make sure they either propagate or are explicitly reported. See A.4.

### A.4 Explicitly report unexpected errors (recommended)

In your global handler, capture truly unexpected exceptions (the catch-all 500 branch) while leaving expected business exceptions unreported:

```java
import io.sentry.Sentry;

@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    Sentry.captureException(ex);        // report the genuine 500s
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Something went wrong"));
}
```

Do **not** add `Sentry.captureException` to handlers for expected exceptions (`ResourceNotFoundException`, `DuplicateResourceException`, `RegistrationDisabledException`, `LoginDisabledException`, validation, etc.) — those are normal flow, not bugs, and would create noise.

### A.5 Optional — attach the user to errors

To see *which user* hit an error, set the Sentry user from your JWT filter / a request interceptor (only if `send-default-pii` policy allows, and mind GDPR):

```java
io.sentry.Sentry.configureScope(scope -> {
    io.sentry.protocol.User u = new io.sentry.protocol.User();
    u.setId(String.valueOf(userId));
    // u.setEmail(email);  // only if your privacy policy permits sending email to Sentry
    scope.setUser(u);
});
```

> GDPR: sending user email/IP to a third party (Sentry) is processing personal data. Keep `send-default-pii: false` and avoid email unless your privacy policy covers it. User **id** alone is lower-risk.

---

## PART B — Frontend (React + Vite)

### B.1 Dependency

```bash
npm install @sentry/react
```

### B.2 Initialize before rendering — main.jsx

```jsx
import * as Sentry from '@sentry/react';

Sentry.init({
    dsn: import.meta.env.VITE_SENTRY_DSN || undefined,  // undefined = disabled
    environment: import.meta.env.MODE,                  // 'development' | 'production'
    integrations: [Sentry.browserTracingIntegration()],
    tracesSampleRate: 0.0,        // errors only to start; raise for performance tracing
    // Only send in production builds — avoid dev noise
    enabled: import.meta.env.PROD,
});
```

> `enabled: import.meta.env.PROD` and a missing/empty DSN both keep Sentry off in dev. Set `VITE_SENTRY_DSN` only in your production env (Vercel/host env panel).

### B.3 Wrap the app with an error boundary

```jsx
import * as Sentry from '@sentry/react';
import App from './App';

const SentryApp = Sentry.withErrorBoundary(App, {
    fallback: ({ resetError }) => (
        <div style={{ padding: 40, textAlign: 'center' }}>
            <h1>Something went wrong</h1>
            <p>Please try again.</p>
            <button onClick={resetError}>Reload</button>
        </div>
    ),
});

// render <SentryApp /> instead of <App />
createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <SentryApp />
    </React.StrictMode>
);
```

The error boundary catches React render crashes (the "white screen" cases), shows a fallback UI instead of a blank page, and reports the error to Sentry. Uncaught exceptions and unhandled promise rejections elsewhere are captured automatically by `Sentry.init`.

### B.4 Optional — manually capture handled errors

In a `catch` where you handle an error but still want visibility:

```jsx
import * as Sentry from '@sentry/react';

catch (err) {
    Sentry.captureException(err);          // optional: report even handled errors
    toast.error(t('messages.somethingWrong'));
}
```

Use sparingly — only for errors worth investigating, not routine validation failures.

### B.5 Optional — identify the user

After login, attach the user so frontend errors show who hit them (mind GDPR — id over email):

```jsx
import * as Sentry from '@sentry/react';

Sentry.setUser({ id: String(user.id) });   // on login
Sentry.setUser(null);                       // on logout
```

---

## PART C — Env vars

```
# Backend (prod host)
SENTRY_DSN=https://<key>@oXXXX.ingest.sentry.io/XXXX
SENTRY_ENV=prod
SENTRY_RELEASE=1.0.0

# Frontend (Vercel/host env panel)
VITE_SENTRY_DSN=https://<key>@oXXXX.ingest.sentry.io/YYYY
```

Add to the env-vars/config spec so they're documented with the rest. Never commit DSNs — treat them like other secrets (though a DSN is lower-risk than a password, keep it env-driven for consistency and easy rotation).

---

## D. Verify it works

1. **Backend:** with `SENTRY_DSN` set, throw a test exception from a temporary endpoint (`throw new RuntimeException("Sentry test");`) → confirm it appears in the Sentry backend project within seconds. Remove the test endpoint after.
2. **Frontend:** with `VITE_SENTRY_DSN` set and a production build, trigger a deliberate error (e.g. a button that does `throw new Error("Sentry test")`) → confirm it appears in the Sentry frontend project. Remove after.
3. Confirm **dev does NOT report** (no DSN / `enabled: PROD`) — your dashboard should stay clean during local work.
4. Set up an **alert rule** in Sentry (e.g. email on any new issue, or when an issue affects > N users).

---

## E. Notes & good practice

- **Don't report expected exceptions** — validation, 404, auth-disabled, etc. are normal flow; reporting them creates noise that hides real bugs.
- **Errors vs tracing:** errors are captured regardless of `tracesSampleRate`. Tracing (performance) is separate and sampled; start at `0.0` and raise later if you want performance insights (it consumes more of the free quota).
- **Free tier quota:** Sentry's free tier has a monthly event cap. Keeping dev disabled and not reporting expected exceptions keeps you well within it for a small shop.
- **GDPR:** Sentry is a third-party processor. Keep PII off (`send-default-pii: false`, user id not email) unless your privacy policy lists Sentry as a processor. Add it to your data-processing disclosures if you send any personal data.
- **Releases (optional later):** wiring `SENTRY_RELEASE` to your build version lets Sentry tell you which deploy introduced an error and mark issues as resolved-in-release. Nice once you're iterating in prod.

---

## F. Checklist

- [ ] Sentry account + two projects (Java backend, React frontend); DSNs obtained
- [ ] Backend: `sentry-spring-boot-starter-jakarta` dependency
- [ ] Backend: `sentry.*` config (empty DSN default = off locally; required in prod)
- [ ] Backend: `Sentry.captureException` in the catch-all 500 handler only (not expected exceptions)
- [ ] Backend: (optional) attach user id to scope
- [ ] Frontend: `@sentry/react` installed
- [ ] Frontend: `Sentry.init` in main.jsx (enabled in PROD only)
- [ ] Frontend: app wrapped with `Sentry.withErrorBoundary` + fallback UI
- [ ] Frontend: (optional) `Sentry.setUser` on login/logout
- [ ] Env vars set in prod (`SENTRY_DSN`, `VITE_SENTRY_DSN`); documented in config spec
- [ ] Verified: test error appears in each project; dev stays silent
- [ ] Sentry alert rule configured (email/Slack on new issues)
- [ ] GDPR: PII policy decided; Sentry listed as processor if sending personal data
