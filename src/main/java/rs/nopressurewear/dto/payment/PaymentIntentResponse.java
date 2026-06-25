package rs.nopressurewear.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentIntentResponse {
    private String paymentIntentId;
    private String clientSecret;
    private Long amount;
    private String currency;
}