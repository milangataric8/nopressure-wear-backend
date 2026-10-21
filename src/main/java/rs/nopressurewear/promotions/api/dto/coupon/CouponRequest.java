package rs.nopressurewear.promotions.api.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.promotions.api.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponRequest {

    @NotBlank(message = "validation.code.required")
    private String code;

    @NotNull(message = "validation.discountType.required")
    private DiscountType discountType;

    @NotNull(message = "validation.discountValue.required")
    private BigDecimal discountValue;

    @NotNull(message = "validation.usageLimit.required")
    @Min(value = 1, message = "validation.usageLimit.invalid")
    private Integer usageLimit;

    private LocalDateTime expiresAt;
}