# Funkcionalnost: Share dugme na stranici detalja proizvoda

NoPressure Wear — dodati dugme za deljenje (share) na ProductDetailPage, koje deli link do proizvoda. Na mobilnom otvara nativni share meni (WhatsApp, Viber, Messenger…), a na desktopu prikazuje fallback meni sa "kopiraj link" + direktnim opcijama.

Stack: React + Vite + Tailwind, react-i18next (EN/SR), react-toastify. Package root: `rs.nopressurewear`.

> **Pristup:** **Web Share API** (`navigator.share`) gde je dostupan (mobilni + neki desktop) → nativni share meni telefona. Fallback (desktop bez Web Share) → dropdown sa "Kopiraj link" + WhatsApp/Viber/Facebook.

---

## 1. Komponenta — ShareButton.jsx

Napravi `src/components/product/ShareButton.jsx` (komponenta van druge komponente — pravilo projekta):

```jsx
import { useState } from 'react';
import { toast } from 'react-toastify';
import { useTranslation } from 'react-i18next';

const ShareButton = ({ product }) => {
    const { t } = useTranslation();
    const [open, setOpen] = useState(false);

    const url = window.location.href;
    const title = product.name;
    const text = `${product.name} — NoPressure Wear`;

    const handleShare = async () => {
        if (navigator.share) {                       // mobilni / Web Share dostupan
            try {
                await navigator.share({ title, text, url });
            } catch (e) {
                // korisnik otkazao — ignoriši
            }
        } else {
            setOpen(prev => !prev);                   // desktop fallback dropdown
        }
    };

    const copyLink = async () => {
        try {
            await navigator.clipboard.writeText(url);
            toast.success(t('product.linkCopied'));
            setOpen(false);
        } catch (e) {
            toast.error(t('product.copyFailed'));
        }
    };

    return (
        <div className="relative inline-block">
            <button
                onClick={handleShare}
                title={t('product.share')}
                className="inline-flex items-center justify-center gap-2 border border-gray-300 text-black text-sm font-semibold uppercase tracking-wide px-4 py-2.5 hover:bg-gray-50 transition-colors"
            >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="flex-shrink-0">
                    <circle cx="18" cy="5" r="3" />
                    <circle cx="6" cy="12" r="3" />
                    <circle cx="18" cy="19" r="3" />
                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                </svg>
                {t('product.share')}
            </button>

            {/* Fallback dropdown — desktop bez Web Share */}
            {open && (
                <div className="absolute top-full left-0 mt-2 bg-white border border-gray-200 shadow-sm z-20 w-56">
                    <button
                        onClick={copyLink}
                        className="w-full text-left px-4 py-2.5 text-sm text-black hover:bg-gray-50 border-b border-gray-100"
                    >
                        {t('product.copyLink')}
                    </button>
                    <a
                        href={`https://wa.me/?text=${encodeURIComponent(text + ' ' + url)}`}
                        target="_blank" rel="noopener noreferrer"
                        onClick={() => setOpen(false)}
                        className="block px-4 py-2.5 text-sm text-black hover:bg-gray-50 border-b border-gray-100"
                    >
                        WhatsApp
                    </a>
                    <a
                        href={`viber://forward?text=${encodeURIComponent(text + ' ' + url)}`}
                        onClick={() => setOpen(false)}
                        className="block px-4 py-2.5 text-sm text-black hover:bg-gray-50 border-b border-gray-100"
                    >
                        Viber
                    </a>
                    <a
                        href={`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`}
                        target="_blank" rel="noopener noreferrer"
                        onClick={() => setOpen(false)}
                        className="block px-4 py-2.5 text-sm text-black hover:bg-gray-50"
                    >
                        Facebook
                    </a>
                </div>
            )}
        </div>
    );
};

export default ShareButton;
```

---

## 2. Upotreba u ProductDetailPage.jsx

```jsx
import ShareButton from '../components/product/ShareButton';

{/* npr. pored "dodaj u korpu" ili ispod imena proizvoda */}
<ShareButton product={product} />
```

Ako želiš pored "Find in Store" dugmeta, stavi ih u flex red:
```jsx
<div className="flex gap-2 flex-wrap">
    {/* ...add to cart / find in store... */}
    <ShareButton product={product} />
</div>
```

---

## 3. Prevodi (en.json / sr.json) — pod "product"

```jsonc
// en.json
"share": "Share",
"copyLink": "Copy link",
"linkCopied": "Link copied!",
"copyFailed": "Failed to copy"

// sr.json
"share": "Podeli",
"copyLink": "Kopiraj link",
"linkCopied": "Link kopiran!",
"copyFailed": "Kopiranje nije uspelo"
```

---

## 4. Kako radi

- **Mobilni:** `navigator.share` postoji → klik otvara nativni share meni; korisnik bira aplikaciju (WhatsApp, Viber, Messenger, kopiranje…). Najbolji doživljaj.
- **Desktop:** `navigator.share` često ne postoji → klik otvara dropdown sa "Kopiraj link" (clipboard API + toast) i direktnim WhatsApp/Viber/Facebook linkovima.
- **Link koji se deli:** `window.location.href` (trenutni URL proizvoda).

---

## 5. Lep preview pri deljenju (povezano sa SEO/OG)

Da podeljeni link prikaže **sliku proizvoda + ime + cenu** (a ne samo go URL), stranica proizvoda mora imati **Open Graph meta tagove** — to je deo SEO specifikacije (`feature-seo.md`):

```html
<meta property="og:title" content="..." />
<meta property="og:description" content="..." />
<meta property="og:image" content="apsolutni-url-slike" />
<meta property="og:url" content="..." />
```

Bez OG tagova share radi, ali preview je samo go link. **Preporuka:** implementirati i OG deo iz SEO specifikacije da deljenje izgleda profesionalno. `og:image` mora biti **apsolutni** URL (javni/CDN), ne relativni `/uploads/...` — još jedan razlog za produkcijski file storage (Cloudinary/S3).

---

## 6. Napomene i ivični slučajevi

- **Web Share zahteva HTTPS** (osim na localhost). U produkciji radi tek kad je sajt na HTTPS — što ionako treba imati.
- **Viber link** (`viber://forward`) radi samo ako korisnik ima Viber instaliran; na desktopu bez Viber-a ne radi ništa — zato je tu i "Kopiraj link" kao univerzalna opcija.
- **Zatvaranje dropdown-a:** opciono dodaj klik-van-dropdowna da se zatvori (npr. `useEffect` + `mousedown` listener), radi urednosti.
- **Komponenta van render-a:** `ShareButton` definisan kao zasebna komponenta (pravilo projekta).
- **Otkazivanje share-a:** `navigator.share` baca grešku ako korisnik otkaže — hvata se i ignoriše (nije prava greška).

---

## 7. Checklist

- [ ] `ShareButton.jsx` komponenta napravljena
- [ ] Ubačena u ProductDetailPage (pored add-to-cart / find-in-store)
- [ ] Web Share API + desktop fallback dropdown (kopiraj link, WhatsApp, Viber, Facebook)
- [ ] "Kopiraj link" koristi clipboard API + toast potvrda
- [ ] Prevodi dodati (EN + SR)
- [ ] (Preporuka) Open Graph tagovi iz SEO specifikacije za lep preview
- [ ] Provereno: mobilni otvara nativni meni; desktop fallback radi; kopiranje radi; preview slike kad su OG tagovi tu
