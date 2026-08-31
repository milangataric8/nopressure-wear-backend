package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rs.nopressurewear.constants.StockDefaults;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.ProductVariant;
import rs.nopressurewear.repository.ProductVariantRepository;
import rs.nopressurewear.repository.StoreSettingsRepository;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class LowStockService {

    private static final Logger log = LoggerFactory.getLogger(LowStockService.class);

    private final StoreSettingsRepository settingsRepository;
    private final EmailService emailService;
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
        return settingsRepository.findByKey("low_stock_alerts_enabled")
                .map(s -> !"false".equalsIgnoreCase(s.getValue()))
                .orElse(true);
    }

    private int threshold() {
        return settingsRepository.findByKey("low_stock_threshold")
                .map(s -> {
                    try { return Integer.parseInt(s.getValue().trim()); }
                    catch (Exception e) { return StockDefaults.LOW_STOCK_THRESHOLD_VALUE; }
                })
                .orElse(StockDefaults.LOW_STOCK_THRESHOLD_VALUE);
    }

    private String defaultLanguage() {
        return settingsRepository.findByKey("default_language")
                .map(s -> s.getValue())
                .orElse("en");
    }
}
