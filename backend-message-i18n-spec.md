# Spec: Backend Message Localization (i18n)

**Repo:** `nopressure-wear-backend`
**Related frontend spec:** `password-validation-frontend-spec.md`
**Status:** ready for implementation

---

## 1. Problem

User-facing messages produced by the backend are hardcoded English or Serbian sentences, so they never follow the
language the user selected in the UI. Two distinct sources:

- **Bean Validation messages** — literal English sentences in DTO annotations
  (`@Pattern(message = "Password must contain at least one number and one special character")`).
- **Messages written directly into the response by filters and exception handlers** — e.g. the hardcoded Serbian
  string in `PasswordChangeRequiredFilter`:
  `{"status":403,"code":"PASSWORD_CHANGE_REQUIRED","message":"Morate promeniti lozinku pre nastavka."}`

Result: a user on the English UI can get a Serbian 403, and a user on the Serbian UI gets English validation errors.

---

## 2. Decision: where does translation happen?

Two viable strategies. **Pick strategy A as the default; use B only where noted.**

### Strategy A — backend returns a key, frontend translates (preferred)

The backend never returns human text. It returns a stable machine code, and the frontend maps it through
`react-i18next`.

Rationale: the frontend already owns the translation files, `FormField` already calls `t(error)`, and adding a
language means editing one JSON per locale in one repo instead of two.

Applies to: all validation messages, all business-rule errors consumed by the SPA.

### Strategy B — backend translates via `MessageSource` + `Accept-Language`

Used only where the response is consumed by something that cannot translate:

- transactional emails (order confirmation, email verification, abandoned cart, low-stock alerts)
- PDF invoices / any generated document
- webhook or third-party integration responses

Both strategies coexist. They are not alternatives to choose between globally — they are chosen per consumer.

---

## 3. Implementation — Strategy A

### 3.1 Error code catalogue

Create `rs.nopressurewear.constants.ErrorCode` as an enum, so codes are not stringly-typed across the codebase:

```java
public enum ErrorCode {
    PASSWORD_CHANGE_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORD_INVALID_FORMAT,
    PASSWORD_REQUIRED,
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    EMAIL_ALREADY_TAKEN,
    FIRST_NAME_REQUIRED,
    LAST_NAME_REQUIRED,
    ACCOUNT_LOCKED,
    // extend as encountered
}
```

Naming: `SCREAMING_SNAKE_CASE`, describing the condition, not the message text. Codes are part of the API contract —
once shipped, treat renames as breaking changes.

### 3.2 Validation annotations return i18n keys

Replace every literal `message = "..."` in DTOs under `rs.nopressurewear.dto` with the frontend i18n key:

```java
@NotBlank(message = "validation.firstName.required")
private String firstName;

@NotBlank(message = "validation.lastName.required")
private String lastName;

@NotBlank(message = "validation.email.required")
@Email(message = "validation.email.invalid")
private String email;

@Size(min = 8, message = "validation.password.minLength")
@Pattern(
        regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
        message = "validation.password.format"
)
private String password;
```

These keys must match exactly the keys defined in the frontend spec. `FormField` already runs `t(error)` and only
falls back to raw text when the key is unknown, so no frontend component change is needed.

DTOs to cover: `UserUpdateRequest`, `UserCreateRequest`, registration request, login request, change-password
request, address/checkout requests. Find them by searching for `message = "` under `dto`.

### 3.3 Consistent error response shape

Every error response — from `@RestControllerAdvice` and from filters alike — must use one shape:

```json
{
  "status": 403,
  "code": "PASSWORD_CHANGE_REQUIRED",
  "messageKey": "errors.passwordChangeRequired",
  "fieldErrors": {
    "password": "validation.password.format"
  }
}
```

- `code` — the `ErrorCode` enum name; used for programmatic branching on the client.
- `messageKey` — i18n key for the top-level toast/banner.
- `fieldErrors` — map of field name → i18n key; omitted when not a validation failure.
- **No** `message` field with literal text. Removing it prevents the frontend from silently falling back to English.

Introduce a `record ApiError(int status, String code, String messageKey, Map<String, String> fieldErrors)` and
serialize it with the shared `ObjectMapper` — including inside filters, so the filter and the exception handler
cannot drift apart.

### 3.4 Fix `PasswordChangeRequiredFilter`

Replace the hand-written JSON string with the shared shape:

```java
response.setStatus(HttpServletResponse.SC_FORBIDDEN);
response.setContentType(MediaType.APPLICATION_JSON_VALUE);
response.setCharacterEncoding(StandardCharsets.UTF_8.name());
objectMapper.writeValue(
        response.getWriter(),
        new ApiError(403, ErrorCode.PASSWORD_CHANGE_REQUIRED.name(),
                     "errors.passwordChangeRequired", null)
);
```

Inject `ObjectMapper` via the existing `@RequiredArgsConstructor`.

### 3.5 Frontend counterpart

Add to `sr.json` / `en.json`:

```json
"errors": {
    "passwordChangeRequired": "Morate promeniti lozinku pre nastavka.",
    "emailAlreadyTaken": "Email adresa je već u upotrebi.",
    "accountLocked": "Nalog je privremeno zaključan."
}
```

In the API error interceptor, read `messageKey` and render `t(messageKey)`; read `fieldErrors` and feed the values
straight into the form's `errors` object, where `FormField` translates them.

Add a defensive fallback: if `messageKey` is missing or unknown, show a generic `errors.unexpected` message rather
than a raw code.

---

## 4. Implementation — Strategy B (emails and documents only)

### 4.1 Message bundles

`src/main/resources/messages_sr.properties`:

```properties
email.verification.subject=Potvrdite vašu email adresu
email.verification.greeting=Zdravo {0},
email.order.confirmation.subject=Potvrda porudžbine #{0}
```

`src/main/resources/messages_en.properties` — same keys, English values.
`messages.properties` — fallback, English content.

### 4.2 Configuration

```java
@Bean
public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:messages");
    source.setDefaultEncoding("UTF-8");
    source.setDefaultLocale(Locale.forLanguageTag("sr"));
    source.setFallbackToSystemLocale(false);
    return source;
}
```

`setFallbackToSystemLocale(false)` matters — otherwise the server's OS locale leaks into output on a missing key,
which differs between local Windows and the Railway container.

### 4.3 Determining the user's language

Do **not** rely on `Accept-Language` for emails — the email is often sent asynchronously, long after the request
that triggered it, and there is no header at that point.

Instead: persist a `preferred_locale` column on the `users` table (Flyway migration), set it at registration from
the UI language and update it when the user switches. Email services read it from the `User` entity:

```java
Locale locale = Locale.forLanguageTag(user.getPreferredLocale());
String subject = messageSource.getMessage("email.verification.subject", null, locale);
```

Default existing rows to `sr` in the migration.

---

## 5. Out of scope

- Adding a third language. Structure must permit it; adding it is a separate task.
- Translating log messages, exception messages meant for developers, or Swagger descriptions — those stay English.
- Currency and date formatting (separate localization concern).

---

## 6. Acceptance criteria

- [ ] No literal user-facing sentence remains in any `message = "..."` under `dto`.
- [ ] `grep` for hardcoded Serbian or English text inside `security` filters returns nothing user-facing.
- [ ] Triggering `PASSWORD_CHANGE_REQUIRED` on the English UI shows the English message; on the Serbian UI, the
      Serbian one — with no backend change between the two.
- [ ] A validation failure returns `fieldErrors` keyed by field name, and each form field shows its message in the
      active UI language.
- [ ] An unknown or missing `messageKey` renders the generic fallback, never a raw code like
      `errors.somethingNew`.
- [ ] A verification email sent to a user with `preferred_locale = 'en'` arrives in English while the UI language of
      whoever triggered it is irrelevant.
