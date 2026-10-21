package rs.nopressurewear.ordering.internal.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.nopressurewear.storefront.api.SettingsQuery;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DeliveryService implements rs.nopressurewear.ordering.api.DeliveryPricing {

    private final SettingsQuery storeSettingsRepository;

    public BigDecimal calculateDeliveryFee(BigDecimal subtotal) {
        boolean enabled = getBool("delivery_enabled", true);
        if (!enabled) return BigDecimal.ZERO;

        BigDecimal threshold = getDecimal("free_shipping_threshold", BigDecimal.ZERO);
        if (threshold.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(threshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return getDecimal("delivery_fee", BigDecimal.ZERO);
    }

    public BigDecimal getDeliveryFeeSetting() {
        return getDecimal("delivery_fee", BigDecimal.ZERO);
    }

    public BigDecimal getFreeShippingThreshold() {
        return getDecimal("free_shipping_threshold", BigDecimal.ZERO);
    }

    public boolean isDeliveryEnabled() {
        return getBool("delivery_enabled", true);
    }

    private boolean getBool(String key, boolean def) {
        return storeSettingsRepository.findValue(key)
                .map(s -> !"false".equalsIgnoreCase(s))
                .orElse(def);
    }

    private BigDecimal getDecimal(String key, BigDecimal def) {
        return storeSettingsRepository.findValue(key)
                .map(s -> {
                    try { return new BigDecimal(s.trim()); }
                    catch (Exception e) { return def; }
                })
                .orElse(def);
    }
}
