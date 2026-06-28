# Feature: Account Lockout After Failed Login Attempts

NoPressure Wear — lock an account temporarily after N consecutive failed logins against a specific email, to slow credential-stuffing / brute-force attacks. Complements the IP-based `RateLimitFilter` (which limits request *rate*); this tracks failures *per email* and locks the account for a cooldown period.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, Spring Security + JWT, React + Vite + Tailwind, react-i18next (EN/SR). Package root: `rs.nopressurewear`.

> **Design (assumption — adjust in §9):** DB-backed counter on the user (no Redis dependency). After **5** failed attempts the account locks for **15 minutes**; a successful login resets the counter. Lock is automatic and self-expiring (no admin action needed), with an optional admin unlock.

---

## 1. Database — Flyway migration

```sql
ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN lock_until TIMESTAMP;   -- null = not locked
```

- `failed_login_attempts` — running count of consecutive failures.
- `lock_until` — if set and in the future, the account is locked until that time.

---

## 2. User entity

```java
@Column(name = "failed_login_attempts", nullable = false)
private int failedLoginAttempts = 0;

@Column(name = "lock_until")
private LocalDateTime lockUntil;

@Transient
public boolean isLocked() {
    return lockUntil != null && lockUntil.isAfter(LocalDateTime.now());
}
```

`isLocked()` is a convenience (not persisted) — true only while the lock window is active.

---

## 3. Config — thresholds

`application.yml` (base):
```yaml
security:
  lockout:
    max-attempts: ${LOCKOUT_MAX_ATTEMPTS:5}
    lock-minutes: ${LOCKOUT_LOCK_MINUTES:15}
```

Inject where needed:
```java
@Value("${security.lockout.max-attempts:5}")
private int maxAttempts;

@Value("${security.lockout.lock-minutes:15}")
private int lockMinutes;
```

---

## 4. Service logic — in AuthService.login

The flow, in order:

1. Look up the user by email.
2. **If locked → reject immediately** (before checking the password) with a clear "account locked, try later" message.
3. Verify credentials.
4. **On failure →** increment the counter; if it reaches `maxAttempts`, set `lockUntil`; persist; throw "invalid credentials" (or "now locked").
5. **On success →** reset counter + clear lock; proceed (then the existing verified/login-enabled gates) and issue JWT.

```java
public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail()).orElse(null);

    // Generic failure message — don't reveal whether the email exists
    if (user == null) {
        throw new BadCredentialsException("Invalid email or password");
    }

    // 2) Already locked?
    if (user.isLocked()) {
        long minutesLeft = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
        throw new AccountLockedException("Account locked. Try again in " + minutesLeft + " minute(s).");
    }

    // 3) Verify password
    boolean valid = passwordEncoder.matches(request.getPassword(), user.getPassword());

    if (!valid) {
        registerFailedAttempt(user);    // 4) increment + maybe lock
        throw new BadCredentialsException("Invalid email or password");
    }

    // 5) Success → reset
    resetFailedAttempts(user);

    // existing gates (email verified, login_enabled) then issue JWT
    // ...
    return buildAuthResponse(user);
}

@Transactional
protected void registerFailedAttempt(User user) {
    int attempts = user.getFailedLoginAttempts() + 1;
    user.setFailedLoginAttempts(attempts);
    if (attempts >= maxAttempts) {
        user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
    }
    userRepository.save(user);
}

@Transactional
protected void resetFailedAttempts(User user) {
    if (user.getFailedLoginAttempts() != 0 || user.getLockUntil() != null) {
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);
    }
}
```

> If your login currently authenticates via `AuthenticationManager.authenticate(...)` rather than a manual `passwordEncoder.matches`, you can instead catch the `BadCredentialsException` it throws, call `registerFailedAttempt`, and rethrow — same effect. The manual check above is simplest to reason about; adapt to your existing flow.

### Notes on ordering
- **Check lock before password.** A locked account shouldn't even attempt password verification.
- **Generic message for unknown email** (`user == null`) — same wording as a bad password, so the endpoint doesn't reveal which emails exist.
- **Self-invocation caveat:** if `registerFailedAttempt` / `resetFailedAttempts` are called from `login` in the same bean, the `@Transactional` on them is bypassed (self-invocation). Since `login` itself runs in a transaction (or you can annotate `login` `@Transactional`), the saves are covered. Simplest: make `login` `@Transactional` and drop the annotations on the helpers, or keep the helpers as plain saves within login's transaction.

---

## 5. Exception + handler

```java
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) { super(message); }
}
```

Map to **HTTP 423 Locked** (or 429) in your `@RestControllerAdvice`:
```java
@ExceptionHandler(AccountLockedException.class)
public ResponseEntity<ErrorResponse> handleLocked(AccountLockedException ex) {
    return ResponseEntity.status(HttpStatus.LOCKED)   // 423
            .body(new ErrorResponse(ex.getMessage()));
}
```

Keep `BadCredentialsException` mapping to **401** with a generic message.

---

## 6. Lock is self-expiring (no cleanup job needed)

Because `isLocked()` checks `lockUntil.isAfter(now)`, the lock simply expires when the window passes — the next login attempt after `lockUntil` proceeds to password verification normally. No scheduled job required. (The counter resets on the next *successful* login; a still-wrong password after expiry starts counting again from the current value — see §9 if you want the counter to also reset at expiry.)

---

## 7. Optional — admin unlock

Let an admin clear a lock manually (e.g. a customer calls in).

Service:
```java
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public void unlockAccount(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);
    userRepository.save(user);
}
```

Controller (under the admin user-management endpoints from the disable-auth spec):
```java
@PatchMapping("/{id}/unlock")
public ResponseEntity<Void> unlock(@PathVariable Long id) {
    userService.unlockAccount(id);
    return ResponseEntity.noContent().build();
}
```

In AdminUsers, show a "Locked" badge when `lockUntil` is in the future and an "Unlock" button calling the endpoint. (Requires exposing `lockUntil` / a `locked` boolean in the user response — add if you want the badge.)

---

## 8. Frontend — handle the locked response

On the login page, detect the 423 and show the lockout message (distinct from a normal bad-password error):

```jsx
catch (err) {
    const status = err.response?.status;
    const msg = err.response?.data?.message;
    if (status === 423) {
        toast.error(msg || t('auth.accountLocked'));   // "Account locked, try again later"
    } else {
        toast.error(msg || t('auth.loginFailed'));
    }
}
```

Optionally show a hint after a couple of failures ("Too many attempts will temporarily lock your account") — but don't reveal the exact remaining-attempts count to an attacker; the backend message already gives the cooldown when locked.

---

## 9. Decisions & tuning

- **Thresholds:** 5 attempts / 15 min are sane defaults; tune via env vars. Stricter (3/30) for higher security, looser (10/10) for fewer false lockouts.
- **Counter reset at expiry (optional):** as written, the counter only resets on success; after the lock expires the next wrong password increments from the prior value and can re-lock quickly. If you'd rather give a fresh allowance after the lock window, reset the counter when you detect an expired lock:
  ```java
  if (user.getLockUntil() != null && user.getLockUntil().isBefore(LocalDateTime.now())) {
      user.setFailedLoginAttempts(0);
      user.setLockUntil(null);
  }
  ```
  Run this at the top of `login` (after loading the user) before checking `isLocked()`.
- **Per-email vs per-IP:** this is per-email (protects a specific account). The `RateLimitFilter` is per-IP (protects the endpoint). Keep both — they cover different attacks. A distributed attack from many IPs against one email is caught here; a flood from one IP is caught by the filter.
- **DB vs Redis:** DB is simplest and fine for a single-instance app. If you scale to multiple backend instances and want shared, fast counters (or to avoid a DB write per failed attempt), move the counter to Redis with a TTL key per email. Same logic, different store. Start with DB.
- **Lockout enumeration:** the "account locked" message does confirm an email exists. This is a common, accepted trade-off (the user needs to know why they can't log in). If you want zero enumeration, use a generic "too many attempts" message that's also shown for unknown emails after enough tries — more complex, usually unnecessary for a webshop.
- **Don't lock staff out forever:** lock applies to all roles, but it self-expires; the admin-unlock (§7) is the escape hatch if an admin locks themselves out (and you should have a second admin, per the disable-auth spec).

---

## 10. Checklist

- [ ] Flyway: `users.failed_login_attempts` + `users.lock_until`
- [ ] User entity fields + `isLocked()` helper
- [ ] Config: `security.lockout.max-attempts` / `lock-minutes` (env-tunable)
- [ ] AuthService.login: check lock → verify → increment-or-reset; lock at threshold
- [ ] `login` transactional so counter saves commit (mind self-invocation)
- [ ] `AccountLockedException` + 423 handler; bad credentials stays 401 generic
- [ ] (Optional) reset counter when an expired lock is detected
- [ ] (Optional) admin unlock endpoint + Locked badge/Unlock button in AdminUsers
- [ ] Frontend: handle 423 with a distinct lockout message
- [ ] Verified: N failures lock the account; locked login is rejected before password check; lock self-expires; success resets; admin unlock works
