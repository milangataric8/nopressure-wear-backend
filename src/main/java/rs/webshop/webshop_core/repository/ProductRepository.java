package rs.webshop.webshop_core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.Product;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByCategoryId(Long categoryId);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByIsActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Product> findByIsActiveTrueAndCategoryIdIn(List<Long> categoryIds, Pageable pageable);

    List<Product> findBySkuContainingAndIdNot(String sku, Long id);

    @Query(value = """
        SELECT * FROM product p
        WHERE (:categoryId IS NULL OR p.category_id = :categoryId
               OR p.category_id IN (
                   SELECT id FROM category WHERE parent_id = :categoryId
               ))
        AND (:search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR p.is_active = :active)
        ORDER BY p.name ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM product p
        WHERE (:categoryId IS NULL OR p.category_id = :categoryId
               OR p.category_id IN (
                   SELECT id FROM category WHERE parent_id = :categoryId
               ))
        AND (:search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:active IS NULL OR p.is_active = :active)
        """,
            nativeQuery = true)
    Page<Product> findByFilters(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query(value = """
        SELECT * FROM product p
        WHERE p.is_active = true
        AND (:categoryId IS NULL OR p.category_id = :categoryId
               OR p.category_id IN (
                   SELECT id FROM category WHERE parent_id = :categoryId
               ))
        AND (:search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        ORDER BY p.name ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM product p
        WHERE p.is_active = true
        AND (:categoryId IS NULL OR p.category_id = :categoryId
               OR p.category_id IN (
                   SELECT id FROM category WHERE parent_id = :categoryId
               ))
        AND (:search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        """,
            nativeQuery = true)
    Page<Product> findActiveByFilters(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}