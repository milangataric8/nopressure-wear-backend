# No Pressure SQL seed

## Opisi i footer sa sajta

Opisi za sva cetiri modela i brand tekst footera preuzeti su sa https://nopressurewear.com/ dana 2026-10-21, na izvornom engleskom jeziku. Ukljuceni su u glavni nopressure.sql. Preuzet je uvodni opis bez Shopify sekcija sa tabelama velicina i uslovima isporuke, jer lokalna aplikacija ima sopstvene velicine i checkout.

Ako su proizvodi vec ubaceni, izvrsi samo `update-site-content.sql` u pgAdmin-u ili DBeaver-u. Ne treba prazniti bazu ili ponovo praviti nalog. Skripta azurira opise za 20 SKU/boja i `store_tagline`, bez menjanja cena, zaliha i kontakata. Ako je prethodna transakcija u gresci, prvo izvrsi `ROLLBACK;`. Nakon direktne SQL izmene restartuj backend da osvezis kes podesavanja.

Postojeci frontend vec prikazuje sliku potpisa `src/assets/nopressure_signature_footer.png` ispod opisa i ispod footera. Zato opisi i store_tagline ne sadrze dodatni img element, da se logo ne prikazuje dvaput. Izvorni URL-ovi proizvoda navedeni su uz UPDATE upite.

Za praznjenje podataka pre seedera koristi `clear-data.sql`: brise korisnike, proizvode, porudzbine i ostale prodajne podatke i resetuje njihove ID sekvence. Cuva store_settings, filter_config, legal_content i Flyway istoriju. Opcioni zakomentarisani TRUNCATE brise i konfiguraciju; seeder je ne obnavlja. Zaustavi backend pre izvrsavanja. SQL nije automatski izvrsen.

Ako zelis da sacuvas podatke i ispravis samo prijavljenu gresku category_pkey, izvrsi `fix-category-sequence.sql`. Ako je prethodni upit ostavio transakciju u gresci, prvo izvrsi `ROLLBACK;`.

Otvori `nopressure.sql` u pgAdmin Query Tool-u ili DBeaver SQL editoru, izaberi bazu aplikacije i izvrsi ceo fajl. Potrebne su Flyway migracije zakljucno sa V63.

Fajl koristi obicne PostgreSQL INSERT upite, bez privremenih tabela, procedura i psql promenljivih. Svaki proizvod ima zaseban INSERT i eksplicitne redove za S/M/L/XL. ID-evi se pronalaze podupitima, bez fiksnih numerickih ID-eva.

- SUPER_ADMIN: `milan.gataric93@gmail.com`, sa trazenom lozinkom sacuvanom kao BCrypt hash.
- Ako nalog postoji, postavlja se trazena lozinka i SUPER_ADMIN uloga, aktivira i verifikuje nalog, resetuje blokada i reset token. Ako se podaci promene, povecava se token_version radi ponistavanja starih JWT tokena.
- Moto, C Row, Signature, Circle: svaki u Black, Green, Ivory, Navy Blue i White boji.
- Kategorija No Pressure T-Shirts, 20 proizvoda/slika, 80 varijanti velicine i 80 usmerenih veza boja.
- Pocetna cena 2990.00, zaliha 10 po velicini. Za druge vrednosti izmeni brojeve u INSERT upitima. Valuta prodavnice se ne menja (pretpostavka je RSD).
- Ponovljeno izvrsavanje ne duplira podatke i cuva postojece cene i zalihe. Identitet proizvoda je SKU + boja; duplikati tog para prekidaju podupit i transakciju.

Sve izmene su u jednoj transakciji. Pri gresci u interaktivnom editoru izvrsi `ROLLBACK;` pre ponovnog pokusaja. Tokom izvrsavanja tabele su zakljucane za konkurentne upise.

## Slike i opciona PowerShell skripta

Slike su vec kopirane u backend `uploads/products`. SQL putanje su `/uploads/products/<ime>.jpg`.

```powershell
# Samo kopiranje slika, bez pristupa bazi:
.\scripts\seed\seed-nopressure.ps1 -PrepareOnly

# Kopiranje slika i izvrsavanje SQL-a:
.\scripts\seed\seed-nopressure.ps1
```

Izvor slika: `C:\milan\projects_media_&_vectors\no_pressure`. Opcije `-MediaRoot` i `-UploadDir` menjaju direktorijume; UploadDir treba da odgovara `app.upload.dir`. Postojeca slika drugacijeg sadrzaja se ne prepisuje.

Podrazumevana konekcija: localhost:5432, baza nopressure_wear, korisnik postgres. Opcije: `-Psql`, `-DatabaseHost`, `-Port`, `-Database`, `-DatabaseUser`. Lozinku baze trazi psql; nije sacuvana u skripti. Cena i zaliha menjaju se direktno u SQL-u; ranije opcije Price, StockPerSize i UpdateExistingAdmin vise se ne koriste.

Direktno kroz psql:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -U postgres -d nopressure_wear -v ON_ERROR_STOP=1 -f .\scripts\seed\nopressure.sql
```

Seeder nije automatska Flyway migracija i ne poziva email ili Stripe servise.
