package rs.nopressurewear.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    java.util.Optional<Product> findByIdForUpdate(@Param("id") Long id);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByIsActiveTrueOrderByIdAsc();

    List<Product> findByCategoryId(Long categoryId);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByIsActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Product> findByIsActiveTrueAndCategoryIdIn(List<Long> categoryIds, Pageable pageable);

    List<Product> findBySkuContainingAndIdNot(String sku, Long id);

    @Query(
    value = """
        SELECT * FROM product p
        WHERE (:active IS NULL OR p.is_active = :active)
          AND (:categoryId IS NULL OR p.category_id = :categoryId
               OR p.category_id IN (
                   SELECT id FROM category WHERE parent_id = :categoryId
               ))
          AND (:search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
          AND (:colorName IS NULL OR LOWER(p.color_name) = LOWER(:colorName))
          AND (:material IS NULL OR LOWER(p.material) = LOWER(:material))
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:sizeCount = 0 OR EXISTS (
                   SELECT 1 FROM product_variant v
                   WHERE v.product_id = p.id
                     AND v.size IN (:sizes)
                     AND v.stock_quantity > 0
               ))
        ORDER BY p.name ASC
        """,
    countQuery = """
        SELECT COUNT(*) FROM product p
        WHERE (:active IS NULL OR p.is_active = :active)
          AND (:categoryId IS NULL OR p.category_id = :categoryId
               OR p.category_id IN (
                   SELECT id FROM category WHERE parent_id = :categoryId
               ))
          AND (:search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
          AND (:colorName IS NULL OR LOWER(p.color_name) = LOWER(:colorName))
          AND (:material IS NULL OR LOWER(p.material) = LOWER(:material))
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:sizeCount = 0 OR EXISTS (
                   SELECT 1 FROM product_variant v
                   WHERE v.product_id = p.id
                     AND v.size IN (:sizes)
                     AND v.stock_quantity > 0
               ))
        """,
    nativeQuery = true)
    Page<Product> findByFilters(
        @Param("categoryId") Long categoryId,
        @Param("search") String search,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("brand") String brand,
        @Param("colorName") String colorName,
        @Param("material") String material,
        @Param("active") Boolean active,
        @Param("gender") String gender,
        @Param("sizes") List<String> sizes,
        @Param("sizeCount") int sizeCount,
        Pageable pageable);

    @Query(
    value = """
        SELECT DISTINCT p.brand
         FROM product p
        WHERE p.brand IS NOT NULL
          AND p.is_active = true
        ORDER BY p.brand
        """,
    nativeQuery = true)
    List<String> findDistinctBrands();

    @Query(
    value = """
        SELECT DISTINCT p.color_name, p.color_hex
         FROM product p
        WHERE p.color_name IS NOT NULL
          AND p.color_hex IS NOT NULL
          AND p.is_active = true
        ORDER BY p.color_name
        """,
    nativeQuery = true)
    List<Object[]> findDistinctColors();

    @Query(
    value = """
        SELECT DISTINCT p.material
         FROM product p
        WHERE p.material IS NOT NULL
          AND p.is_active = true
        ORDER BY p.material
        """,
    nativeQuery = true)
    List<String> findDistinctMaterials();

    /**
     * Sizes present on at least one active product with stock. Returned in whatever
     * order Postgres picks — the caller sorts by {@code ProductSize} declaration order.
     */
    @Query(
    value = """
        SELECT DISTINCT v.size
         FROM product_variant v
         JOIN product p ON p.id = v.product_id
        WHERE p.is_active = true
          AND v.stock_quantity > 0
        """,
    nativeQuery = true)
    List<String> findDistinctInStockSizes();

    @Query(
    value = """
        SELECT * FROM product
        WHERE is_active = true
        ORDER BY sales_count DESC
        LIMIT :limit
        """,
    nativeQuery = true)
    List<Product> findMostSold(@Param("limit") int limit);

    @Query(
    value = """
        SELECT * FROM product p
        WHERE p.is_active = true
          AND p.category_id = :categoryId
          AND p.id != :productId
        ORDER BY RANDOM()
        LIMIT :limit
        """,
    nativeQuery = true)
    List<Product> findSimilarProducts(
        @Param("categoryId") Long categoryId,
        @Param("productId") Long productId,
        @Param("limit") int limit);

    @Query(
    value = """
        SELECT * FROM product p
        WHERE p.is_active = true
          AND p.category_id IN (SELECT id FROM category WHERE parent_id = :parentCategoryId)
          AND p.id NOT IN (:excludeIds)
        ORDER BY RANDOM()
        LIMIT :limit
        """,
    nativeQuery = true)
    List<Product> findSimilarFromParentCategory(
        @Param("parentCategoryId") Long parentCategoryId,
        @Param("excludeIds") List<Long> excludeIds,
        @Param("limit") int limit);

    @Query(
    value = """
        SELECT * FROM product
        WHERE is_active = true
          AND id NOT IN (:excludeIds)
        ORDER BY sales_count DESC
        LIMIT :limit
        """,
    nativeQuery = true)
    List<Product> findMostSoldExcluding(
            @Param("excludeIds") List<Long> excludeIds,
            @Param("limit") int limit);

    @Query(value = "SELECT COUNT(*) FROM product WHERE is_active = true", nativeQuery = true)
    Long countActiveProducts();

    @Query(value = """
    SELECT p.id, p.name, p.sales_count AS salesCount,
           p.price, p.image_url AS imageUrl
     FROM product p
    WHERE p.is_active = true
    ORDER BY COALESCE(p.sales_count, 0) DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<Map<String, Object>> findTopSellingProducts(@Param("limit") int limit);

    /**
     * One row per variant at or below the threshold. Stock lives on product_variant,
     * so a sold-out size shows up even when the product's other sizes are well stocked.
     * Carries {@code colorName} and {@code gender} so the caller can tell apart products
     * that share a name (e.g. "Signature" in White and Black).
     */
    @Query(value = """
    SELECT p.id AS "productId", p.name AS "name", p.image_url AS "imageUrl",
           p.color_name AS "colorName", p.gender AS "gender",
           v.id AS "variantId", v.size AS "size", v.stock_quantity AS "stockQuantity"
      FROM product_variant v
      JOIN product p ON p.id = v.product_id
     WHERE p.is_active = true
       AND v.stock_quantity <= :threshold
     ORDER BY v.stock_quantity ASC, p.name ASC, v.size ASC
    """, nativeQuery = true)
    List<Map<String, Object>> findLowStockVariants(@Param("threshold") int threshold);
}