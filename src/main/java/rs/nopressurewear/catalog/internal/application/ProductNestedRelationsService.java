package rs.nopressurewear.catalog.internal.application;
import rs.nopressurewear.storefront.internal.application.StoreLocationService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.storefront.api.dto.store.ProductStoreRequest;
import rs.nopressurewear.platform.error.FieldValidationException;
import rs.nopressurewear.catalog.internal.domain.Product;
import rs.nopressurewear.storefront.internal.domain.StoreLocation;
import rs.nopressurewear.catalog.internal.persistence.ProductRepository;
import rs.nopressurewear.storefront.internal.persistence.StoreLocationRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;

@Service
@RequiredArgsConstructor
public class ProductNestedRelationsService {

    private static final Logger log = LoggerFactory.getLogger(ProductNestedRelationsService.class);

    private final ColorVariantService colorVariantService;
    private final StoreLocationService storeLocationService;
    private final ProductRepository productRepository;
    private final StoreLocationRepository storeLocationRepository;

    /**
     * Links color-variant products and creates store-availability rows for a freshly
     * created product, reusing the exact service methods the standalone link/add-store
     * endpoints use so the grouping logic is never duplicated. Runs in {@code create}'s
     * transaction — any failure here rolls back the product too.
     *
     * <p>{@code null} means "not provided"; only a non-null, non-empty list does anything.
     */
    @Transactional(propagation = MANDATORY)
    public void linkNestedRelations(Long productId, List<Long> colorVariantIds, List<ProductStoreRequest> stores) {
        if (nonNull(colorVariantIds)) {
            for (Long variantProductId : colorVariantIds) {
                colorVariantService.linkColorVariant(productId, variantProductId);
            }
        }
        if (nonNull(stores)) {
            for (ProductStoreRequest store : stores) {
                storeLocationService.addProductToStore(productId, store);
            }
        }
    }

    /**
     * Reject a bad nested payload before anything is written,
     * so a failure never leaves a half-built product behind.
     * */
    public void validateNestedRelations(List<Long> colorVariantIds, List<ProductStoreRequest> stores) {
        colorVariantsExists(colorVariantIds);
        storeLocationsExists(stores);
    }

    private void colorVariantsExists(List<Long> colorVariantIds) {
        if (nonNull(colorVariantIds) && !colorVariantIds.isEmpty()) {
            if (colorVariantIds.stream().anyMatch(Objects::isNull)) {
                throw new FieldValidationException("validation.colorVariantIds.required", "colorVariantIds");
            }
            if (new HashSet<>(colorVariantIds).size() != colorVariantIds.size()) {
                throw new FieldValidationException("validation.colorVariantIds.duplicate", "colorVariantIds");
            }

            Set<Long> found = productRepository.findAllById(colorVariantIds).stream()
                    .map(Product::getId)
                    .collect(Collectors.toSet());
            List<Long> missing = colorVariantIds.stream().filter(id -> !found.contains(id)).toList();
            if (!missing.isEmpty()) {
                // the specific missing ids are dev-facing detail only now; log them, don't try to
                // squeeze a dynamic list into an i18n key.
                log.warn("Color variant products not found: {}", missing);
                throw new FieldValidationException("validation.colorVariantIds.notFound", "colorVariantIds");
            }
        }
    }

    private void storeLocationsExists(List<ProductStoreRequest> stores) {
        if (nonNull(stores) && !stores.isEmpty()) {
            List<Long> storeIds = stores.stream().map(ProductStoreRequest::getStoreLocationId).toList();
            if (storeIds.stream().anyMatch(Objects::isNull)) {
                throw new FieldValidationException("validation.stores.required", "stores");
            }
            if (new HashSet<>(storeIds).size() != storeIds.size()) {
                throw new FieldValidationException("validation.stores.duplicate", "stores");
            }

            Set<Long> found = storeLocationRepository.findAllById(storeIds).stream()
                    .map(StoreLocation::getId)
                    .collect(Collectors.toSet());
            List<Long> missing = storeIds.stream().filter(id -> !found.contains(id)).toList();
            if (!missing.isEmpty()) {
                log.warn("Store locations not found: {}", missing);
                throw new FieldValidationException("validation.stores.notFound", "stores");
            }
        }
    }
}
