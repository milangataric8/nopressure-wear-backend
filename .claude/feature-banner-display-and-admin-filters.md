# Feature: Banner "Display Title" toggle + Filters on Banners, Popups, Notifications

NoPressure Wear — implementation instructions.
Stack: Spring Boot 3.4.5 / Java 21 backend, React + Vite + Tailwind frontend, PostgreSQL + Flyway, react-i18next (EN/SR).

> Assumptions (adjust if needed):
> - \*\*AdminBanners\*\* filters by: \*search by title\* \*\*and\*\* \*Active/Inactive\*.
> - \*\*AdminPopups\*\* filters by: \*Active/Inactive\* \*\*and\*\* \*search by title\*.
> - \*\*AdminNotifications\*\* (history) filters by: \*search by subject\* \*\*and\*\* \*channel (EMAIL/WHATSAPP/VIBER)\*.
> Reuse the existing `AdminSearchFilter` component where it fits.

\---

## 1\. Banner — "Display" checkbox for Header/Title

A boolean that controls whether the banner's title/header is rendered on the storefront. Editable via checkbox in admin, displayed in the AdminBanners.jsx and usable as a filter.

### 1.1 Flyway migration

```sql
ALTER TABLE banner ADD COLUMN display\_title BOOLEAN DEFAULT TRUE;
```

### 1.2 Banner.java (entity)

```java
@Column(name = "display\_title")
private Boolean displayTitle = true;
```

### 1.3 BannerRequest.java / BannerResponse.java

```java
private Boolean displayTitle;
```

### 1.4 BannerService

On create/update:

```java
banner.setDisplayTitle(request.getDisplayTitle() != null ? request.getDisplayTitle() : true);
```

In `toResponse`:

```java
.displayTitle(banner.getDisplayTitle())
```

### 1.5 AdminBanners.jsx — checkbox in the form

```jsx
<label className="flex items-center gap-2 cursor-pointer">
    <input
        type="checkbox"
        checked={formData.displayTitle}
        onChange={(e) => setFormData(prev => ({ ...prev, displayTitle: e.target.checked }))}
        className="w-3.5 h-3.5"
    />
    <span className="text-xs text-gray-500 uppercase tracking-wide">
        {t('admin.displayTitle') || 'Display'}
    </span>
</label>
```

Add `displayTitle: true` to:

* `formData` initial state
* `resetForm`
* `handleEdit` → `displayTitle: banner.displayTitle ?? true`

### 1.6 HeroBanner.jsx (storefront) — respect the flag

```jsx
{banner.displayTitle \&\& (
    <h2 className="...">{banner.title}</h2>
)}
```

\---

## 2\. Filter on AdminBanners

Filter by **Display Title (on/off)** and **Active/Inactive**.

### 2.1 Frontend state (AdminBanners.jsx)

```jsx
const \[activeFilter, setActiveFilter] = useState(null);       // true | false | null
const \[displayTitleFilter, setDisplayTitleFilter] = useState(null); // true | false | null
```

### 2.2 Filter UI

```jsx
<div className="flex gap-2 mb-6 flex-wrap items-center">
    {/\* Active toggle \*/}
    <button
        onClick={() => setActiveFilter(prev => prev === true ? null : true)}
        className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-wide border transition-colors ${
            activeFilter === true ? 'bg-black text-white border-black' : 'border-gray-300 text-gray-500 hover:border-black'
        }`}
    >
        {t('admin.active')}
    </button>
    <button
        onClick={() => setActiveFilter(prev => prev === false ? null : false)}
        className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-wide border transition-colors ${
            activeFilter === false ? 'bg-black text-white border-black' : 'border-gray-300 text-gray-500 hover:border-black'
        }`}
    >
        {t('admin.inactive')}
    </button>

    <span className="w-px bg-gray-200 self-stretch mx-1" />

    {/\* Display title toggle \*/}
    <button
        onClick={() => setDisplayTitleFilter(prev => prev === true ? null : true)}
        className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-wide border transition-colors ${
            displayTitleFilter === true ? 'bg-black text-white border-black' : 'border-gray-300 text-gray-500 hover:border-black'
        }`}
    >
        {t('admin.titleShown') || 'Title shown'}
    </button>
    <button
        onClick={() => setDisplayTitleFilter(prev => prev === false ? null : false)}
        className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-wide border transition-colors ${
            displayTitleFilter === false ? 'bg-black text-white border-black' : 'border-gray-300 text-gray-500 hover:border-black'
        }`}
    >
        {t('admin.titleHidden') || 'Title hidden'}
    </button>
</div>
```

### 2.3 Applying the filter

Simplest: filter client-side if the banner list is small (banners usually are):

```jsx
const filteredBanners = banners.filter(b => {
    if (activeFilter !== null \&\& b.active !== activeFilter) return false;
    if (displayTitleFilter !== null \&\& (b.displayTitle ?? true) !== displayTitleFilter) return false;
    return true;
});
```

Render `filteredBanners` instead of `banners`.

> If the banner list is large, move filtering to the backend with a `findByFilters` native query (pattern: `WHERE (:active IS NULL OR is\_active = :active) AND (:displayTitle IS NULL OR display\_title = :displayTitle)`).

\---

## 3\. Filter on AdminPopups

Filter by **Active/Inactive** + **search by title**. Reuse `AdminSearchFilter`.

### 3.1 Frontend state

```jsx
const \[searchInput, setSearchInput] = useState('');
const \[searchQuery, setSearchQuery] = useState('');
const \[activeFilter, setActiveFilter] = useState(null);
```

### 3.2 Use the shared component

```jsx
<AdminSearchFilter
    searchInput={searchInput}
    setSearchInput={setSearchInput}
    searchQuery={searchQuery}
    setSearchQuery={setSearchQuery}
    activeFilter={activeFilter}
    setActiveFilter={setActiveFilter}
    setPage={setPage}            // omit if popups aren't paginated
    searchPlaceholder={t('admin.searchPopups')}
/>
```

### 3.3 Filtering

Client-side (if small list):

```jsx
const filteredPopups = popups.filter(p => {
    if (activeFilter !== null \&\& p.active !== activeFilter) return false;
    if (searchQuery \&\& !p.title?.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    return true;
});
```

> Note: popup `isActive`/`active` field — confirm the popup entity exposes an `active` boolean in its response (Jackson maps `isActive` → `active`). If popups don't have an active flag yet, add one or filter only by search.

\---

## 4\. Filter on AdminNotifications (broadcast history)

Filter history by **search (subject/message)** + **channel**.

### 4.1 Frontend state

```jsx
const \[historySearch, setHistorySearch] = useState('');
const \[channelFilter, setChannelFilter] = useState('');   // '' | 'EMAIL' | 'WHATSAPP' | 'VIBER'
```

### 4.2 Filter UI (above the history list)

```jsx
<div className="flex gap-2 mb-4 flex-wrap items-center">
    <input
        type="text"
        value={historySearch}
        onChange={(e) => setHistorySearch(e.target.value)}
        placeholder={t('admin.searchBroadcasts') || 'Search broadcasts...'}
        className="flex-1 min-w-\[200px] border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black"
    />
    {\['EMAIL', 'WHATSAPP', 'VIBER'].map(ch => (
        <button
            key={ch}
            onClick={() => setChannelFilter(prev => prev === ch ? '' : ch)}
            className={`px-4 py-1.5 text-xs font-semibold uppercase tracking-wide border transition-colors ${
                channelFilter === ch ? 'bg-black text-white border-black' : 'border-gray-300 text-gray-500 hover:border-black'
            }`}
        >
            {ch}
        </button>
    ))}
</div>
```

### 4.3 Filtering

`broadcast.message` is now rich HTML — strip tags before matching so the search works on visible text:

```jsx
const stripHtml = (html) => (html || '').replace(/<\[^>]\*>/g, ' ');

const filteredHistory = history.filter(b => {
    if (channelFilter \&\& !b.channels?.includes(channelFilter)) return false;
    if (historySearch) {
        const q = historySearch.toLowerCase();
        const subjectMatch = (b.subject || '').toLowerCase().includes(q);
        const messageMatch = stripHtml(b.message).toLowerCase().includes(q);
        if (!subjectMatch \&\& !messageMatch) return false;
    }
    return true;
});
```

Render `filteredHistory` instead of `history`.

\---

## 5\. i18n keys to add (en.json / sr.json)

```jsonc
// en.json
"admin": {
    "displayTitle": "Display",
    "titleShown": "Title shown",
    "titleHidden": "Title hidden",
    "searchPopups": "Search popups...",
    "searchBroadcasts": "Search broadcasts..."
}

// sr.json
"admin": {
    "displayTitle": "Prikaži",
    "titleShown": "Naslov prikazan",
    "titleHidden": "Naslov sakriven",
    "searchPopups": "Pretraži popape...",
    "searchBroadcasts": "Pretraži obaveštenja..."
}
```

\---

## 6\. Checklist

* \[ ] Flyway: `banner.display\_title` column added
* \[ ] Banner entity + Request/Response DTOs updated
* \[ ] BannerService sets and returns `displayTitle`
* \[ ] AdminBanners form has Display checkbox (formData, resetForm, handleEdit)
* \[ ] HeroBanner storefront respects `displayTitle`
* \[ ] AdminBanners filter (active + display title)
* \[ ] AdminPopups filter (active + search) — confirm popup has `active` field
* \[ ] AdminNotifications history filter (search + channel), HTML-stripped search
* \[ ] i18n keys added to en.json and sr.json

