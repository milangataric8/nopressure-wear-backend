# Spec (BACKEND): Prinudna promena lozinke pri prvom logovanju

**Projekat:** nopressure-wear-backend (Spring Boot 4.1 / Java 25)
**Povezano:** `backend-role-based-access.md`, `backend-employee-role-edit.md`
**Frontend parnjak:** `frontend-forced-password-change.md`

**Kontekst:** SUPER_ADMIN kreira nalog zaposlenom i zadaje mu početnu lozinku.
Ta lozinka je poznata i onome ko je kreirao nalog, pa mora biti promenjena pri
prvom ulasku.

---

## 1. Ključna stvar koja se lako pogreši

Prinudna promena lozinke **nije redirect na frontendu**. Ako je gate samo u React
ruteru, korisnik sa privremenom lozinkom ima potpuno validan JWT i može direktno
da poziva sve endpointe koje mu rola dozvoljava — samo zaobiđe UI.

Backend mora da blokira sve osim promene lozinke dok je flag podignut.
Frontend redirect je samo UX.

---

## 2. Model + migracija

```sql
ALTER TABLE users
ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
```

Postojeći nalozi ostaju `FALSE` — ne forsirati promenu ljudima koji već rade.

**Flag se postavlja na `TRUE`:**
- pri kreiranju zaposlenog od strane SUPER_ADMIN-a (uvek),
- kada SUPER_ADMIN kroz edit formu upiše novu lozinku za **tuđi** nalog
  (reset zaboravljene lozinke — ista situacija, ista opasnost).

**Flag se ne postavlja:**
- kada korisnik sam menja svoju lozinku,
- kod self-service "zaboravljena lozinka" preko email tokena, ako postoji —
  tu korisnik sam bira lozinku i niko je drugi ne zna.

---

## 3. JWT claim

Dodati claim `pwdChange` (boolean) pri izdavanju tokena, da filter ne mora da
gađa bazu za svaki request.

Posledica: pri uspešnoj promeni lozinke mora se izdati **novi token** — stari i
dalje nosi `pwdChange: true` i korisnik bi ostao zaključan. Kombinovati sa
`tokenVersion` bump-om.

---

## 4. Filter

Registrovati **nakon** `JwtAuthenticationFilter`:

```java
private static final Set<String> ALLOWED_WHEN_PASSWORD_CHANGE_REQUIRED = Set.of(
        "/api/auth/change-password",
        "/api/auth/me",
        "/api/auth/logout",
        "/api/auth/refresh"
);
```

Ako je `pwdChange == true` i putanja nije u listi:

```json
{
  "status": 403,
  "code": "PASSWORD_CHANGE_REQUIRED",
  "message": "Morate promeniti lozinku pre nastavka."
}
```

**Polje `code` je obavezno** — frontend po njemu razlikuje ovaj 403 od običnog
"nemate ovlašćenje" i radi redirect umesto toast-a.

Dve stvari koje se ovde previđaju:
- `OPTIONS` preflight requestovi moraju proći, inače frontend puca na CORS-u pre
  nego što uopšte vidi 403.
- Whitelist držati eksplicitnom. `/api/auth/**` bi propustilo i login i register —
  nije rupa sama po sebi, ali eksplicitna lista je jasnija i teže se pokvari.

---

## 5. Endpoint

```
POST /api/auth/change-password
Body: { "currentPassword": "...", "newPassword": "...", "confirmPassword": "..." }
→ 200 { "token": "<novi JWT>" }
→ 400 lozinke se ne poklapaju / nova ne zadovoljava pravila / nova == trenutna
→ 401 currentPassword netačna
```

Pravila za novu lozinku — **uskladiti sa postojećim pravilima na registraciji**,
ne uvoditi drugačija:
- minimum 8 karaktera,
- mora se razlikovati od trenutne,
- `newPassword` == `confirmPassword`.

Na uspeh, redom:
1. `passwordEncoder.encode(newPassword)` → snimi,
2. `passwordChangeRequired = false`,
3. `tokenVersion++`,
4. izdaj i vrati novi JWT sa `pwdChange: false`,
5. `log.info("PASSWORD_CHANGED user={}", email)`.

**Rate limiting:** projekat već ima account lockout mehanizam iz ranijeg feature-a.
Primeniti ga i na `currentPassword` proveru ovde — inače je ovo endpoint na kome
se privremena lozinka može brute-force-ovati bez ograničenja.

---

## 6. Login response

Login mora da vrati flag da frontend zna gde da usmeri korisnika:

```json
{
  "token": "...",
  "user": { "id": 5, "email": "...", "role": "EMPLOYEE" },
  "passwordChangeRequired": true
}
```

Isti flag vraćati i iz `GET /api/auth/me`, jer se frontend na njega oslanja pri
refresh-u stranice.

---

## 7. Testovi

| Test | Očekivano |
|---|---|
| Novokreiran zaposleni → login | 200, `passwordChangeRequired: true` |
| Taj token → `GET /api/admin/products` | 403 `PASSWORD_CHANGE_REQUIRED` |
| Taj token → `POST /api/auth/change-password` | 200, vraća novi token |
| Novi token → `GET /api/admin/products` | 200 |
| Stari token nakon promene | 401 |
| `newPassword` == trenutna | 400 |
| `newPassword` != `confirmPassword` | 400 |
| Pogrešna `currentPassword` | 401 |
| Više pogrešnih pokušaja zaredom | lockout kao i na loginu |
| SUPER_ADMIN resetuje tuđu lozinku → taj user login | `passwordChangeRequired: true` |
| Korisnik sam menja lozinku | flag ostaje `false` |
| Postojeći nalog (flag false) → login | normalan pristup |
| `OPTIONS` preflight sa podignutim flagom | prolazi |

---

## 8. Acceptance criteria

- [ ] Novokreiran zaposleni ne može da pozove nijedan endpoint osim whitelist-e.
- [ ] Direktan Postman poziv sa tim tokenom vraća 403 sa `code: PASSWORD_CHANGE_REQUIRED`.
- [ ] Nakon promene lozinke korisnik dobija novi token i radi normalno bez ponovnog logina.
- [ ] Stari token prestaje da važi.
- [ ] Nova lozinka ne može biti ista kao privremena.
- [ ] Admin reset tuđe lozinke ponovo podiže flag.
- [ ] Postojeći nalozi nisu pogođeni.
- [ ] Endpoint je pokriven istim lockout mehanizmom kao login.

## 9. Redosled implementacije

1. Migracija + polje na entitetu.
2. Postavljanje flag-a pri kreiranju zaposlenog i pri admin reset-u lozinke.
3. `pwdChange` claim u tokenu.
4. Filter sa whitelist-om.
5. `POST /api/auth/change-password` + izdavanje novog tokena.
6. Flag u login i `/me` response-u.
7. Testovi + **obavezna ručna provera kroz Postman** — UI gate ne dokazuje ništa.
