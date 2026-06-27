# Feature: Gender Filter on AdminProducts (below category filters)

NoPressure Wear — add a **gender** filter (Men / Women / Unisex) to the AdminProducts page, displayed **below the existing category filters**. Lets admins narrow the product list by gender.

Stack: React + Vite + Tailwind, react-i18next (EN/SR), Spring Boot backend. Builds on the existing **gender field** (`feature-product-gender.md`) — the product entity, DTOs, and the backend filter already accept `gender`.

> **Prerequisite:** `feature-product-gender.md` implemented. The backend `findByFilters` / `filter(...)` must already accept a `gender` param and the `ProductController` listing endpoint must expose it. This spec is **frontend-only** if that's done; §1 is a quick check.

---

## 1. Backend check (should already exist from the gender spec)

Confirm the admin product listing path supports `gender`:
- `ProductController.getAll(...)` accepts `@RequestParam(required = false) String gender` and includes it in `haveFilters(...)` and the `filter(...)` call.
- `ProductService.filter(...)` passes `gender` into the native `findByFilters` query (predicate present in BOTH `value` and `countQuery`).

> **Admin note:** unlike the storefront, the admin filter usually wants **exact** gender match (show only MEN when MEN is picked — NOT MEN + UNISEX). If your storefront predicate is `gender = :gender OR gender = 'UNISEX'`, that's fine for shoppers but the admin wants strictness. Options:
> - Reuse the same endpoint and accept that picking MEN also shows UNISEX in admin (simplest, often acceptable), OR
> - Add a strict mode: when an `exactGender` flag is true, use `(:gender IS NULL OR gender = :gender)` without the UNISEX clause. For admin filtering, exact is clearer.
>
> Recommended for admin: **exact match.** If your shared query includes the UNISEX clause, add an `exact` param or a dedicated admin filter that omits it. See §4.

If the admin list already calls a generic products endpoint with filters, no backend change is needed beyond ensuring `gender` is honored (and exact-match for admin, per above).

---

## 2. AdminProducts.jsx — state

Add the gender filter state near the other filter states (`colorFilter`, `brandFilter`, etc.):

```jsx
const [genderFilter, setGenderFilter] = useState('');   // '' | 'MEN' | 'WOMEN' | 'UNISEX'
```

Include it in the products fetch params and — **critically** — in the fetch `useEffect`/`useCallback` dependency array (recurring stale-closure bug in this codebase):

```jsx
const fetchProducts = useCallback(async () => {
    const params = {
        // ...existing: search, categoryId/selectedCategory, colorFilter, brandFilter, active, page, size...
        gender: genderFilter || undefined,
    };
    const res = await getProducts(params);
    // ...set products, totalPages...
}, [/* ...existing deps..., */ genderFilter, page, /* etc */]);

useEffect(() => { fetchProducts(); }, [fetchProducts]);
```

Reset to page 0 when the filter changes (handled in the button onClick below).

---

## 3. The gender filter UI (below category filters)

Place this block **immediately after** the category filter section in the AdminProducts JSX. Style mirrors your existing admin brand filter (pill buttons), so it reads consistently.

```jsx
{!showForm && (
    <div className="flex gap-2 mb-6 flex-wrap items-center">
        <span className="text-xs font-semibold uppercase tracking-wide text-gray-500 mr-2">
            {t('product.gender')}:
        </span>
        {['MEN', 'WOMEN', 'UNISEX'].map(g => (
            <button
                key={g}
                onClick={() => {
                    setGenderFilter(prev => prev === g ? '' : g);
                    setPage(0);
                }}
                className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-wide border transition-colors ${
                    genderFilter === g
                        ? 'bg-black text-white border-black'
                        : 'border-gray-300 text-gray-500 hover:border-black hover:text-black'
                }`}
            >
                {t(`product.gender${g.charAt(0) + g.slice(1).toLowerCase()}`)}
            </button>
        ))}
        {genderFilter && (
            <button
                onClick={() => { setGenderFilter(''); setPage(0); }}
                className="text-xs text-red-400 hover:text-red-600 underline ml-1"
            >
                {t('common.clear')}
            </button>
        )}
    </div>
)}
```

Notes:
- Clicking an active pill again toggles it off (`prev === g ? '' : g`).
- `setPage(0)` so filtering starts from the first page.
- The `{t(`product.gender${...}`)}` resolves to `product.genderMen` / `genderWomen` / `genderUnisex` (keys from the gender spec).
- `!showForm` guard hides the filter while the add/edit form is open, matching your other admin filter bars.
- A small "Clear" link appears only when a gender is selected.

> **Placement:** put this directly under the category filter block in the JSX tree so it renders below it, as requested.

---

## 4. (Optional) exact-match for admin

If your backend gender predicate includes the UNISEX clause (so MEN shows MEN + UNISEX), and you want the admin filter to be strict (MEN shows only MEN):

### Backend — add an `exact` flag
```java
// ProductController
@RequestParam(required = false) String gender,
@RequestParam(required = false, defaultValue = "false") boolean exactGender,
```
Pass `exactGender` into the service/query. In the native query, choose the predicate by flag — simplest is two query methods or a conditional:
```sql
-- when exactGender = true
AND (:gender IS NULL OR product.gender = :gender)
-- when exactGender = false (storefront)
AND (:gender IS NULL OR product.gender = :gender OR product.gender = 'UNISEX')
```
> Implementing two predicates in one native query is awkward; a clean approach is a separate admin filter method, or pass a second param that "disables" the UNISEX branch:
> `AND (:gender IS NULL OR product.gender = :gender OR (:includeUnisex = TRUE AND product.gender = 'UNISEX'))`
> Then admin sends `includeUnisex=false`, storefront sends `true`.

### Frontend — admin sends exact
```jsx
const params = {
    gender: genderFilter || undefined,
    includeUnisex: false,   // admin = exact match
    // ...
};
```

If exact-vs-inclusive doesn't matter to you for admin, skip §4 entirely and just reuse the existing endpoint.

---

## 5. Combine with existing filters

The gender filter should stack with category/brand/color/search (AND logic) — selecting "MEN" + a category shows men's products in that category. Since they're all separate params on the same fetch, this works automatically as long as `genderFilter` is in the params + deps (§2). No extra logic needed.

Also include `genderFilter` in any "Clear all filters" handler you have, so it resets with the rest:
```jsx
const clearAllFilters = () => {
    setSelectedCategory('');
    setColorFilter('');
    setBrandFilter('');
    setGenderFilter('');   // ← add
    setSearch('');
    setPage(0);
};
```

---

## 6. i18n keys

These come from the gender spec; confirm they exist (en.json / sr.json):

```jsonc
// en.json — "product"
"gender": "Gender",
"genderMen": "Men",
"genderWomen": "Women",
"genderUnisex": "Unisex"

// sr.json — "product"
"gender": "Pol",
"genderMen": "Muški",
"genderWomen": "Ženski",
"genderUnisex": "Uniseks"
```

`common.clear` should already exist; if not:
```jsonc
// en: "clear": "Clear"   |   sr: "clear": "Obriši"
```

---

## 7. Edge cases & notes

- **Dependency array:** `genderFilter` MUST be in the fetch `useEffect`/`useCallback` deps, or the list won't refresh when you change it (stale-closure bug).
- **Page reset:** always `setPage(0)` on filter change so you don't land on an out-of-range page.
- **Toggle off:** clicking the active pill clears the filter (back to all genders).
- **`!showForm` guard:** keep the filter hidden while editing/creating, consistent with your other admin filter bars.
- **Exact vs inclusive:** decide whether admin MEN should include UNISEX (§4). For admin clarity, exact is usually better; for zero backend work, inclusive (reusing the storefront query) is fine.
- **Optional dropdown alternative:** if pills feel heavy, a `<select>` works too:
  ```jsx
  <select value={genderFilter} onChange={(e) => { setGenderFilter(e.target.value); setPage(0); }}>
      <option value="">{t('common.all')}</option>
      <option value="MEN">{t('product.genderMen')}</option>
      <option value="WOMEN">{t('product.genderWomen')}</option>
      <option value="UNISEX">{t('product.genderUnisex')}</option>
  </select>
  ```

---

## 8. Checklist

- [ ] Gender field + backend filter param implemented (prerequisite)
- [ ] `genderFilter` state added to AdminProducts
- [ ] `genderFilter` included in fetch params AND dependency array
- [ ] Gender pill filter rendered directly below the category filters (`!showForm` guarded)
- [ ] Toggle-off + Clear link + `setPage(0)` on change
- [ ] Included in "Clear all filters" handler
- [ ] (Optional) exact-match for admin (`includeUnisex=false`) if backend uses the UNISEX-inclusive predicate
- [ ] i18n keys confirmed (EN + SR)
- [ ] Verified: filtering by each gender narrows the list; stacks with category/brand/search; refreshes correctly
