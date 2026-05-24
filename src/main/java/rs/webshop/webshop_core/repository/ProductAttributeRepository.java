package rs.webshop.webshop_core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.ProductAttribute;

import java.util.List;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
    List<ProductAttribute> findByProductId(Long productId);

    @Query(value = "SELECT DISTINCT attribute_key FROM product_attribute ORDER BY attribute_key", nativeQuery = true)
    List<String> findDistinctKeys();

    @Query(value = "SELECT DISTINCT attribute_value FROM product_attribute WHERE attribute_key = :key ORDER BY attribute_value", nativeQuery = true)
    List<String> findDistinctValuesByKey(String key);
}