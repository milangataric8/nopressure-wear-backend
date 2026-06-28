# Feature: Response Caching on Catalog Endpoints (Spring Cache + Caffeine)

NoPressure Wear — cache frequently-read, rarely-changed endpoints in memory (Caffeine) to cut database load and speed up the storefront. Targets: active products, active categories, settings map, available filters.

Stack: Spring Boot 3.4.5 / Java 21, Spring Cache, Caffeine. Package root: `rs.nopressurewear`.

> **What to cache:** read-heavy + change-rarely data. Good candidates: `/api/categories/active`, `/api/settings/map`, `/api/products/filters` (available filter facets). **Be careful** caching `/api/products/active` directly — it's paginated and filtered, so cache keys must include all params (see §4). The biggest, safest wins are categories + settings + filter facets.

---

## 1. Dependency (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## 2. Enable + configure caches

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "categories", "settings", "productFilters", "featuredProducts");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))   // safety TTL
                .maximumSize(500));
        return manager;
    }
}
```

`expireAfterWrite` is a backstop — even if you forget to evict, the cache self-refreshes after 10 min. Combine with explicit eviction on writes (§3) for immediate consistency.

## 3. Annotate service methods

### Cacheable reads
```java
@Cacheable("categories")
public List<CategoryResponse> getActiveCategories() { ... }

@Cacheable("settings")
public Map<String, String> getSettingsMap() { ... }

@Cacheable("productFilters")
public Map<String, Object> getAvailableFilters() { ... }

@Cacheable("featuredProducts")
public List<ProductResponse> getMostSold(int limit) { ... }  // key = limit
```

### Evict on writes (immediate consistency)
When the underlying data changes, clear the relevant cache so users see fresh data without waiting for the TTL.

```java
// CategoryService — any create/update/delete/toggle
@CacheEvict(value = "categories", allEntries = true)
public CategoryResponse create(...) { ... }
@CacheEvict(value = "categories", allEntries = true)
public CategoryResponse update(...) { ... }
@CacheEvict(value = "categories", allEntries = true)
public void delete(...) { ... }

// Settings — on any setting update
@CacheEvict(value = "settings", allEntries = true)
public void updateSetting(...) { ... }

// Products — changing products affects filters + featured
@CacheEvict(value = {"productFilters", "featuredProducts"}, allEntries = true)
public ProductResponse create(...) { ... }
@CacheEvict(value = {"productFilters", "featuredProducts"}, allEntries = true)
public ProductResponse update(...) { ... }
@CacheEvict(value = {"productFilters", "featuredProducts"}, allEntries = true)
public void delete(...) { ... }
```

> Match every write path. If you have `toggleActive`, bulk imports, stock changes, etc., add eviction there too — a stale categories/filters cache after an edit is the classic caching bug.

## 4. Caching the paginated products list (careful)

`/api/products/active` and the filtered search are paginated + multi-param. If you cache them, the **key must include every parameter** (page, size, sort, categoryId, search, brand, color, material, gender, price) or you'll serve the wrong page/filter. Spring's default key won't do this safely for `Pageable`.

Two safe approaches:
- **Don't cache the paginated list** (recommended to start). Cache the cheaper, high-hit, low-cardinality things (categories, settings, filters, featured). The paginated query is already indexed and fast.
- **If you must:** use a custom `keyGenerator` or an explicit `@Cacheable(key = "...")` string concatenating all params, and accept that high-cardinality filters create many cache entries with low reuse (limited benefit). Often not worth it.

> Rule of thumb: cache the small, shared, slow-changing responses; leave the big, per-user, highly-variable ones to the DB.

## 5. Settings cache + the 'settings-updated' flow

Your frontend dispatches `settings-updated` and refetches after an admin change. With `@CacheEvict` on `updateSetting`, the backend serves fresh settings immediately on that refetch — so the cache and the UI stay consistent. No frontend change needed; just ensure every settings write path evicts.

## 6. Verify

- Hit `/api/categories/active` twice; confirm the second is served from cache (add a log in the service method — it should print once, not twice, within the TTL).
- Edit a category in admin → immediately re-fetch → confirm the change appears (eviction worked).
- Let the TTL pass → confirm the service method runs again (self-refresh).
- Load-test the storefront before/after if you want to quantify DB load reduction.

## 7. Notes

- **Single-instance only:** Caffeine is in-process. If you scale to multiple backend instances, each has its own cache — fine for short TTLs, but for shared invalidation across instances you'd move to Redis (`spring-boot-starter-data-redis` + Redis cache manager). Start with Caffeine; revisit only if you scale out.
- **Don't cache user-specific data:** never cache cart, orders, or anything per-user under a shared key — you'd leak one user's data to another. Only cache public, shared responses.
- **TTL vs eviction:** eviction gives immediate freshness on writes; TTL is the safety net for anything you forgot to evict. Use both.
- **Memory:** `maximumSize` caps entries; Caffeine evicts least-recently-used beyond it. 500 is generous for these few caches.

## 8. Checklist

- [ ] `spring-boot-starter-cache` + Caffeine dependencies
- [ ] `@EnableCaching` + `CacheConfig` (named caches, TTL, max size)
- [ ] `@Cacheable` on categories / settings / filters / featured reads
- [ ] `@CacheEvict` on every matching write path (category, settings, product CRUD/toggle)
- [ ] Decide on paginated products list (recommended: don't cache)
- [ ] Verified: second read cached; edit invalidates immediately; TTL self-refreshes
- [ ] No per-user data cached under shared keys
