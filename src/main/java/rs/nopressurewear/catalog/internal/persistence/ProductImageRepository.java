package rs.nopressurewear.catalog.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.catalog.internal.domain.ProductImage;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    int countByProductIdAndIsPrimaryFalse(Long productId);

    void deleteByProductId(Long productId);
}