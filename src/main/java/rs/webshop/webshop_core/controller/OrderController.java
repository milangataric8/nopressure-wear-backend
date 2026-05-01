package rs.webshop.webshop_core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.constants.OrderStatus;
import rs.webshop.webshop_core.dto.order.OrderResponse;
import rs.webshop.webshop_core.service.OrderService;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<OrderResponse> checkout(@PathVariable Long userId) {
        return ResponseEntity.status(CREATED).body(orderService.checkout(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getByUser(userId));
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