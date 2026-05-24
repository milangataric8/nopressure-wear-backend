package rs.webshop.webshop_core.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentIntentResponse {
    private String clientSecret;
    private Long amount;
    private String currency;
}