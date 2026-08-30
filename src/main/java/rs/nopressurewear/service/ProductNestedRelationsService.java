package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.dto.store.ProductStoreRequest;
import rs.nopressurewear.exception.FieldValidationException;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.StoreLocation;
import rs.nopressurewear.repository.ProductRepository;
import rs.nopressurewear.repository.StoreLocationRepository;

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
                throw new FieldValidationException("Color variant id must not be null", "colorVariantIds");
            }
            if (new HashSet<>(colorVariantIds).size() != colorVariantIds.size()) {
                throw new FieldValidationException("Duplicate color variant ids", "colorVariantIds");
            }

            Set<Long> found = productRepository.findAllById(colorVariantIds).stream()
                    .map(Product::getId)
                    .collect(Collectors.toSet());
            List<Long> missing = colorVariantIds.stream().filter(id -> !found.contains(id)).toList();
            if (!missing.isEmpty()) {
                throw new FieldValidationException("Color variant products not found: " + missing, "colorVariantIds");
            }
        }
    }

    private void storeLocationsExists(List<ProductStoreRequest> stores) {
        if (nonNull(stores) && !stores.isEmpty()) {
            List<Long> storeIds = stores.stream().map(ProductStoreRequest::getStoreLocationId).toList();
            if (storeIds.stream().anyMatch(Objects::isNull)) {
                throw new FieldValidationException("Store location id must not be null", "stores");
            }
            if (new HashSet<>(storeIds).size() != storeIds.size()) {
                throw new FieldValidationException("Duplicate store location ids", "stores");
            }

            Set<Long> found = storeLocationRepository.findAllById(storeIds).stream()
                    .map(StoreLocation::getId)
                    .collect(Collectors.toSet());
            List<Long> missing = storeIds.stream().filter(id -> !found.contains(id)).toList();
            if (!missing.isEmpty()) {
                throw new FieldValidationException("Store location not found: " + missing, "stores");
            }
        }
    }
}
