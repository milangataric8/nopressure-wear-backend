package rs.webshop.webshop_core.dto.coupon;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ApplyCouponResponse {

    private String code;
    private BigDecimal discountAmount;
    private BigDecimal originalTotal;
    private BigDecimal finalTotal;
    private String message;
}