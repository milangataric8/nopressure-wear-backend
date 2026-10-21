package rs.nopressurewear.shopping.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.shopping.internal.domain.CartItem;
import rs.nopressurewear.catalog.api.ProductSize;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductIdAndSize(Long cartId, Long productId, ProductSize size);
}