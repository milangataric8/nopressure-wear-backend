package rs.nopressurewear.dto.coupon;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.constants.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Boolean active;
    private LocalDateTime expiresAt;
    private Integer usageLimit;
    private Integer usageCount;
    private LocalDateTime createdAt;
}