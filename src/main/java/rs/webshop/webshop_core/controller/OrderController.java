package rs.webshop.webshop_core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.constants.OrderStatus;
import rs.webshop.webshop_core.dto.order.OrderResponse;
import rs.webshop.webshop_core.service.OrderService;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @PathVariable Long userId,
            @RequestParam(required = false) String couponCode) {
        return ResponseEntity.status(CREATED)
                .body(orderService.checkout(userId, couponCode));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<OrderResponse>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Page<OrderResponse>> getByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 5, sort = "createdAt", direction = DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getByUser(userId, pageable));
    }

    @GetMapping("/{userId}/{orderId}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long userId,
                                                 @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getById(userId, orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long orderId,
                                                      @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateStatus(orderId, status));
    }
}