package rs.webshop.webshop_core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.ProductColorVariant;

import java.util.List;

@Repository
public interface ProductColorVariantRepository extends JpaRepository<ProductColorVariant, Long> {

    List<ProductColorVariant> findByProductId(Long productId);

    boolean existsByProductIdAndVariantId(Long productId, Long variantId);
}