package rs.nopressurewear.ordering.api;

import java.math.BigDecimal;

public interface DeliveryPricing {
    BigDecimal calculateDeliveryFee(BigDecimal subtotal);
    BigDecimal getDeliveryFeeSetting();
    BigDecimal getFreeShippingThreshold();
    boolean isDeliveryEnabled();
}
