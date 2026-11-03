package rs.nopressurewear.catalog.internal.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.catalog.api.ProductSize;

import java.math.BigDecimal;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class CatalogModelRepository {
    private final EntityManager entityManager;

    // UNION (not UNION ALL) terminates even for reciprocal links and cycles.
    // Inactive products cannot become options or bridges between active models.
    private static final String GROUPS = """
        WITH RECURSIVE active AS (SELECT id FROM product WHERE is_active = true),
        edges AS (
            SELECT cv.product_id AS a, cv.variant_id AS b FROM product_color_variant cv
            JOIN active p ON p.id = cv.product_id JOIN active v ON v.id = cv.variant_id
            UNION SELECT cv.variant_id, cv.product_id FROM product_color_variant cv
            JOIN active p ON p.id = cv.product_id JOIN active v ON v.id = cv.variant_id
        ), reach(id, member) AS (
            SELECT id, id FROM active
            UNION SELECT r.id, e.b FROM reach r JOIN edges e ON e.a = r.member
        ), groups AS (SELECT id, MIN(member) AS model_id FROM reach GROUP BY id)
        """;

    public record Member(Long productId, Long modelId) {}

    public Page<Member> findModels(Long categoryId, String search, BigDecimal minPrice, BigDecimal maxPrice,
                                   String brand, String colorName, String material, String gender,
                                   List<ProductSize> sizes, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE p.is_active = true");
        if (categoryId != null) {
            where.append(" AND (p.category_id = :categoryId OR p.category_id IN (SELECT id FROM category WHERE parent_id = :categoryId))");
            params.put("categoryId", categoryId);
        }
        if (search != null && !search.isBlank()) {
            where.append(" AND (LOWER(p.name) LIKE :search OR LOWER(p.sku) LIKE :search)");
            params.put("search", "%" + search.toLowerCase(Locale.ROOT) + "%");
        }
        if (minPrice != null) { where.append(" AND p.price >= :minPrice"); params.put("minPrice", minPrice); }
        if (maxPrice != null) { where.append(" AND p.price <= :maxPrice"); params.put("maxPrice", maxPrice); }
        addText(where, params, "brand", "brand", brand);
        addText(where, params, "color_name", "colorName", colorName);
        addText(where, params, "material", "material", material);
        addText(where, params, "gender", "gender", gender);
        if (sizes != null && !sizes.isEmpty()) {
            where.append(" AND EXISTS (SELECT 1 FROM product_variant v WHERE v.product_id = p.id AND v.size IN (:sizes) AND v.stock_quantity > 0)");
            params.put("sizes", sizes.stream().map(Enum::name).toList());
        }
        String matched = GROUPS + ", matched AS (SELECT p.*, g.model_id, "
                + "(SELECT COALESCE(SUM(v.stock_quantity), 0) FROM product_variant v WHERE v.product_id = p.id) AS catalog_stock "
                + "FROM product p JOIN groups g ON g.id = p.id" + where + ") ";
        long total = ((Number) query(matched + "SELECT COUNT(DISTINCT model_id) FROM matched", params).getSingleResult()).longValue();
        String order = sortSql(pageable.getSort());
        String sql = matched + ", ranked AS (SELECT *, ROW_NUMBER() OVER (PARTITION BY model_id ORDER BY " + order
                + ") AS option_rank FROM matched), page_models AS (SELECT model_id, ROW_NUMBER() OVER (ORDER BY "
                + order + ") AS model_rank FROM ranked WHERE option_rank = 1 ORDER BY " + order + " LIMIT :limit OFFSET :offset) "
                + "SELECT m.id, m.model_id FROM ranked m JOIN page_models pg ON pg.model_id = m.model_id ORDER BY pg.model_rank, m.option_rank";
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());
        @SuppressWarnings("unchecked") List<Object[]> rows = query(sql, params).getResultList();
        List<Member> members = rows.stream().map(r -> new Member(((Number) r[0]).longValue(), ((Number) r[1]).longValue())).toList();
        // A page contains models, not individual option rows. Service assembles the model DTOs.
        return new PageImpl<>(members, Pageable.unpaged(), total);
    }

    private Query query(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }

    private static void addText(StringBuilder sql, Map<String, Object> params, String column, String key, String value) {
        if (value == null || value.isBlank()) return;
        sql.append(" AND LOWER(p.").append(column).append(") = LOWER(:").append(key).append(")");
        params.put(key, value);
    }

    private static String sortSql(Sort sort) {
        List<String> columns = new ArrayList<>();
        for (Sort.Order order : sort) {
            String column = switch (order.getProperty()) {
                case "price" -> "price";
                case "createdAt" -> "created_at";
                case "stockQuantity", "totalStock" -> "catalog_stock";
                default -> "name";
            };
            columns.add(column + (order.isAscending() ? " ASC" : " DESC") + " NULLS LAST");
        }
        columns.add("id ASC");
        return String.join(", ", columns);
    }
}
