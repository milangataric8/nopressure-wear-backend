package rs.nopressurewear.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query(
    value = """
        SELECT * FROM orders o
        WHERE (:status IS NULL
                   OR o.status = :status)
          AND (:search IS NULL
                    OR LOWER(o.customer_full_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(o.order_code) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY o.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM orders o
         WHERE (:status IS NULL
                    OR o.status = :status)
           AND (:search IS NULL
                    OR LOWER(o.customer_full_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(o.order_code) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
    nativeQuery = true)
    Page<Order> findByFilters(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    @Query(
    value = """
        SELECT COALESCE(SUM(total_amount), 0)
         FROM orders
        """,
    nativeQuery = true)
    BigDecimal getTotalRevenue();

    @Query(
    value = """
        SELECT COALESCE(SUM(total_amount), 0)
         FROM orders
        WHERE created_at >= :since
        """,
    nativeQuery = true)
    BigDecimal getRevenueSince(@Param("since") LocalDateTime since);

    @Query(
    value = """
        SELECT COUNT(*)
         FROM orders
        WHERE
        created_at >= :since
        """,
    nativeQuery = true)
    Long countOrdersSince(@Param("since") LocalDateTime since);

    @Query(
    value = """
        SELECT TO_CHAR(created_at, 'YYYY-MM') AS month,
               COALESCE(SUM(total_amount), 0) AS revenue,
               COUNT(*) AS orders
         FROM orders
        WHERE created_at >= NOW() - INTERVAL '12 months'
        GROUP BY TO_CHAR(created_at, 'YYYY-MM')
        ORDER BY month ASC
        """,
    nativeQuery = true)
    List<Map<String, Object>> getRevenueByMonth();

    @Query(
    value = """
       SELECT status, COUNT(*) AS count
         FROM orders
        GROUP BY status
       """,
    nativeQuery = true)
    List<Map<String, Object>> getOrderCountByStatus();

    @Query(
    value = """
        SELECT u.first_name || ' ' || u.last_name AS name,
               u.email,
               COUNT(o.id) AS orders,
               COALESCE(SUM(o.total_amount), 0) AS total_spent
         FROM orders o
         JOIN users u ON u.id = o.user_id
        GROUP BY u.id, u.first_name, u.last_name, u.email
        ORDER BY total_spent DESC
        LIMIT :limit
        """,
    nativeQuery = true)
    List<Map<String, Object>> findTopCustomers(@Param("limit") int limit);

    @Query(
    value = """
        SELECT c.name AS category,
               COALESCE(SUM(oi.price_at_purchase * oi.quantity), 0) AS revenue
         FROM order_item oi
         JOIN product p ON p.id = oi.product_id
         JOIN category c ON c.id = p.category_id
        GROUP BY c.id, c.name
        ORDER BY revenue DESC
        """,
    nativeQuery = true)
    List<Map<String, Object>> getRevenueByCategory();

    @Query(value = "SELECT COUNT(*) FROM orders WHERE payment_method = :method", nativeQuery = true)
    Long countByPaymentMethod(@Param("method") String method);

    @Query(
    value = """
        SELECT o.id, o.order_code AS orderCode,
               o.customer_full_name AS customerName,
               o.total_amount AS totalAmount,
               o.status, o.payment_method AS paymentMethod,
               o.created_at AS createdAt
         FROM orders o
        ORDER BY o.created_at DESC
        LIMIT :limit
        """,
    nativeQuery = true)
    List<Map<String, Object>> findRecentOrders(@Param("limit") int limit);

    List<Order> findAllByOrderByCreatedAtDesc();

    @Query(
    value = """
        SELECT u.first_name || ' ' || u.last_name AS name,
               u.email,
               COUNT(o.id) AS orders,
               COALESCE(SUM(o.total_amount), 0) AS total_spent
         FROM orders o
         JOIN users u ON u.id = o.user_id
        GROUP BY u.id, u.first_name, u.last_name, u.email
        ORDER BY total_spent DESC
        """,
    nativeQuery = true)
    List<Map<String, Object>> findAllCustomerStats();
}