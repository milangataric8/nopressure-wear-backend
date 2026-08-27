package rs.nopressurewear.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.constants.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponRequest {

    @NotBlank(message = "validation.codeRequired")
    private String code;

    @NotNull(message = "validation.required")
    private DiscountType discountType;

    @NotNull(message = "validation.required")
    private BigDecimal discountValue;

    @NotNull(message = "validation.required")
    @Min(value = 1, message = "validation.usageLimitInvalid")
    private Integer usageLimit;

    private LocalDateTime expiresAt;
}