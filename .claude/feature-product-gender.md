# Feature: Product Gender (Men / Women / Unisex) — Field, Filter & Nav Sections

NoPressure Wear — add a `gender` attribute to products (MEN / WOMEN / UNISEX), selected in the admin product form, used as a storefront filter, and surfaced as separate Men/Women navigation sections.

Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, React + Vite + Tailwind, react-i18next (EN/SR). Package root: `rs.nopressurewear`.

> **Design rationale:** gender is a **product attribute** (like `brand` / `material` / `category`), NOT a per-stock variant like sizes. A men's and women's shirt are different products with their own stock/cut/images — so one product table with a `gender` enum is correct. Men/Women nav sections are just pre-filtered links (`/products?gender=MEN`). UNISEX products should appear under **both** Men and Women browsing.

---

## 1. Database — Flyway migration

```sql
ALTER TABLE product ADD COLUMN gender VARCHAR(20) NOT NULL DEFAULT 'UNISEX';
```

Existing products default to `UNISEX` (so they keep showing everywhere until reclassified).

---

## 2. Enum — Gender.java

```java
package rs.nopressurewear.model;

public enum Gender {
    MEN, WOMEN, UNISEX
}
```

---

## 3. Entity — Product.java

```java
@Enumerated(EnumType.STRING)
@Column(name = "gender", nullable = false, length = 20)
private Gender gender = Gender.UNISEX;
```

---

## 4. DTOs

### ProductRequest.java
```java
private Gender gender;
```

### ProductResponse.java
```java
private Gender gender;
```

---

## 5. ProductService — set on create/update, return in response

Create / update:
```java
product.setGender(request.getGender() != null ? request.getGender() : Gender.UNISEX);
```

`toResponse`:
```java
.gender(product.getGender())
```

### Filtering logic (the important part — UNISEX shows in both)

When a customer browses MEN, they should see MEN **and** UNISEX products; WOMEN sees WOMEN **and** UNISEX. Only an explicit "Unisex" filter (if you offer one) shows UNISEX alone.

Extend your existing native filter query. Add a `gender` param and this predicate:

```sql
AND (
    :gender IS NULL
    OR product.gender = :gender
    OR product.gender = 'UNISEX'
)
```

> This means filtering by MEN returns MEN + UNISEX. If you ever want a strict "exactly this gender" mode, add a separate flag; for normal shopping, including UNISEX is the expected behavior.

Service method signature gains a `String gender` (or `Gender gender`) param, passed through `filter(...)` / `getActiveFiltered(...)` exactly like `brand`/`material`:

```java
String genderParam = (nonNull(gender) && !gender.isBlank()) ? gender : null;
// pass genderParam into productRepository.findByFilters(..., genderParam, ...)
```

Update the repository `findByFilters` / `findActiveByFilters` signatures + `@Query` to include the new param and the predicate above. Remember: native query needs the param in BOTH the `value` and `countQuery`.

---

## 6. Controller — accept the gender query param

In `ProductController`, add `@RequestParam(required = false) String gender` to the listing/search endpoints (`getAll`, `search`) and pass it into the service. Include it in the `haveFilters(...)` check:

```java
@GetMapping
public ResponseEntity<Page<ProductResponse>> getAll(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String colorName,
        @RequestParam(required = false) String material,
        @RequestParam(required = false) String gender,   // ← new
        @PageableDefault(sort = "name") Pageable pageable) {

    if (haveFilters(categoryId, search, active, brand, colorName, material, gender)) {
        return ResponseEntity.ok(productService.filter(
                categoryId, search, active, brand, colorName, material, gender, pageable));
    }
    return ResponseEntity.ok(productService.getAll(pageable));
}
```

Add `gender` to the `haveFilters` boolean OR-chain.

### Optional: available filters
If you want a "Gender" facet in the sidebar driven by what's in the catalog, add `findDistinctGenders()` and include it in `getAvailableFilters()`. (Often unnecessary since the three values are fixed — you can hardcode the three options in the UI.)

---

## 7. Frontend — Admin product form (dropdown)

Add `gender` to `formData` (default `'UNISEX'`), `resetForm`, and `handleEdit` (`gender: product.gender ?? 'UNISEX'`).

```jsx
<div>
    <label className={labelClass}>{t('product.gender')}</label>
    <select
        value={formData.gender}
        onChange={(e) => setFormData(prev => ({ ...prev, gender: e.target.value }))}
        className={inputClass}
    >
        <option value="MEN">{t('product.genderMen')}</option>
        <option value="WOMEN">{t('product.genderWomen')}</option>
        <option value="UNISEX">{t('product.genderUnisex')}</option>
    </select>
</div>
```

---

## 8. Frontend — Storefront filter + nav sections

### 8.1 Navbar — Men / Women sections

Add nav links that route to the products page pre-filtered by gender:

```jsx
<Link to="/products?gender=MEN" className="...">{t('nav.men')}</Link>
<Link to="/products?gender=WOMEN" className="...">{t('nav.women')}</Link>
{/* keep the existing "Products"/all link too if you want an all-items view */}
```

### 8.2 ProductsPage — read gender from the URL

```jsx
const [searchParams, setSearchParams] = useSearchParams();
const [selectedGender, setSelectedGender] = useState(searchParams.get('gender') || '');

// keep state in sync when the URL changes (clicking Men then Women)
useEffect(() => {
    setSelectedGender(searchParams.get('gender') || '');
    setPage(0);
}, [searchParams]);
```

Include `gender` in the fetch params and the `useEffect`/`useCallback` dependency array (same pattern as your other filters — this avoids the stale-closure bug):

```jsx
const params = {
    // ...existing: categoryId, search, brand, colorName, material, price...
    gender: selectedGender || undefined,
    page, size,
};
```

### 8.3 Optional gender filter control in the sidebar/bar

If you also want an in-page toggle (not just nav), add three buttons that set `selectedGender` and update the URL:

```jsx
{['MEN', 'WOMEN', 'UNISEX'].map(g => (
    <button
        key={g}
        onClick={() => {
            const next = selectedGender === g ? '' : g;
            setSelectedGender(next);
            setPage(0);
            const sp = new URLSearchParams(searchParams);
            if (next) sp.set('gender', next); else sp.delete('gender');
            setSearchParams(sp);
        }}
        className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-wide border transition-colors ${
            selectedGender === g ? 'bg-black text-white border-black' : 'border-gray-300 text-gray-500 hover:border-black'
        }`}
    >
        {t(`product.gender${g.charAt(0) + g.slice(1).toLowerCase()}`)}
    </button>
))}
```

> Syncing `selectedGender` to the URL means the Men/Women nav links, the in-page toggle, and shareable URLs all stay consistent. Clear-all should also remove the `gender` param.

### 8.4 Page heading (nice touch)

Show a contextual heading when a gender is active: "Men" / "Women" instead of generic "Products", driven by `selectedGender`.

### 8.5 Product detail (optional)

Display the gender as a small label near the brand/material if useful (`t('product.gender' + ...)`).

---

## 9. i18n keys (en.json / sr.json)

```jsonc
// en.json
"nav": { "men": "Men", "women": "Women" },
"product": {
    "gender": "Gender",
    "genderMen": "Men",
    "genderWomen": "Women",
    "genderUnisex": "Unisex"
}

// sr.json
"nav": { "men": "Muškarci", "women": "Žene" },
"product": {
    "gender": "Pol",
    "genderMen": "Muški",
    "genderWomen": "Ženski",
    "genderUnisex": "Uniseks"
}
```

---

## 10. Edge cases & notes

- **UNISEX appears in both** Men and Women browsing (the `OR gender = 'UNISEX'` predicate). This is usually what shoppers expect; if you want strict separation, drop the UNISEX clause and give UNISEX its own filter only.
- **Existing products** default to UNISEX, so nothing disappears; reclassify them in admin over time.
- **URL is the source of truth** for the selected gender so nav links + in-page toggle + sharing all agree; sync state to `searchParams`.
- **Dependency arrays:** add `selectedGender` to the products fetch `useEffect`/`useCallback` deps (recurring stale-closure gotcha in this codebase).
- **Native query:** the new `gender` predicate must be added to BOTH the `value` and `countQuery` of `findByFilters`.
- **Categories stay orthogonal:** keep categories about product *type* (T-Shirts, Hoodies) and gender as its own field, so "Women's Hoodies" = category + gender filter combined.

---

## 11. Checklist

- [ ] Flyway: `product.gender` column (default UNISEX)
- [ ] `Gender` enum, `Product.gender` field
- [ ] ProductRequest/Response include `gender`
- [ ] ProductService sets gender on create/update, returns it
- [ ] Filter query includes gender predicate (MEN/WOMEN also match UNISEX) in value + countQuery
- [ ] Controller accepts `gender` param + included in haveFilters
- [ ] Admin form: gender dropdown (formData, resetForm, handleEdit)
- [ ] Navbar: Men / Women links → `/products?gender=...`
- [ ] ProductsPage reads gender from URL, syncs state, includes in fetch + deps
- [ ] Optional in-page gender toggle synced to URL
- [ ] Contextual page heading per gender
- [ ] i18n keys (EN + SR)
- [ ] Verified: Men shows men+unisex, Women shows women+unisex, nav + filter + URL stay in sync
