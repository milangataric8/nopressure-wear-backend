package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.ProductColorVariant;

import java.util.List;

@Repository
public interface ProductColorVariantRepository extends JpaRepository<ProductColorVariant, Long> {

    List<ProductColorVariant> findByProductId(Long productId);

    boolean existsByProductIdAndVariantId(Long productId, Long variantId);

    void deleteByProductIdAndVariantId(Long productId, Long variantId);

    void deleteByProductId(Long productId);

    void deleteByVariantId(Long variantId);
}