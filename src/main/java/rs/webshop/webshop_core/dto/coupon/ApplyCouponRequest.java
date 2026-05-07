package rs.webshop.webshop_core.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;
}