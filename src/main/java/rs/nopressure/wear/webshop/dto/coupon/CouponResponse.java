package rs.nopressure.wear.webshop.dto.coupon;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.nopressure.wear.webshop.constants.DiscountType;

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