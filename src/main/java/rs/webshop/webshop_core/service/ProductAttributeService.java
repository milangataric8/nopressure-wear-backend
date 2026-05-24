package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.product.ProductAttributeRequest;
import rs.webshop.webshop_core.dto.product.ProductAttributeResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Product;
import rs.webshop.webshop_core.model.ProductAttribute;
import rs.webshop.webshop_core.repository.ProductAttributeRepository;
import rs.webshop.webshop_core.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductAttributeService {

    private final ProductAttributeRepository productAttributeRepository;
    private final ProductRepository productRepository;

    public ProductAttributeResponse addAttribute(Long productId, ProductAttributeRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductAttribute attribute = ProductAttribute.builder()
                .product(product)
                .key(request.getKey())
                .value(request.getValue())
                .build();

        return toAttributeResponse(productAttributeRepository.save(attribute));
    }

    public void deleteAttribute(Long attributeId) {
        ProductAttribute attribute = productAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found"));
        productAttributeRepository.delete(attribute);
    }

    private ProductAttributeResponse toAttributeResponse(ProductAttribute attr) {
        return ProductAttributeResponse.builder()
                .id(attr.getId())
                .key(attr.getKey())
                .value(attr.getValue())
                .build();
    }
}
