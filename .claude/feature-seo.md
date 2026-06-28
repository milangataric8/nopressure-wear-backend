# Feature: SEO — Meta Tags, Open Graph, Sitemap & robots.txt

NoPressure Wear — give each page (especially products) its own title, description, and Open Graph tags so search engines index them properly and shared links show rich previews; plus `sitemap.xml` and `robots.txt` so crawlers discover all pages.

Stack: React + Vite, React Router, Spring Boot backend (for a dynamic sitemap, optional). Package root: `rs.nopressurewear`.

> **Important context:** this is a client-rendered SPA (Vite). Search engines do execute JS, but for best SEO, per-page meta must be set on navigation (React Helmet or React 19 native `<head>`). True per-product prerendering/SSR is a bigger change (Next.js / prerender service) — out of scope here; the meta + sitemap below covers the high-value basics.

---

## PART A — Per-page meta tags + Open Graph

### A.1 Choose the mechanism

- **React 19+:** you can render `<title>`, `<meta>`, `<link>` directly in components and React hoists them to `<head>`. Simplest if you're on React 19.
- **Older React:** use `react-helmet-async`.

```bash
npm install react-helmet-async
```
Wrap the app once:
```jsx
import { HelmetProvider } from 'react-helmet-async';
// <HelmetProvider><App /></HelmetProvider>
```

### A.2 Reusable Seo component

`src/components/seo/Seo.jsx`:
```jsx
import { Helmet } from 'react-helmet-async';   // or use native <head> tags on React 19

const SITE_NAME = 'NoPressure Wear';
const DEFAULT_DESC = 'Premium minimalist clothing. Be relaxed. Live easy.';

const Seo = ({ title, description, image, url, type = 'website' }) => {
    const fullTitle = title ? `${title} · ${SITE_NAME}` : SITE_NAME;
    const desc = description || DEFAULT_DESC;

    return (
        <Helmet>
            <title>{fullTitle}</title>
            <meta name="description" content={desc} />

            {/* Open Graph (Facebook, link previews) */}
            <meta property="og:site_name" content={SITE_NAME} />
            <meta property="og:title" content={fullTitle} />
            <meta property="og:description" content={desc} />
            <meta property="og:type" content={type} />
            {url && <meta property="og:url" content={url} />}
            {image && <meta property="og:image" content={image} />}

            {/* Twitter card */}
            <meta name="twitter:card" content={image ? 'summary_large_image' : 'summary'} />
            <meta name="twitter:title" content={fullTitle} />
            <meta name="twitter:description" content={desc} />
            {image && <meta name="twitter:image" content={image} />}
        </Helmet>
    );
};

export default Seo;
```

### A.3 Use it per page

**Product detail** — the highest-value SEO page:
```jsx
<Seo
    title={product.name}
    description={stripHtml(product.description)?.slice(0, 155) || `${product.name} — ${product.categoryName}`}
    image={product.imageUrl ? getAbsoluteImageUrl(product.imageUrl) : undefined}
    url={`${SITE_URL}/products/${product.id}`}
    type="product"
/>
```
- `stripHtml(...).slice(0,155)` → a clean ~155-char description from the rich-text body.
- `image` must be an **absolute** URL (Open Graph requires it) — build from your CDN/storage public URL, not a relative `/uploads/...` path.

**Category / products listing:**
```jsx
<Seo title={categoryName || t('nav.products')} url={`${SITE_URL}/products`} />
```

**Home, Contact, legal pages:** give each a sensible title/description.

### A.4 Base meta in index.html

Set sensible defaults in `index.html` `<head>` (used before React mounts and as fallback):
```html
<title>NoPressure Wear</title>
<meta name="description" content="Premium minimalist clothing. Be relaxed. Live easy." />
<meta property="og:site_name" content="NoPressure Wear" />
<meta property="og:image" content="https://yourdomain.com/og-default.jpg" />
```
Add a default OG image (`public/og-default.jpg`, ~1200×630) for pages without a specific image.

---

## PART B — robots.txt

Static file at `public/robots.txt` (Vite serves `public/` at the root):

```
User-agent: *
Allow: /

# Don't index admin or auth flows
Disallow: /admin
Disallow: /cart
Disallow: /checkout
Disallow: /login
Disallow: /register
Disallow: /reset-password
Disallow: /verify-email

Sitemap: https://yourdomain.com/sitemap.xml
```

Adjust the domain. Disallow private/admin/transactional routes; allow catalog pages.

---

## PART C — sitemap.xml

A list of your indexable URLs so crawlers find every product/category.

### Option 1 — static (simple, manual)

`public/sitemap.xml` listing your stable routes:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url><loc>https://yourdomain.com/</loc></url>
  <url><loc>https://yourdomain.com/products</loc></url>
  <url><loc>https://yourdomain.com/contact</loc></url>
  <url><loc>https://yourdomain.com/privacy-policy</loc></url>
</urlset>
```
Downside: doesn't include individual products (you'd edit it constantly). Fine as a stopgap.

### Option 2 — dynamic from the backend (recommended for products)

A backend endpoint that generates the sitemap from the catalog:

```java
@RestController
public class SitemapController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.frontend-url}")
    private String siteUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        // static pages
        for (String path : List.of("", "/products", "/contact", "/privacy-policy", "/terms", "/returns")) {
            sb.append("<url><loc>").append(siteUrl).append(path).append("</loc></url>");
        }
        // products
        productRepository.findByIsActiveTrue().forEach(p ->
            sb.append("<url><loc>").append(siteUrl).append("/products/").append(p.getId()).append("</loc></url>"));
        // categories
        categoryRepository.findByIsActiveTrue().forEach(c ->
            sb.append("<url><loc>").append(siteUrl).append("/products?categoryId=").append(c.getId()).append("</loc></url>"));

        sb.append("</urlset>");
        return sb.toString();
    }
}
```
Make `/sitemap.xml` `permitAll`. Point `robots.txt` at it. This auto-includes new products with no maintenance.

> If the backend is on a different domain (api.yourdomain.com) than the frontend, either serve the sitemap from the frontend domain (a small serverless function / build step) or ensure Google can reach the backend URL and the `<loc>`s point at the **frontend** domain (they do above, via `app.frontend-url`).

---

## PART D — Submit & verify

- **Google Search Console:** verify the domain, submit `sitemap.xml`. This is how Google discovers and reports indexing.
- **Test OG previews:** use Facebook Sharing Debugger / Twitter Card Validator / a link preview in any chat app — paste a product URL and confirm the title/description/image show.
- **Confirm meta per route:** navigate between pages, inspect `<head>` — title/description/OG should change per page.
- **robots.txt:** visit `yourdomain.com/robots.txt` → confirm it loads and lists the sitemap.

---

## E. Notes

- **Absolute image URLs for OG:** relative paths don't work for link previews; use the public/CDN URL. This is another reason production file storage (Cloudinary/S3) helps.
- **SPA limitation:** crawlers run JS, but server-rendered meta is more reliable. If SEO becomes a priority, consider a prerender service (prerender.io) or migrating product pages to SSR — bigger effort, note for later.
- **Descriptions ~150–160 chars:** longer gets truncated in search results; strip HTML from rich-text descriptions.
- **Don't index private routes:** keep admin/cart/checkout/auth out of robots + sitemap.
- **Canonical URLs (optional):** add `<link rel="canonical">` per page to avoid duplicate-content issues from query-param variations (e.g. the same products page with different filters).

---

## F. Checklist

- [ ] Meta mechanism chosen (React 19 native or react-helmet-async + provider)
- [ ] `Seo` component (title, description, OG, Twitter)
- [ ] Used on product detail (absolute image URL), listings, home, contact, legal
- [ ] Default meta + default OG image in index.html / public
- [ ] `public/robots.txt` (disallow private routes, link sitemap)
- [ ] `sitemap.xml` — static or dynamic backend endpoint (recommended) listing products + categories + static pages
- [ ] `/sitemap.xml` permitAll if backend-generated; loc points to frontend domain
- [ ] Submitted to Google Search Console
- [ ] OG preview verified via a sharing debugger
- [ ] (Optional) canonical links
