# Feature: Navbar "Products" → Men's / Women's → Category Subitems

NoPressure Wear — restructure the navbar so **Products** opens to **Men's** and **Women's**, and each shows the product-type subcategories (T-Shirts, Polo Shirts, …). Selecting one lands on the products page pre-filtered by both gender and category.

Stack: React + Vite + Tailwind, react-i18next (EN/SR), React Router. Builds on the existing **gender field** (`feature-product-gender.md`) and existing **categories**.

---

## Concept (read first)

This is a **presentation layer** over two filters you already have:
- **Gender** is a product field (`MEN` / `WOMEN` / `UNISEX`) → URL param `gender`.
- **Category** (T-Shirts, Polo Shirts, …) is your existing category entity → URL param `categoryId`.

The navbar combines them into links:

```
Products
├── Men's            → /products?gender=MEN
│   ├── T-Shirts     → /products?gender=MEN&categoryId=3
│   └── Polo Shirts  → /products?gender=MEN&categoryId=7
└── Women's          → /products?gender=WOMEN
    ├── T-Shirts     → /products?gender=WOMEN&categoryId=3
    └── Polo Shirts  → /products?gender=WOMEN&categoryId=7
```

> Categories are **gender-neutral and shared** — the same category list (T-Shirts, Polo) appears under both Men's and Women's; the `gender` param narrows the products. You do NOT create separate "Men's T-Shirts" and "Women's T-Shirts" categories. One category list + gender filter = every combination.

**Prerequisites:**
- Gender field implemented (`feature-product-gender.md`) — backend filter accepts `gender`, ProductsPage reads it from the URL.
- Categories exist (T-Shirts, Polo Shirts, …) as your normal product-type categories.

---

## 1. Data the navbar needs

The mega-menu needs the list of categories to show under each gender. Use the categories you want as top-level nav entries.

- If you want **all** active top-level (root) categories: fetch them via your existing categories endpoint (e.g. `getCategories()` / `getRootCategories()`).
- If you want only **specific** categories in the nav (e.g. just T-Shirts + Polo), either filter client-side by id/name, or add a `showInNav` boolean to the category (optional — see §6).

Fetch once in the Navbar (or a layout that wraps it):

```jsx
const [categories, setCategories] = useState([]);

useEffect(() => {
    getRootCategories()   // your existing API returning top-level categories
        .then(r => setCategories(r.data))
        .catch(() => setCategories([]));
}, []);
```

---

## 2. Desktop — Products mega-menu

A hover/click panel under "Products" with two columns (Men's, Women's), each listing the category links. Define the component **outside** the Navbar render (project rule: components defined outside other components).

```jsx
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const GENDERS = [
    { key: 'MEN',   labelKey: 'nav.men' },
    { key: 'WOMEN', labelKey: 'nav.women' },
];

const ProductsMegaMenu = ({ categories, onNavigate }) => {
    const { t } = useTranslation();

    return (
        <div className="absolute left-0 top-full bg-white border border-gray-200 shadow-lg z-50 flex gap-12 p-8 min-w-[420px]">
            {GENDERS.map(g => (
                <div key={g.key} className="min-w-[150px]">
                    {/* Column header → all products for that gender */}
                    <Link
                        to={`/products?gender=${g.key}`}
                        onClick={onNavigate}
                        className="block text-xs font-black uppercase tracking-wide text-black mb-3 hover:underline"
                    >
                        {t(g.labelKey)}
                    </Link>

                    {/* Category subitems → gender + category */}
                    <div className="space-y-1.5">
                        {categories.map(cat => (
                            <Link
                                key={cat.id}
                                to={`/products?gender=${g.key}&categoryId=${cat.id}`}
                                onClick={onNavigate}
                                className="block text-sm text-gray-500 hover:text-black transition-colors"
                            >
                                {cat.name}
                            </Link>
                        ))}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default ProductsMegaMenu;
```

### Wire it into the Navbar (hover to open)

```jsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import ProductsMegaMenu from './ProductsMegaMenu';

// inside Navbar:
const [productsOpen, setProductsOpen] = useState(false);

<div
    className="relative"
    onMouseEnter={() => setProductsOpen(true)}
    onMouseLeave={() => setProductsOpen(false)}
>
    <Link to="/products" className="text-sm font-semibold uppercase tracking-wide hover:text-gray-600">
        {t('nav.products')}
    </Link>
    {productsOpen && (
        <ProductsMegaMenu
            categories={categories}
            onNavigate={() => setProductsOpen(false)}
        />
    )}
</div>
```

- "Products" itself still links to `/products` (all items).
- Hovering reveals the panel; clicking any link closes it (`onNavigate`).
- The wrapping `relative` lets the absolute panel anchor under the trigger.

---

## 3. Mobile — accordion

Hover menus don't work on touch. In the mobile menu drawer, render an expandable structure:

```jsx
const [openGender, setOpenGender] = useState(null); // 'MEN' | 'WOMEN' | null

// inside the mobile menu:
<div>
    <p className="text-xs font-black uppercase tracking-wide text-black mb-2">{t('nav.products')}</p>

    {GENDERS.map(g => (
        <div key={g.key} className="mb-2">
            <button
                onClick={() => setOpenGender(prev => prev === g.key ? null : g.key)}
                className="w-full flex items-center justify-between py-2 text-sm font-semibold text-black"
            >
                {t(g.labelKey)}
                <span>{openGender === g.key ? '−' : '+'}</span>
            </button>

            {openGender === g.key && (
                <div className="ml-3 border-l border-gray-200 pl-3 space-y-1">
                    <Link
                        to={`/products?gender=${g.key}`}
                        onClick={closeMobileMenu}
                        className="block text-sm text-gray-500 py-1"
                    >
                        {t('nav.allCategory')}
                    </Link>
                    {categories.map(cat => (
                        <Link
                            key={cat.id}
                            to={`/products?gender=${g.key}&categoryId=${cat.id}`}
                            onClick={closeMobileMenu}
                            className="block text-sm text-gray-500 py-1"
                        >
                            {cat.name}
                        </Link>
                    ))}
                </div>
            )}
        </div>
    ))}
</div>
```

(`closeMobileMenu` = whatever closes your mobile drawer.)

---

## 4. ProductsPage — handle BOTH params together

The page must read `gender` AND `categoryId` from the URL and apply both. The gender part comes from `feature-product-gender.md`; ensure category is read the same way.

```jsx
const [searchParams] = useSearchParams();
const [selectedGender, setSelectedGender] = useState(searchParams.get('gender') || '');
const [selectedCategory, setSelectedCategory] = useState(searchParams.get('categoryId') || '');

// keep both in sync when the URL changes (clicking different mega-menu links)
useEffect(() => {
    setSelectedGender(searchParams.get('gender') || '');
    setSelectedCategory(searchParams.get('categoryId') || '');
    setPage(0);
}, [searchParams]);

// include both in the fetch params + the fetch effect's dependency array
const params = {
    gender: selectedGender || undefined,
    categoryId: selectedCategory || undefined,
    // ...search, brand, color, material, price, page, size
};
```

> **Critical:** add BOTH `selectedGender` and `selectedCategory` to the fetch `useEffect`/`useCallback` dependency array — the recurring stale-closure bug in this codebase. Clicking "Men's → Polo" then "Women's → T-Shirts" must re-fetch with the new combo.

---

## 5. Contextual heading + breadcrumb (nice touch)

Show what the user is browsing:

```jsx
const genderLabel = selectedGender
    ? t(`product.gender${selectedGender.charAt(0) + selectedGender.slice(1).toLowerCase()}`)
    : null;
const categoryName = categories.find(c => String(c.id) === String(selectedCategory))?.name;

<h1 className="text-2xl font-black uppercase tracking-tight">
    {genderLabel && categoryName ? `${genderLabel} · ${categoryName}`
        : genderLabel || categoryName || t('nav.products')}
</h1>
```

e.g. "Men's · Polo Shirts", or just "Women's", or "Products" when nothing is selected.

---

## 6. Optional — control which categories appear in the nav

If you don't want every category in the mega-menu (only T-Shirts + Polo), two options:

**A. Client-side filter** by a known set (quick, no backend change):
```jsx
const NAV_CATEGORY_NAMES = ['T-Shirts', 'Polo Shirts'];
const navCategories = categories.filter(c => NAV_CATEGORY_NAMES.includes(c.name));
```

**B. `showInNav` flag on category** (cleaner, admin-controlled):
```sql
ALTER TABLE category ADD COLUMN show_in_nav BOOLEAN DEFAULT TRUE;
```
Add to Category entity + Request/Response, a checkbox in AdminCategories, and a `findByShowInNavTrue()` endpoint the navbar uses. Recommended if the category list will grow.

---

## 7. i18n keys (en.json / sr.json)

```jsonc
// en.json — "nav"
"products": "Products",
"men": "Men's",
"women": "Women's",
"allCategory": "All"

// sr.json — "nav"
"products": "Proizvodi",
"men": "Muškarci",
"women": "Žene",
"allCategory": "Sve"
```

(Gender labels `product.genderMen/Women/Unisex` come from the gender spec.)

---

## 8. Edge cases & notes

- **Shared categories:** the same category list shows under both genders; the products are narrowed by the `gender` param. No duplicate categories.
- **UNISEX products:** per the gender spec, MEN and WOMEN browsing also include UNISEX items (filter predicate `gender = :selected OR gender = 'UNISEX'`), so a unisex polo appears under both Men's and Women's Polo.
- **"Products" still works** as an all-items link (no gender/category) for users who want to see everything.
- **URL is the source of truth** — mega-menu links, mobile accordion, in-page filters, and shareable URLs all agree because everything reads from `searchParams`.
- **Dependency arrays:** both `selectedGender` and `selectedCategory` must be in the fetch deps.
- **Components outside render:** `ProductsMegaMenu` defined as its own component (project rule).
- **Subcategories:** if your categories have their own children (e.g. T-Shirts → Graphic / Plain), you can nest a third level in the mega-menu, but keep it shallow for usability — two levels (gender → category) is the sweet spot.

---

## 9. Checklist

- [ ] Gender field implemented (prerequisite — `feature-product-gender.md`)
- [ ] Navbar fetches categories for the menu
- [ ] `ProductsMegaMenu` component (desktop): Men's/Women's columns, category links combine `gender` + `categoryId`
- [ ] Hover-to-open wired in Navbar; "Products" still links to `/products`
- [ ] Mobile accordion: gender → category links
- [ ] ProductsPage reads BOTH `gender` + `categoryId` from URL, syncs state, includes both in fetch + deps
- [ ] Contextual heading ("Men's · Polo Shirts")
- [ ] Optional: limit nav categories (client filter or `showInNav` flag)
- [ ] i18n keys (EN + SR)
- [ ] Verified: Men's→Polo and Women's→T-Shirts filter correctly; UNISEX shows under both; URL sharable; mobile works
