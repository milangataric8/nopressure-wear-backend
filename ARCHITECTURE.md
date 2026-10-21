# Backend: modularna organizacija

Jedna Spring Boot aplikacija, jedan Maven artefakt i jedna PostgreSQL baza.
Build ostaje na Java 25 i Spring Boot 4.1.0, prema `pom.xml`.

## Moduli

| Paket u `rs.nopressurewear` | Vlasništvo |
| --- | --- |
| `identity` | Korisnici, adrese, zaposleni, autentikacija, OAuth2, JWT i bezbednosni filteri |
| `catalog` | Proizvodi, kategorije, varijante, zalihe, filteri i recenzije |
| `shopping` | Korpa, omiljeni proizvodi i podsetnici za napuštene korpe |
| `ordering` | Porudžbine, checkout i pravila obračuna dostave |
| `promotions` | Kuponi i popusti |
| `payments` | Stripe integracija, PaymentIntent i webhook |
| `storefront` | Podešavanja, lokacije, baneri, popup, pravni sadržaj i sitemap |
| `notifications` | Email šabloni, transport, retry, kontakt i kanali obaveštavanja |
| `media` | Upload, skladištenje, Cloudinary i uklanjanje pozadine |
| `reporting` | Dashboard i izveštaji |
| `platform` | Tehnička konfiguracija, zajednički HTTP ugovor grešaka i tekstualne/locale funkcije |

Zalihe i recenzije za sada pripadaju katalogu. `ProductStore` predstavlja vezu proizvoda sa lokacijom i pripada katalogu; lokacije pripadaju `storefront`.

## Unutrašnja struktura

```text
ordering/
  api/                       # Ugovori dostupni drugim modulima
    DeliveryPricing.java
    OrderStatus.java
    dto/order/
  internal/
    web/                     # REST adapteri
    application/             # Poslovne operacije i transakcije
    domain/                  # JPA entiteti
    persistence/             # Spring Data repozitorijumi
```

`api` označava Java ugovor, a REST kontroleri pripadaju `internal.web`.
Moduli nemaju zasebne procese niti zasebne Spring kontekste. Glavna aplikaciona klasa
ostaje u korenom paketu, pa component/entity/repository scanning obuhvata sve module.
URL rute, JSON polja, imena Spring bean-ova, tabele i Flyway migracije ostaju isti.

Testovi prate pakete odgovarajućih modula. `architecture` sadrži samo testove granica.

## Izdvojeni ugovori

- `storefront.api.SettingsQuery`: read-only pristup vrednostima podešavanja, bez izlaganja JPA entiteta.
- `notifications.api.EmailOperations`: sinhroni ugovor za postojeće email operacije.
- `notifications.api.CartReminderItem`: nepromenljiv snimak artikla; email modul ne pristupa korpi ili lazy proizvodima.
- `ordering.api.DeliveryPricing`: obračun dostave koji koristi modul plaćanja.
- `platform.api.LocalizedRecipient`: minimalni pogled na locale primaoca, nezavisan od korisničkog entiteta.

Izvršavanje email operacija zadržava postojeću sinhronu semantiku. Ova reorganizacija ne uvodi događaje,
outbox, rezervacije zaliha ili promene transakcionih granica checkout-a.

## Pravila i automatske provere

`ModuleBoundariesTest` se izvršava kroz standardni `mvn test` i bez baze proverava:

1. Kod pripada imenovanom modulu, a paket odgovara putanji fajla.
2. Javni API ugovori ne referenciraju interne klase, ni sopstvenog ni drugih modula.
3. `platform` ne zavisi od poslovnih modula.
4. Nema novih direktnih referenci na interne klase drugog modula.
5. Razrešeni izuzeci moraju biti uklonjeni iz evidencije.
6. Projektni wildcard importi nisu dozvoljeni, da ne bi sakrili zavisnosti.

Provera analizira Java izvore, eksplicitne importe i potpuno kvalifikovane reference.
Ne analizira refleksiju, SQL stringove ili runtime pozive preko SpEL-a.

## Preostala izolacija: eksplicitni migracioni dug

Ovo je sprovedena reorganizacija po modulima sa početnim javnim ugovorima i zabranom
novog preplitanja. **Nije završena potpuna izolacija svih domena.**

Postojeći checkout, Stripe, izveštaji i JPA modeli i dalje imaju direktne zavisnosti
između modula. Konkretne reference evidentirane su u
`src/test/resources/architecture/legacy-module-access.txt`.
Ta evidencija nije javni API i ne treba je proširivati radi prolaska testa.
Novi kod koristi `api`; pri razrešavanju stare veze uklanja se odgovarajući red.
Preostali ciklusi implementacije znače da globalna provera acikličnosti modula još ne može proći.

Dalji rad na strogoj izolaciji:

1. Uvesti uske upite/komande kataloga, korisnika, korpe i kupona za checkout i Stripe.
2. Zameniti međumodulske JPA relacije identifikatorima i snimcima gde poslovna pravila to zahtevaju.
3. Izdvojiti projekcije/upite za reporting umesto direktnog pristupa tuđim repozitorijumima.
4. Razdvojiti potvrdu plaćanja od porudžbina pomoću eksplicitnih ugovora/događaja.
5. Posle uklanjanja evidencije izuzetaka uključiti potpunu proveru acikličnosti.

Takve izmene menjaju poslovne i transakcione granice i treba ih proveravati zasebno
od promene paketa. Originalni `MODULARNI_MONOLIT.md` ostaje predlog šire ciljne arhitekture.

## Provera

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd test '-Dtest=ModuleBoundariesTest'
```

Integracioni testovi koriste profil `ci` sa `ddl-auto: create-drop`.
`DATABASE_URL` mora pokazivati na namensku test bazu, ne razvojnu bazu sa podacima.
Pomeranje paketa samo po sebi ne zahteva novu Flyway migraciju.

Validacija reorganizacije: `mvnw -o -B clean verify` uspešno je završio sa 167 testova,
bez grešaka i preskočenih testova. Korišćena je privremena PostgreSQL 18 instanca
na `127.0.0.1:55432` sa namenskom bazom `modular_backend_test`.
