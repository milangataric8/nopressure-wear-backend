package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.constants.ProductSize;
import rs.nopressurewear.model.ProductVariant;

import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByProductIdAndSize(Long productId, ProductSize size);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.size = :size")
    Optional<ProductVariant> findWithLockByProductIdAndSize(@Param("productId") Long productId, @Param("size") ProductSize size);

    @Modifying
    @Query("DELETE FROM ProductVariant v WHERE v.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
