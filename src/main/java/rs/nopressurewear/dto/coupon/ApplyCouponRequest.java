package rs.nopressurewear.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyCouponRequest {

    @NotBlank(message = "validation.couponInvalid")
    private String code;
}