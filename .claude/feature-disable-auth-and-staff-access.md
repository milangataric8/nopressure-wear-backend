# Feature: Disable Customer Registration \& Login + Staff (Admin/Employee) Access

NoPressure Wear — admin toggles to disable customer registration and login ("maintenance / closed" mode), while admins and employees can always sign in to manage the shop. Since self-registration is disabled, staff accounts are created by an admin.

Stack: Spring Boot 3.4.5 / Java 21, Spring Security + JWT, PostgreSQL + Flyway, React + Vite + Tailwind, react-i18next (EN/SR). Roles: `ADMIN`, `EMPLOYEE`, `CUSTOMER`.

\---

## Design decision (assumption — adjust if you prefer the alternative)

**Chosen: role-gated single login.**

* The normal `/api/auth/login` endpoint stays available.
* When `login\_enabled = false`, the endpoint **authenticates credentials first**, then rejects users whose role is `CUSTOMER`, while `ADMIN`/`EMPLOYEE` pass through.
* `registration\_enabled = false` blocks the public register endpoint entirely.
* Staff accounts are created via an **admin-only user-management endpoint** (no self-registration for staff).

> \*\*Alternative (not used here):\*\* a separate hidden staff route `/api/auth/staff-login` that's always on, with the public `/login` fully closed when disabled. If you want this instead, see §8.

Security note: gating happens **after** credential verification so the toggle never leaks whether an account exists. A customer with valid credentials simply sees "login temporarily unavailable."

\---

## 1\. Settings — Flyway migration

```sql
INSERT INTO store\_settings (key, value, label) VALUES
    ('registration\_enabled', 'true', 'Customer Registration'),
    ('login\_enabled', 'true', 'Customer Login');
```

Both default to `true` (open). Setting to `false` closes that flow for customers only.

\---

## 2\. Backend — read settings helper

Add a small helper (in `AuthService`, or a shared `SettingsService`) using your existing `StoreSettingsRepository.findByKey(...)` pattern:

```java
private boolean isSettingEnabled(String key, boolean defaultValue) {
    return storeSettingsRepository.findByKey(key)
            .map(s -> !"false".equalsIgnoreCase(s.getValue()))
            .orElse(defaultValue);
}
```

Inject `StoreSettingsRepository` into `AuthService` if not already present.

\---

## 3\. Backend — gate REGISTRATION

In `AuthService.register(...)`, at the very top:

```java
public AuthResponse register(RegisterRequest request) {
    if (!isSettingEnabled("registration\_enabled", true)) {
        throw new RegistrationDisabledException("Registration is currently disabled");
    }
    // ... existing registration logic ...
}
```

Create the exception (or reuse a generic 403 business exception):

```java
public class RegistrationDisabledException extends RuntimeException {
    public RegistrationDisabledException(String message) { super(message); }
}
```

Map it to **HTTP 403** in your `@RestControllerAdvice` global handler:

```java
@ExceptionHandler(RegistrationDisabledException.class)
public ResponseEntity<ErrorResponse> handleRegistrationDisabled(RegistrationDisabledException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getMessage()));
}
```

\---

## 4\. Backend — gate LOGIN (role-aware)

In `AuthService.login(...)`, after the credentials are authenticated and you have the `User`/role, but before issuing the JWT:

```java
public AuthResponse login(LoginRequest request) {
    // 1) authenticate credentials (existing logic) → resolve the user
    User user = authenticateAndGetUser(request);   // your existing flow

    // 2) gate customers when login is disabled; staff always pass
    boolean loginEnabled = isSettingEnabled("login\_enabled", true);
    if (!loginEnabled \&\& user.getRole() == Role.CUSTOMER) {
        throw new LoginDisabledException("Login is currently disabled");
    }

    // 3) issue JWT (existing logic)
    return buildAuthResponse(user);
}
```

Exception + handler (403):

```java
public class LoginDisabledException extends RuntimeException {
    public LoginDisabledException(String message) { super(message); }
}
```

```java
@ExceptionHandler(LoginDisabledException.class)
public ResponseEntity<ErrorResponse> handleLoginDisabled(LoginDisabledException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getMessage()));
}
```

> \*\*Important:\*\* verify credentials FIRST, then check the toggle. Never reject before authentication, or the toggle becomes an account-enumeration oracle and also locks out staff who share the endpoint.

### 4.1 OAuth2 logins (Google/Facebook)

OAuth2 users are customers. In your OAuth2 success handler, apply the same gate: if `login\_enabled = false` and the resolved/created user is `CUSTOMER`, redirect to the frontend with an error param instead of issuing a token:

```java
if (!isSettingEnabled("login\_enabled", true) \&\& user.getRole() == Role.CUSTOMER) {
    response.sendRedirect(frontendUrl + "/login?error=login\_disabled");
    return;
}
```

\---

## 5\. Backend — STAFF account creation (admin-only)

Because registration is disabled, admins create staff (and customer) accounts here.

### 5.1 DTO

```java
@Getter @Setter
public class CreateStaffRequest {
    @NotBlank private String fullName;
    @Email @NotBlank private String email;
    @NotBlank private String password;
    @NotNull  private Role role;     // ADMIN | EMPLOYEE | CUSTOMER
}
```

### 5.2 Service (UserService or AuthService)

```java
@PreAuthorize("hasRole('ADMIN')")
public UserResponse createStaff(CreateStaffRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateResourceException("Email already in use");
    }
    User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole())
            .provider("LOCAL")
            .enabled(true)
            .build();
    return toUserResponse(userRepository.save(user));
}
```

> Only `ADMIN` can create accounts (note `hasRole('ADMIN')`, not `hasAnyRole`). If you want employees to create only customers, add a branch that forbids EMPLOYEE from setting ADMIN/EMPLOYEE roles.

### 5.3 Controller

```java
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStaff(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(userService.listUsers(role));   // optional role filter
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id, @RequestParam Role role) {
        return ResponseEntity.ok(userService.changeRole(id, role));
    }
}
```

Ensure your `@Order(1)` API SecurityFilterChain protects `/api/admin/\*\*` with `hasRole('ADMIN')` (method-level `@PreAuthorize` already does, but a URL rule is good defense in depth).

### 5.4 Safety guard

Prevent removing the last admin (so the shop can't lock itself out):

```java
public UserResponse changeRole(Long id, Role newRole) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (user.getRole() == Role.ADMIN \&\& newRole != Role.ADMIN
            \&\& userRepository.countByRole(Role.ADMIN) <= 1) {
        throw new RuntimeException("Cannot demote the last admin");
    }
    user.setRole(newRole);
    return toUserResponse(userRepository.save(user));
}
```

\---

## 6\. Frontend — AdminSettings toggles

Add to the relevant section's `keys` array (e.g. a new "Access" section or the existing features group):

```jsx
{ key: 'access', title: t('settings.access'),
  keys: \['registration\_enabled', 'login\_enabled'] }
```

Add both keys to the boolean-toggle `.includes(setting.key)` render-chain array (the same list that holds `contact\_enabled`, `add\_to\_cart\_enabled`, etc.):

```jsx
).includes(setting.key) ? (   // ...add 'registration\_enabled', 'login\_enabled'
```

> Reminder: a toggle key must appear in BOTH the `sections` array AND the `.includes(...)` render-chain list, or it renders as a text field instead of a checkbox.

\---

## 7\. Frontend — Login / Register pages

### 7.1 Read the settings

```jsx
const \[registrationEnabled, setRegistrationEnabled] = useState(true);
const \[loginEnabled, setLoginEnabled] = useState(true);

useEffect(() => {
    getSettingsMap().then(r => {
        setRegistrationEnabled(r.data.registration\_enabled !== 'false');
        setLoginEnabled(r.data.login\_enabled !== 'false');
    }).catch(() => {});
}, \[]);
```

### 7.2 RegisterPage — block when disabled

Registration is fully closed for everyone, so hide the form and show a notice (or redirect):

```jsx
if (!registrationEnabled) {
    return (
        <div className="max-w-md mx-auto py-20 text-center">
            <h1 className="text-xl font-black uppercase mb-3">{t('auth.registrationClosedTitle')}</h1>
            <p className="text-sm text-gray-500">{t('auth.registrationClosed')}</p>
        </div>
    );
}
// ... otherwise the normal register form
```

Also hide the "Don't have an account? Register" link on the login page when `!registrationEnabled`.

### 7.3 LoginPage — keep the form (staff must log in), show a notice

The login form must stay functional so staff can sign in. When `loginEnabled` is false, show an informational banner but keep the form; the backend decides per role:

```jsx
{!loginEnabled \&\& (
    <div className="mb-4 p-3 bg-amber-50 border border-amber-200 text-xs text-amber-800">
        {t('auth.loginRestricted')}
    </div>
)}
{/\* normal login form stays here \*/}
```

Handle the backend 403 in the submit handler:

```jsx
catch (err) {
    const msg = err.response?.data?.message;
    if (err.response?.status === 403 \&\& msg) {
        toast.error(t('auth.loginDisabled'));   // "Login is currently unavailable"
    } else {
        toast.error(msg || t('auth.loginFailed'));
    }
}
```

### 7.4 OAuth2 error redirect

On the login page, read `?error=login\_disabled` from the URL and show the same notice:

```jsx
const \[params] = useSearchParams();
useEffect(() => {
    if (params.get('error') === 'login\_disabled') {
        toast.error(t('auth.loginDisabled'));
    }
}, \[params]);
```

Optionally hide the "Continue with Google/Facebook" buttons when `!loginEnabled`.

\---

## 8\. Frontend — Admin user management (create staff)

New page `AdminUsers.jsx` (link it from the AdminDashboard, Management section).

### 8.1 API (adminUserApi.js)

```javascript
export const createStaff = (data) => axiosInstance.post('/admin/users', data);
export const listUsers   = (role) => axiosInstance.get('/admin/users', { params: { role } });
export const changeUserRole = (id, role) =>
    axiosInstance.patch(`/admin/users/${id}/role`, null, { params: { role } });
```

### 8.2 Create-staff form (essentials)

```jsx
const \[form, setForm] = useState({ fullName: '', email: '', password: '', role: 'EMPLOYEE' });

const handleCreate = async () => {
    try {
        await createStaff(form);
        toast.success(t('admin.staffCreated'));
        setForm({ fullName: '', email: '', password: '', role: 'EMPLOYEE' });
        fetchUsers();
    } catch (e) {
        toast.error(e.response?.data?.message || t('admin.staffCreateFailed'));
    }
};

// role select:
<select value={form.role} onChange={(e) => setForm(p => ({ ...p, role: e.target.value }))}>
    <option value="EMPLOYEE">{t('admin.roleEmployee')}</option>
    <option value="ADMIN">{t('admin.roleAdmin')}</option>
    <option value="CUSTOMER">{t('admin.roleCustomer')}</option>
</select>
```

Guard the route so only ADMIN can open it (your existing admin route guard / `RequireRole` wrapper).

\---

## 9\. i18n keys (en.json / sr.json)

```jsonc
// en.json
"settings": {
    "access": "Access",
    "registration\_enabled": "Customer Registration",
    "login\_enabled": "Customer Login"
},
"auth": {
    "registrationClosedTitle": "Registration Closed",
    "registrationClosed": "New account registration is currently unavailable.",
    "loginRestricted": "Customer login is temporarily unavailable. Staff may still sign in.",
    "loginDisabled": "Login is currently unavailable.",
    "loginFailed": "Invalid email or password."
},
"admin": {
    "staffCreated": "Account created",
    "staffCreateFailed": "Failed to create account",
    "roleEmployee": "Employee",
    "roleAdmin": "Admin",
    "roleCustomer": "Customer"
}

// sr.json
"settings": {
    "access": "Pristup",
    "registration\_enabled": "Registracija kupaca",
    "login\_enabled": "Prijava kupaca"
},
"auth": {
    "registrationClosedTitle": "Registracija zatvorena",
    "registrationClosed": "Registracija novih naloga trenutno nije dostupna.",
    "loginRestricted": "Prijava kupaca je trenutno nedostupna. Osoblje i dalje može da se prijavi.",
    "loginDisabled": "Prijava trenutno nije dostupna.",
    "loginFailed": "Pogrešan email ili lozinka."
},
"admin": {
    "staffCreated": "Nalog kreiran",
    "staffCreateFailed": "Kreiranje naloga nije uspelo",
    "roleEmployee": "Zaposleni",
    "roleAdmin": "Administrator",
    "roleCustomer": "Kupac"
}
```

\---

## 10\. Security \& edge cases

* **Authenticate before gating.** Always verify credentials first, then apply the role/toggle check — prevents account enumeration and never blocks staff.
* **Staff bypass.** The login gate only rejects `CUSTOMER`. `ADMIN`/`EMPLOYEE` always pass, so the shop stays manageable in maintenance mode.
* **Don't lock yourself out.** The "last admin" guard in §5.4 prevents demoting/removing the final admin.
* **Token endpoints.** Refresh-token / "remember me" flows should also respect `login\_enabled` for customers if you have them.
* **Existing customer sessions.** Disabling login doesn't invalidate already-issued JWTs. If you need to force customers out immediately, that's a separate token-revocation concern — note it but it's out of scope here.
* **First admin bootstrap.** Ensure at least one ADMIN exists before enabling these toggles (seed via a Flyway data migration or a one-time bootstrap), otherwise creating staff is impossible once login is restricted.
* **URL-level protection.** Add `/api/admin/\*\*` → `hasRole('ADMIN')` in the `@Order(1)` API filter chain in addition to `@PreAuthorize`.

\---

## 11\. Checklist

* \[ ] Flyway: `registration\_enabled`, `login\_enabled` settings inserted
* \[ ] `isSettingEnabled` helper + `StoreSettingsRepository` injected into AuthService
* \[ ] `register()` gated → 403 when disabled
* \[ ] `login()` gated by role (customers blocked, staff pass) → 403
* \[ ] OAuth2 success handler applies the same customer gate
* \[ ] `CreateStaffRequest` + `createStaff` service (ADMIN-only) + `AdminUserController`
* \[ ] Last-admin demotion guard
* \[ ] AdminSettings: both toggles render as checkboxes (in sections AND render-chain list)
* \[ ] RegisterPage blocks/redirects when disabled; register link hidden on login
* \[ ] LoginPage keeps form + notice; handles 403 + `?error=login\_disabled`
* \[ ] AdminUsers page: create staff + list + change role (ADMIN route-guarded)
* \[ ] i18n keys (EN + SR)
* \[ ] Verified: customer blocked, admin/employee can log in, admin can create staff

