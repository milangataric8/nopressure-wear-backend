package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.service.DashboardService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    @GetMapping("/revenue-by-month")
    public ResponseEntity<List<Map<String, Object>>> getRevenueByMonth() {
        return ResponseEntity.ok(dashboardService.getRevenueByMonth());
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<List<Map<String, Object>>> getOrdersByStatus() {
        return ResponseEntity.ok(dashboardService.getOrdersByStatus());
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getTopProducts(limit));
    }

    @GetMapping("/top-customers")
    public ResponseEntity<List<Map<String, Object>>> getTopCustomers(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getTopCustomers(limit));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Map<String, Object>>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(dashboardService.getLowStockProducts(threshold));
    }

    @GetMapping("/revenue-by-category")
    public ResponseEntity<List<Map<String, Object>>> getRevenueByCategory() {
        return ResponseEntity.ok(dashboardService.getRevenueByCategory());
    }

    @GetMapping("/payment-stats")
    public ResponseEntity<Map<String, Object>> getPaymentStats() {
        return ResponseEntity.ok(dashboardService.getPaymentMethodStats());
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<List<Map<String, Object>>> getRecentOrders(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentOrders(limit));
    }
}