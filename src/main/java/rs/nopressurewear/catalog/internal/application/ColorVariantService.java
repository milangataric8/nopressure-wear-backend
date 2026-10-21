package rs.nopressurewear.catalog.internal.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.catalog.api.dto.product.ProductColorVariantResponse;
import rs.nopressurewear.platform.error.DuplicateResourceException;
import rs.nopressurewear.platform.error.ResourceNotFoundException;
import rs.nopressurewear.catalog.internal.domain.Product;
import rs.nopressurewear.catalog.internal.domain.ProductColorVariant;
import rs.nopressurewear.catalog.internal.persistence.ProductColorVariantRepository;
import rs.nopressurewear.catalog.internal.persistence.ProductRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorVariantService {

    private final ProductRepository productRepository;
    private final ProductColorVariantRepository variantRepository;

    @Transactional(readOnly = true)
    public List<ProductColorVariantResponse> getColorVariants(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<ProductColorVariantResponse> result = new ArrayList<>();
        result.add(toResponse(product, productId));

        variantRepository.findByProductId(productId).forEach(cv ->
                result.add(toResponse(cv.getVariant(), productId)));

        return result;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Transactional
    public List<ProductColorVariantResponse> linkColorVariant(Long productId, Long variantProductId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Product variant = productRepository.findById(variantProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant product not found"));

        if (variantRepository.existsByProductIdAndVariantId(productId, variantProductId)) {
            throw new DuplicateResourceException("errors.colorVariant.alreadyLinked");
        }

        linkColorVariants(product, variant);
        reverseLinkColorVariants(productId, variantProductId, variant, product);

        return getColorVariants(productId);
    }

    private void reverseLinkColorVariants(Long productId, Long variantProductId, Product variant, Product product) {
        if (!variantRepository.existsByProductIdAndVariantId(variantProductId, productId)) {
            linkColorVariants(variant, product);
        }
    }

    private void linkColorVariants(Product product, Product variant) {
        variantRepository.save(ProductColorVariant.builder()
                .product(product).variant(variant).build());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Transactional
    public List<ProductColorVariantResponse> unlinkColorVariant(Long productId, Long variantProductId) {
        variantRepository.deleteByProductIdAndVariantId(productId, variantProductId);
        variantRepository.deleteByProductIdAndVariantId(variantProductId, productId);
        return getColorVariants(productId);
    }

    private ProductColorVariantResponse toResponse(Product p, Long currentId) {
        return ProductColorVariantResponse.builder()
                .productId(p.getId())
                .name(p.getName())
                .colorName(p.getColorName())
                .colorHex(p.getColorHex())
                .imageUrl(p.getImageUrl())
                .sku(p.getSku())
                .isCurrent(p.getId().equals(currentId))
                .build();
    }
}