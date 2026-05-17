package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.coupon.*;
import rs.webshop.webshop_core.service.CouponService;

import static java.util.Objects.nonNull;
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
    public ResponseEntity<Page<CouponResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "code") Pageable pageable) {
        if ((nonNull(search) && !search.isBlank())
                || nonNull(active)) {
            return ResponseEntity.ok(couponService.search(search, active, pageable));
        }
        return ResponseEntity.ok(couponService.getAll(pageable));
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