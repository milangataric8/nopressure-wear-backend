package rs.nopressurewear.catalog.internal.application;
import rs.nopressurewear.notifications.api.EmailOperations;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rs.nopressurewear.catalog.api.StockDefaults;
import rs.nopressurewear.catalog.internal.domain.Product;
import rs.nopressurewear.catalog.internal.domain.ProductVariant;
import rs.nopressurewear.catalog.internal.persistence.ProductVariantRepository;
import rs.nopressurewear.storefront.api.SettingsQuery;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class LowStockService {

    private static final Logger log = LoggerFactory.getLogger(LowStockService.class);

    private final SettingsQuery settingsRepository;
    private final EmailOperations emailService;
    private final ProductVariantRepository productVariantRepository;

    public void checkAndAlertVariant(Product product, ProductVariant variant) {
        try {
            if (!alertsEnabled()) return;
            int threshold = threshold();
            int stock = variant.getStockQuantity();

            if (stock <= threshold && !variant.isLowStockAlerted()) {
                String lang = defaultLanguage();
                String gender = nonNull(product.getGender()) ? product.getGender().name() : null;
                emailService.sendAdminLowStockAlert(product.getName(), product.getSku(), product.getColorName(),
                        gender, variant.getSize().name(), stock, threshold, lang);
                variant.setLowStockAlerted(true);
                productVariantRepository.save(variant);
            }
        } catch (Exception e) {
            log.warn("Low-stock alert failed for product {} variant {}: {}", product.getId(), variant.getSize(), e.getMessage());
        }
    }

    private boolean alertsEnabled() {
        return settingsRepository.findValue("low_stock_alerts_enabled")
                .map(s -> !"false".equalsIgnoreCase(s))
                .orElse(true);
    }

    private int threshold() {
        return settingsRepository.findValue("low_stock_threshold")
                .map(s -> {
                    try { return Integer.parseInt(s.trim()); }
                    catch (Exception e) { return StockDefaults.LOW_STOCK_THRESHOLD_VALUE; }
                })
                .orElse(StockDefaults.LOW_STOCK_THRESHOLD_VALUE);
    }

    private String defaultLanguage() {
        return settingsRepository.findValue("default_language")
                .orElse("en");
    }
}
