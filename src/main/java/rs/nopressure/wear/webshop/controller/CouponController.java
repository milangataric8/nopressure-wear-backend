package rs.nopressure.wear.webshop.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressure.wear.webshop.dto.coupon.CouponResponse;
import rs.nopressure.wear.webshop.dto.coupon.*;
import rs.nopressure.wear.webshop.service.CouponService;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(CREATED).body(couponService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAll() {
        return ResponseEntity.ok(couponService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CouponResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.toggleActive(id));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApplyCouponResponse> validate(
            @RequestBody ApplyCouponRequest request,
            @RequestParam Long userId) {
        return ResponseEntity.ok(couponService.validate(request.getCode(), userId));
    }
}