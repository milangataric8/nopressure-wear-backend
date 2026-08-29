package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.Cart;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("""
        SELECT c FROM Cart c
        WHERE c.user IS NOT NULL
          AND c.reminderSentAt IS NULL
          AND c.updatedAt < :idleBefore
          AND c.updatedAt > :notOlderThan
          AND SIZE(c.cartItems) > 0
        """)
    List<Cart> findAbandonedCarts(@Param("idleBefore") LocalDateTime idleBefore,
                                  @Param("notOlderThan") LocalDateTime notOlderThan);
}