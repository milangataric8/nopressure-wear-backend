package rs.webshop.webshop_core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query(value = """
        SELECT * FROM orders o
        WHERE (:status IS NULL OR o.status = :status)
        AND (:search IS NULL
                    OR LOWER(o.customer_full_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(o.order_code) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY o.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM orders o
        WHERE (:status IS NULL OR o.status = :status)
        AND (:search IS NULL
                    OR LOWER(o.customer_full_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(o.order_code) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
            nativeQuery = true)
    Page<Order> findByFilters(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}