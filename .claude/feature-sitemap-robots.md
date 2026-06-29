# Feature: sitemap.xml + robots.txt

NoPressure Wear — add `robots.txt` (tells crawlers what to index) and `sitemap.xml` (lists all indexable URLs so crawlers find every product/category page). Both help search engines discover and rank pages.

Stack: React + Vite (frontend), Spring Boot 3.4.5 / Java 21 (backend, for the dynamic sitemap). Package root: `rs.nopressurewear`.

> **Two parts:** `robots.txt` is always a static file. `sitemap.xml` can be **static** (manual, simple, but won't include individual products) or **dynamic** (generated from the catalog, auto-includes new products — recommended). This spec covers both sitemap approaches; pick one.

---

## PART A — robots.txt (static)

Vite serves anything in `public/` at the site root, so `public/robots.txt` → `https://yourdomain.com/robots.txt`.

Create `public/robots.txt`:

```
User-agent: *
Allow: /

# Private / transactional routes — don't index
Disallow: /admin
Disallow: /cart
Disallow: /checkout
Disallow: /login
Disallow: /register
Disallow: /reset-password
Disallow: /verify-email
Disallow: /orders

Sitemap: https://yourdomain.com/sitemap.xml
```

Notes:
- `User-agent: *` → applies to all crawlers.
- `Allow: /` → everything is crawlable by default…
- `Disallow:` lines → …except these private/admin/transactional routes (no SEO value, shouldn't be indexed).
- The `Sitemap:` line tells crawlers where the sitemap is — use your **real production domain**.

> Replace `yourdomain.com` with your actual domain. Keep this list in sync with your private routes.

---

## PART B — sitemap.xml

### Option 1 — Static file (simple, stopgap)

`public/sitemap.xml` with your stable routes. Downside: it won't list individual products (you'd have to edit it every time you add a product), so it's only a starting point.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://yourdomain.com/</loc>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>https://yourdomain.com/products</loc>
    <changefreq>daily</changefreq>
    <priority>0.9</priority>
  </url>
  <url>
    <loc>https://yourdomain.com/contact</loc>
    <changefreq>monthly</changefreq>
    <priority>0.5</priority>
  </url>
  <url>
    <loc>https://yourdomain.com/privacy-policy</loc>
    <changefreq>yearly</changefreq>
    <priority>0.3</priority>
  </url>
  <url>
    <loc>https://yourdomain.com/terms</loc>
    <changefreq>yearly</changefreq>
    <priority>0.3</priority>
  </url>
  <url>
    <loc>https://yourdomain.com/returns</loc>
    <changefreq>yearly</changefreq>
    <priority>0.3</priority>
  </url>
</urlset>
```

`changefreq` and `priority` are hints (optional); the essential element is `<loc>`.

### Option 2 — Dynamic from the backend (recommended)

Generates the sitemap from the live catalog, so new products/categories are included automatically with zero maintenance.

```java
package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.nopressurewear.repository.CategoryRepository;
import rs.nopressurewear.repository.ProductRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.frontend-url}")
    private String siteUrl;   // e.g. https://yourdomain.com (NOT the backend/api domain)

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        // Static pages
        List.of("", "/products", "/contact", "/privacy-policy", "/terms", "/returns", "/imprint")
            .forEach(path -> appendUrl(sb, siteUrl + path));

        // Active products
        productRepository.findByIsActiveTrue().forEach(p ->
            appendUrl(sb, siteUrl + "/products/" + p.getId()));

        // Active categories (as filtered product views)
        categoryRepository.findByIsActiveTrue().forEach(c ->
            appendUrl(sb, siteUrl + "/products?categoryId=" + c.getId()));

        sb.append("</urlset>");
        return sb.toString();
    }

    private void appendUrl(StringBuilder sb, String loc) {
        sb.append("<url><loc>").append(escapeXml(loc)).append("</loc></url>");
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
```

Key points:
- `produces = APPLICATION_XML_VALUE` → correct `Content-Type: application/xml`.
- `siteUrl` comes from `app.frontend-url` → the `<loc>` URLs point at the **frontend** domain (where the pages actually live), even though the backend generates the file.
- `escapeXml` matters because category URLs contain `&` (e.g. `?gender=MEN&categoryId=3`) — unescaped `&` is invalid XML and breaks the sitemap.
- Uses your existing `findByIsActiveTrue()` repository methods (add a simple one to CategoryRepository if missing).

Make it public in the `@Order(1)` API security chain:
```java
.requestMatchers("/sitemap.xml").permitAll()
```

> **Domain note:** if your backend runs on `api.yourdomain.com` and frontend on `yourdomain.com`, the sitemap is reachable at `api.yourdomain.com/sitemap.xml`. Google can crawl it there, but the `Sitemap:` line in `robots.txt` (which lives on the frontend domain) should point to wherever the sitemap actually is. Cleanest: serve it from the frontend domain. Options:
> - **Reverse proxy:** route `yourdomain.com/sitemap.xml` → backend (if your host/proxy allows).
> - **Build step / serverless function** on the frontend that fetches the backend data and serves the XML at the frontend root.
> - Or simply point `robots.txt` `Sitemap:` at the backend URL and submit the backend URL in Search Console — works, just less tidy.

### Option 3 — Generated at build (middle ground)

A Vite build script that fetches products from the API and writes `dist/sitemap.xml` at build time. Auto-includes products as of the build, served as a static file on the frontend domain, no runtime backend dependency. Good if you redeploy often; stale between deploys. (Dynamic Option 2 is fresher; static Option 1 is simplest.)

---

## PART C — Submit & verify

1. **Deploy** so `robots.txt` and `sitemap.xml` are reachable at the production domain.
2. Visit `https://yourdomain.com/robots.txt` → confirm it loads and the `Sitemap:` line is correct.
3. Visit the sitemap URL → confirm valid XML, real product URLs, no unescaped `&`.
4. **Google Search Console:** verify domain ownership → Sitemaps → submit the sitemap URL. Google reports discovered/indexed pages and any errors.
5. (Optional) Validate the sitemap with an online sitemap validator.

---

## D. Notes & edge cases

- **XML escaping:** category/filter URLs with `&` MUST be escaped (`&amp;`) or the sitemap is invalid. The dynamic generator handles it; if you hand-write static XML with query params, escape manually.
- **Only public pages:** never list `/admin`, `/cart`, `/checkout`, auth, or `/orders` in the sitemap (and Disallow them in robots).
- **Active only:** the dynamic sitemap lists only active products/categories — inactive/soft-deleted ones shouldn't be indexed.
- **Keep robots + sitemap consistent:** routes you Disallow in robots shouldn't appear in the sitemap.
- **Domain consistency:** `<loc>` URLs and the `robots.txt` Sitemap line must use the real public frontend domain (https).
- **Big catalogs:** if you ever exceed ~50,000 URLs (not your case now), sitemaps must be split with a sitemap index — irrelevant at current scale, noted for completeness.
- **Resubmit:** the dynamic sitemap updates itself; Google re-crawls periodically. No need to resubmit per product.

---

## E. Checklist

- [ ] `public/robots.txt` with Allow + Disallow (private routes) + Sitemap line (real domain)
- [ ] sitemap approach chosen: static (Option 1), dynamic backend (Option 2, recommended), or build-time (Option 3)
- [ ] (Dynamic) `SitemapController` → `/sitemap.xml`, XML content-type, `app.frontend-url` for loc, `&` escaped, active products + categories + static pages
- [ ] (Dynamic) `/sitemap.xml` permitAll; reachable on the right domain
- [ ] robots + sitemap consistent (no private routes in sitemap)
- [ ] Deployed + URLs reachable in production
- [ ] Submitted to Google Search Console
- [ ] Verified: valid XML, real product URLs, escaped ampersands, robots loads
