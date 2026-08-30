package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.constants.ProductSize;
import rs.nopressurewear.model.ProductVariant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    /**
     * Total variant stock per product, for the computed {@code stockQuantity} on the
     * product response. One query for a whole page of products — no per-row lookup.
     * Rows: {@code [productId (Long), totalStock (Long)]}. Products absent from the
     * result have no variants and should be treated as 0.
     */
    @Query("""
        SELECT v.product.id, COALESCE(SUM(v.stockQuantity), 0)
          FROM ProductVariant v
         WHERE v.product.id IN :productIds
         GROUP BY v.product.id
        """)
    List<Object[]> sumStockByProductIds(@Param("productIds") Collection<Long> productIds);

    Optional<ProductVariant> findByProductIdAndSize(Long productId, ProductSize size);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.size = :size")
    Optional<ProductVariant> findWithLockByProductIdAndSize(@Param("productId") Long productId, @Param("size") ProductSize size);

    @Modifying
    @Query("DELETE FROM ProductVariant v WHERE v.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
