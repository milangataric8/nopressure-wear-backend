package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.product.ProductRequest;
import rs.webshop.webshop_core.dto.product.ProductResponse;
import rs.webshop.webshop_core.exception.DuplicateResourceException;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Category;
import rs.webshop.webshop_core.model.Product;
import rs.webshop.webshop_core.repository.CategoryRepository;
import rs.webshop.webshop_core.repository.ProductRepository;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product with this SKU already exists");
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .build();

        if (nonNull(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        return toResponse(productRepository.save(product));
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toResponse(product);
    }

    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<ProductResponse> getActive(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable).map(this::toResponse);
    }

    public Page<ProductResponse> getByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable).map(this::toResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());

        if (nonNull(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        return toResponse(productRepository.save(product));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        return toResponse(productRepository.save(product));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse activate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(true);
        return toResponse(productRepository.save(product));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productRepository.delete(product);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sku(product.getSku())
                .imageUrl(product.getImageUrl())
                .isActive(product.isActive())
                .categoryId(nonNull(product.getCategory()) ? product.getCategory().getId() : null)
                .categoryName(nonNull(product.getCategory()) ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}