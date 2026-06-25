# Feature: Admin-Configurable Display Duration (seconds) for Banners & Popups

NoPressure Wear — let the admin set, in seconds, how long banners and popups display.
Stack: Spring Boot 3.4.5 / Java 21, PostgreSQL + Flyway, React + Vite + Tailwind, react-i18next (EN/SR).

> **What "duration" means per type (assumption — adjust if different):**
> - **Banner:** the carousel auto-rotate interval — how many seconds each slide shows before advancing to the next. `0` (or empty) = no auto-rotate (manual only).
> - **Popup:** how many seconds the popup stays open before it auto-closes. `0` (or empty) = stays until the user closes it manually.
>
> Stored as **seconds** in the UI/DB; converted to **milliseconds** in the frontend timers.

---

## PART A — Banners

### A.1 Flyway migration

```sql
ALTER TABLE banner ADD COLUMN display_duration INTEGER DEFAULT 5;
```

`display_duration` is in **seconds**. Default 5s. `0` disables auto-rotate.

### A.2 Banner.java (entity)

```java
@Column(name = "display_duration")
private Integer displayDuration = 5;
```

### A.3 BannerRequest.java / BannerResponse.java

```java
private Integer displayDuration;
```

Optional validation on the request DTO (clamp to a sane range):

```java
@Min(value = 0, message = "Duration cannot be negative")
@Max(value = 60, message = "Duration cannot exceed 60 seconds")
private Integer displayDuration;
```

### A.4 BannerService

On create/update:

```java
banner.setDisplayDuration(
    request.getDisplayDuration() != null ? request.getDisplayDuration() : 5
);
```

In `toResponse`:

```java
.displayDuration(banner.getDisplayDuration())
```

### A.5 AdminBanners.jsx — duration input in the form

```jsx
<div>
    <label className={labelClass}>{t('admin.displayDuration')}</label>
    <div className="flex items-center gap-2">
        <input
            type="number"
            min="0"
            max="60"
            value={formData.displayDuration}
            onChange={(e) => setFormData(prev => ({
                ...prev,
                displayDuration: e.target.value === '' ? '' : parseInt(e.target.value, 10)
            }))}
            className="w-24 border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black"
        />
        <span className="text-xs text-gray-400">{t('admin.seconds')}</span>
    </div>
    <p className="text-xs text-gray-400 mt-1">{t('admin.bannerDurationHint')}</p>
</div>
```

Add `displayDuration: 5` to:
- `formData` initial state
- `resetForm`
- `handleEdit` → `displayDuration: banner.displayDuration ?? 5`

### A.6 HeroBanner.jsx (storefront) — use it as the auto-rotate interval

The interval comes from the **currently shown** banner's `displayDuration` (each slide can have its own timing). Convert seconds → milliseconds; skip rotation when `0`.

```jsx
import { useState, useEffect, useCallback } from 'react';

const HeroBanner = ({ banners }) => {
    const [current, setCurrent] = useState(0);

    const goNext = useCallback(() => {
        setCurrent(prev => (prev + 1) % banners.length);
    }, [banners.length]);

    useEffect(() => {
        if (!banners || banners.length <= 1) return;

        const seconds = banners[current]?.displayDuration ?? 5;
        if (!seconds || seconds <= 0) return;   // 0 = no auto-rotate

        const timer = setTimeout(goNext, seconds * 1000);
        return () => clearTimeout(timer);
    }, [current, banners, goNext]);

    // ... render banners[current], prev/next arrows, dots ...
};
```

Key points:
- `seconds * 1000` converts to ms.
- The effect depends on `current`, so each slide re-arms the timer with its own duration.
- `clearTimeout` cleanup prevents stacked timers when the user manually navigates.
- `0`/null → no timer, slide stays until manual navigation.

---

## PART B — Popups

### B.1 Flyway migration

```sql
ALTER TABLE popup ADD COLUMN display_duration INTEGER DEFAULT 0;
```

`display_duration` in **seconds**. Default `0` = stays open until manually closed.

### B.2 Popup.java (entity)

```java
@Column(name = "display_duration")
private Integer displayDuration = 0;
```

### B.3 PopupRequest.java / PopupResponse.java

```java
private Integer displayDuration;
```

Optional validation:

```java
@Min(value = 0, message = "Duration cannot be negative")
@Max(value = 120, message = "Duration cannot exceed 120 seconds")
private Integer displayDuration;
```

### B.4 PopupService

On create/update:

```java
popup.setDisplayDuration(
    request.getDisplayDuration() != null ? request.getDisplayDuration() : 0
);
```

In `toResponse`:

```java
.displayDuration(popup.getDisplayDuration())
```

### B.5 AdminPopups.jsx — duration input in the form

```jsx
<div>
    <label className={labelClass}>{t('admin.displayDuration')}</label>
    <div className="flex items-center gap-2">
        <input
            type="number"
            min="0"
            max="120"
            value={formData.displayDuration}
            onChange={(e) => setFormData(prev => ({
                ...prev,
                displayDuration: e.target.value === '' ? '' : parseInt(e.target.value, 10)
            }))}
            className="w-24 border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black"
        />
        <span className="text-xs text-gray-400">{t('admin.seconds')}</span>
    </div>
    <p className="text-xs text-gray-400 mt-1">{t('admin.popupDurationHint')}</p>
</div>
```

Add `displayDuration: 0` to `formData` initial state, `resetForm`, and `handleEdit` (`displayDuration: popup.displayDuration ?? 0`).

### B.6 HomePopup.jsx (storefront) — auto-close after the duration

```jsx
import { useState, useEffect } from 'react';

const HomePopup = ({ popup }) => {
    const [visible, setVisible] = useState(true);

    useEffect(() => {
        if (!popup) return;

        const seconds = popup.displayDuration ?? 0;
        if (!seconds || seconds <= 0) return;   // 0 = manual close only

        const timer = setTimeout(() => setVisible(false), seconds * 1000);
        return () => clearTimeout(timer);
    }, [popup]);

    if (!popup || !visible) return null;

    return (
        <div className="popup-overlay">
            <div className="popup-content">
                <button onClick={() => setVisible(false)} className="popup-close">×</button>
                {/* ... popup body ... */}
            </div>
        </div>
    );
};
```

Key points:
- `seconds * 1000` → ms.
- `0`/null → no auto-close timer; the manual × close button always works regardless.
- Cleanup clears the timer if the popup unmounts or the user closes early.

> If your popup has a "show once per session" rule (localStorage/sessionStorage flag), the auto-close should still set that flag the same way the manual close does — extract a single `closePopup()` that both the timeout and the × button call, so dismissal logic stays in one place.

Refactor suggestion:

```jsx
const closePopup = () => {
    setVisible(false);
    // sessionStorage.setItem('popupSeen', 'true');  // if you track this
};

useEffect(() => {
    const seconds = popup?.displayDuration ?? 0;
    if (!seconds || seconds <= 0) return;
    const timer = setTimeout(closePopup, seconds * 1000);
    return () => clearTimeout(timer);
}, [popup]);
```

---

## i18n keys (en.json / sr.json)

```jsonc
// en.json — under "admin"
"displayDuration": "Display Duration",
"seconds": "seconds",
"bannerDurationHint": "How long each slide shows before advancing. 0 = no auto-rotate.",
"popupDurationHint": "How long the popup stays open. 0 = until manually closed."

// sr.json — under "admin"
"displayDuration": "Trajanje prikaza",
"seconds": "sekundi",
"bannerDurationHint": "Koliko dugo se svaki slajd prikazuje pre prelaska na sledeći. 0 = bez automatske rotacije.",
"popupDurationHint": "Koliko dugo popap ostaje otvoren. 0 = dok se ručno ne zatvori."
```

---

## Edge cases & notes

- **Seconds vs milliseconds:** the value is stored and shown in **seconds**; always multiply by 1000 for `setTimeout`/`setInterval`. This is the most common bug — a value of `5` must become `5000`ms.
- **Zero / empty:** treat `0`, `null`, and empty input as "no timer" (banner = manual only, popup = manual close only). The guards `if (!seconds || seconds <= 0) return;` handle all three.
- **Empty input while typing:** the `onChange` keeps `''` so the field can be cleared; coerce to a number (or default) on save in the submit handler: `displayDuration: formData.displayDuration === '' ? (defaultValue) : formData.displayDuration`.
- **Timer cleanup:** always `return () => clearTimeout(timer)` in the effect to avoid stacked/leaking timers, especially for the banner where the effect re-runs on each slide change.
- **Single banner:** skip rotation entirely when `banners.length <= 1`.
- **Existing rows:** the `DEFAULT` in the migration backfills old banners/popups (5s banners, 0s popups), so nothing breaks for pre-existing records.

---

## Checklist

- [ ] Flyway: `banner.display_duration` (default 5) added
- [ ] Flyway: `popup.display_duration` (default 0) added
- [ ] Banner entity + Request/Response DTOs updated
- [ ] Popup entity + Request/Response DTOs updated
- [ ] BannerService / PopupService set + return `displayDuration`
- [ ] AdminBanners form: seconds input (formData, resetForm, handleEdit)
- [ ] AdminPopups form: seconds input (formData, resetForm, handleEdit)
- [ ] HeroBanner uses per-slide duration as auto-rotate interval (sec → ms, 0 = off)
- [ ] HomePopup auto-closes after duration (sec → ms, 0 = manual only), shared closePopup()
- [ ] i18n keys added (EN + SR)
- [ ] Verified: 0 disables timer; non-zero rotates/closes correctly
