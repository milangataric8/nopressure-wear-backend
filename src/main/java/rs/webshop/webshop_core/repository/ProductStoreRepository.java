package rs.webshop.webshop_core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.ProductStore;

import java.util.List;

@Repository
public interface ProductStoreRepository extends JpaRepository<ProductStore, Long> {
    List<ProductStore> findByProductId(Long productId);
    List<ProductStore> findByProductIdAndInStockTrue(Long productId);
    List<ProductStore> findByStoreLocationId(Long storeLocationId);
    boolean existsByProductIdAndStoreLocationId(Long productId, Long storeLocationId);
    void deleteByProductIdAndStoreLocationId(Long productId, Long storeLocationId);
}