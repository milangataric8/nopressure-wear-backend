package rs.nopressurewear.dto.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentIntentRequest {
    private Long userId;
    private String couponCode;
}