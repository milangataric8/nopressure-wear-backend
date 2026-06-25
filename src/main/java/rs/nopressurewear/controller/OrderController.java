package rs.nopressurewear.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.constants.OrderStatus;
import rs.nopressurewear.dto.order.GuestOrderRequest;
import rs.nopressurewear.dto.order.OrderResponse;
import rs.nopressurewear.service.OrderService;

import static java.util.Objects.nonNull;
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
            @RequestParam(required = false) String couponCode,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.status(CREATED)
                .body(orderService.checkout(userId, couponCode, paymentMethod, lang));
    }

    @PostMapping("/guest-checkout")
    public ResponseEntity<OrderResponse> guestCheckout(
            @Valid @RequestBody GuestOrderRequest request,
            @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.status(CREATED).body(orderService.guestCheckout(request, lang));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Page<OrderResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        if ((nonNull(search) && !search.isBlank()) || (nonNull(status) && !status.isBlank())) {
            return search(search, status, pageable);
        }
        return findAll(pageable);
    }

    @GetMapping("/user/{userId}/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Page<OrderResponse>> getByUserId(
            @PathVariable Long userId,
            @PageableDefault() Pageable pageable) {
        return ResponseEntity.ok(orderService.getByUserId(userId, pageable));
    }

    private ResponseEntity<Page<OrderResponse>> search(String search, String status, @PageableDefault Pageable pageable){
        return ResponseEntity.ok(orderService.search(search, status, pageable));
    }

    private ResponseEntity<Page<OrderResponse>> findAll(
            @PageableDefault(sort = "createdAt", direction = DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAll(pageable));
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

    @GetMapping("/admin/{orderId}")
    public ResponseEntity<OrderResponse> getByIdAdmin(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getByIdAdmin(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long orderId,
                                                      @RequestParam OrderStatus status,
                                                      @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.ok(orderService.updateStatus(orderId, status, lang));
    }
}