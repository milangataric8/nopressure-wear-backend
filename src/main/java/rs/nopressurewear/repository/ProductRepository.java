package rs.nopressurewear.repository;

import jakarta.persistence.LockModeType;
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

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    java.util.Optional<Product> findByIdForUpdate(@Param("id") Long id);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByCategoryId(Long categoryId);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByIsActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Product> findByIsActiveTrueAndCategoryIdIn(List<Long> categoryIds, Pageable pageable);

    List<Product> findBySkuContainingAndIdNot(String sku, Long id);

    List<Product> findByIsActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(int stockQuantity);

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
        AND (:excludeIds IS NULL OR p.id NOT IN (:excludeIds))
        ORDER BY p.id DESC
        LIMIT :limit
        """,
    nativeQuery = true)
    List<Product> findActiveFillerProducts(
        @Param("excludeIds") List<Long> excludeIds,
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
           p.price, p.stock_quantity AS stockQuantity,
           p.image_url AS imageUrl
     FROM product p
    WHERE p.is_active = true
    ORDER BY p.sales_count DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<Map<String, Object>> findTopSellingProducts(@Param("limit") int limit);

    @Query(value = """
    SELECT p.id, p.name, p.stock_quantity AS stockQuantity,
           p.image_url AS imageUrl
     FROM product p
    WHERE p.is_active = true AND p.stock_quantity <= :threshold
    ORDER BY p.stock_quantity ASC
    """, nativeQuery = true)
    List<Map<String, Object>> findLowStockProducts(@Param("threshold") int threshold);
}