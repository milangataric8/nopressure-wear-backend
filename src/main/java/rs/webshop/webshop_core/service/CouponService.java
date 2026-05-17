package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.category.CategoryResponse;
import rs.webshop.webshop_core.dto.coupon.*;
import rs.webshop.webshop_core.exception.DuplicateResourceException;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Cart;
import rs.webshop.webshop_core.model.Coupon;
import rs.webshop.webshop_core.repository.CartRepository;
import rs.webshop.webshop_core.repository.CouponRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.nonNull;
import static rs.webshop.webshop_core.constants.DiscountType.PERCENTAGE;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new DuplicateResourceException("Coupon with this code already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .usageLimit(request.getUsageLimit())
                .usageCount(0)
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .build();

        return toResponse(couponRepository.save(coupon));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<CouponResponse> getAll(Pageable pageable) {
        return couponRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<CouponResponse> search(String query, Boolean active, Pageable pageable) {
        return couponRepository.findByFilters(query, active, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public void delete(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        couponRepository.delete(coupon);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public CouponResponse toggleActive(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        coupon.setActive(!coupon.isActive());
        return toResponse(couponRepository.save(coupon));
    }

    public ApplyCouponResponse validate(String code, Long userId) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        validateCoupon(coupon);

        var cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        BigDecimal totalPrice = calculateTotalPrice(cart);

        BigDecimal discountAmount = applyCouponDiscount(coupon, totalPrice);

        BigDecimal finalTotal = totalPrice.subtract(discountAmount);

        return buildCouponResponse(coupon, discountAmount, totalPrice, finalTotal);
    }

    private static void validateCoupon(Coupon coupon) {
        if (!coupon.isActive()) {
            throw new RuntimeException("Coupon is not active");
        }

        if (nonNull(coupon.getExpiresAt()) && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Coupon has expired");
        }

        if (coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new RuntimeException("Coupon usage limit reached");
        }
    }

    public static BigDecimal calculateTotalPrice(Cart cart) {
        return cart.getCartItems().stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(ZERO, BigDecimal::add);
    }

    public static BigDecimal applyCouponDiscount(Coupon coupon, BigDecimal originalTotal) {
        BigDecimal discountAmount;

        if (coupon.getDiscountType() == PERCENTAGE) {
            discountAmount = originalTotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, HALF_UP);
        } else {
            discountAmount = coupon.getDiscountValue().min(originalTotal);
        }

        return discountAmount;
    }

    private static ApplyCouponResponse buildCouponResponse(Coupon coupon,
                                                           BigDecimal discountAmount,
                                                           BigDecimal originalTotal,
                                                           BigDecimal finalTotal) {
        return ApplyCouponResponse.builder()
                .code(coupon.getCode())
                .discountAmount(discountAmount)
                .originalTotal(originalTotal)
                .finalTotal(finalTotal)
                .message(coupon.getDiscountType() == PERCENTAGE
                        ? coupon.getDiscountValue() + "% discount applied"
                        : "$" + coupon.getDiscountValue() + " discount applied")
                .build();
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .active(coupon.isActive())
                .expiresAt(coupon.getExpiresAt())
                .usageLimit(coupon.getUsageLimit())
                .usageCount(coupon.getUsageCount())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}