package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.dto.product.ProductColorVariantResponse;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.ProductColorVariant;
import rs.nopressurewear.repository.ProductColorVariantRepository;
import rs.nopressurewear.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorVariantService {

    private final ProductRepository productRepository;
    private final ProductColorVariantRepository variantRepository;

    @Transactional(readOnly = true)
    public List<ProductColorVariantResponse> getVariants(Long productId) {
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
    public List<ProductColorVariantResponse> linkVariant(Long productId, Long variantProductId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Product variant = productRepository.findById(variantProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant product not found"));

        if (variantRepository.existsByProductIdAndVariantId(productId, variantProductId)) {
            throw new DuplicateResourceException("Variant already linked");
        }

        linkColorVariants(product, variant);
        reverseLinkColorVariants(productId, variantProductId, variant, product);

        return getVariants(productId);
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
    public List<ProductColorVariantResponse> unlinkVariant(Long productId, Long variantProductId) {
        variantRepository.deleteByProductIdAndVariantId(productId, variantProductId);
        variantRepository.deleteByProductIdAndVariantId(variantProductId, productId);
        return getVariants(productId);
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