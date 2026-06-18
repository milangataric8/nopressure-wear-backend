package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // Key metrics
        overview.put("totalRevenue", orderRepository.getTotalRevenue());
        overview.put("totalOrders", orderRepository.count());
        overview.put("totalCustomers", userRepository.countCustomers());
        overview.put("totalProducts", productRepository.countActiveProducts());

        // This month
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        overview.put("monthlyRevenue", orderRepository.getRevenueSince(startOfMonth));
        overview.put("monthlyOrders", orderRepository.countOrdersSince(startOfMonth));
        overview.put("monthlyCustomers", userRepository.countCustomersSince(startOfMonth));

        // Today
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        overview.put("todayRevenue", orderRepository.getRevenueSince(startOfDay));
        overview.put("todayOrders", orderRepository.countOrdersSince(startOfDay));

        // Average order value
        BigDecimal totalRevenue = (BigDecimal) overview.get("totalRevenue");
        Long totalOrders = (Long) overview.get("totalOrders");
        overview.put("averageOrderValue",
                totalOrders > 0 && totalRevenue != null
                        ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, HALF_UP)
                        : ZERO);

        return overview;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<Map<String, Object>> getRevenueByMonth() {
        return orderRepository.getRevenueByMonth();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<Map<String, Object>> getOrdersByStatus() {
        return orderRepository.getOrderCountByStatus();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<Map<String, Object>> getTopProducts(int limit) {
        return productRepository.findTopSellingProducts(limit);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<Map<String, Object>> getTopCustomers(int limit) {
        return orderRepository.findTopCustomers(limit);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<Map<String, Object>> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<Map<String, Object>> getRevenueByCategory() {
        return orderRepository.getRevenueByCategory();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Map<String, Object> getPaymentMethodStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("card", orderRepository.countByPaymentMethod("CARD"));
        stats.put("cod", orderRepository.countByPaymentMethod("COD"));
        return stats;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<Map<String, Object>> getRecentOrders(int limit) {
        return orderRepository.findRecentOrders(limit);
    }
}