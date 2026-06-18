package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.product.*;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Category;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.ProductColorVariant;
import rs.nopressurewear.model.ProductImage;
import rs.nopressurewear.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductColorVariantRepository colorVariantRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ProductResponse create(ProductRequest request) {

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .videoUrl(request.getVideoUrl())
                .brand(request.getBrand())
                .colorName(request.getColorName())
                .colorHex(request.getColorHex())
                .build();

        if (nonNull(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        product.setMaterial(request.getMaterial());
        product.setDiscountPercentage(nonNull(request.getDiscountPercentage())
                ? request.getDiscountPercentage()
                : ZERO);
        calculateDiscountPrice(product);

        Product saved = productRepository.save(product);
        linkColorVariants(saved);
        return toResponse(saved);

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

    public List<Product> getByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream().toList();
    }

    public Page<ProductResponse> getByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable).map(this::toResponse);
    }

    public Page<ProductResponse> getByCategories(List<Long> categoryIds, Pageable pageable) {
        return productRepository.findByIsActiveTrueAndCategoryIdIn(categoryIds, pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByIsActiveTrueAndPriceBetween(minPrice, maxPrice, pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> filter(Long categoryId,
                                        String search,
                                        Boolean active,
                                        String brand,
                                        String colorName,
                                        String material,
                                        Pageable pageable) {
        String searchParam = (nonNull(search) && !search.isBlank()) ? search : null;
        String brandParam = (nonNull(brand) && !brand.isBlank()) ? brand : null;
        String colorParam = (nonNull(colorName) && !colorName.isBlank()) ? colorName : null;
        String materialParam = (nonNull(material) && !material.isBlank()) ? material : null;

        return findByFilters(categoryId, searchParam, brandParam, colorParam, materialParam, active, pageable);
    }

    private Page<ProductResponse> findByFilters(Long categoryId,
                                                      String searchParam,
                                                      String brandParam,
                                                      String colorParam,
                                                      String material,
                                                      Boolean active,
                                                      Pageable pageable) {
        return productRepository.findByFilters(categoryId, searchParam, null, null, brandParam, colorParam, material, active, pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> getActiveFiltered(Long categoryId,
                                                   String search,
                                                   BigDecimal minPrice,
                                                   BigDecimal maxPrice,
                                                   String brand,
                                                   String colorName,
                                                   String material,
                                                   Pageable pageable) {
        String searchParam = (nonNull(search) && !search.isBlank()) ? search : null;
        String brandParam = (nonNull(brand) && !brand.isBlank()) ? brand : null;
        String colorParam = (nonNull(colorName) && !colorName.isBlank()) ? colorName : null;
        String materialParam = (nonNull(material) && !material.isBlank()) ? material : null;

        return productRepository
                .findByFilters(categoryId, searchParam, minPrice, maxPrice, brandParam, colorParam, materialParam, Boolean.TRUE, pageable)
                .map(this::toResponse);
    }

    public Map<String, Object> getAvailableFilters() {
        List<String> brands = productRepository.findDistinctBrands();
        List<Object[]> colors = productRepository.findDistinctColors();
        List<String> materials = productRepository.findDistinctMaterials();

        Map<String, Object> filters = new HashMap<>();
        filters.put("brands", brands);
        filters.put("colors", colors.stream()
                .map(c -> {
                    Map<String, String> color = new HashMap<>();
                    color.put("colorName", (String) c[0]);
                    color.put("colorHex", (String) c[1]);
                    return color;
                })
                .toList());
        filters.put("materials", materials);

        return filters;
    }

    public List<ProductResponse> getMostSold(int limit) {
        return productRepository.findMostSold(limit).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> getSimilarProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<Product> similar = productRepository.findSimilarProducts(
                product.getCategory().getId(), productId, limit);

        if (similar.size() < limit
                && nonNull(product.getCategory())
                && nonNull(product.getCategory().getParent())) {
            getSimilarProducts(productId, limit, similar, product);
        }

        if (similar.size() < limit) {
            getMostSoldProducts(productId, limit, similar);
        }

        return similar.stream().map(this::toResponse).toList();
    }

    private void getMostSoldProducts(Long productId, int limit, List<Product> similar) {
        List<Long> excludeIds = new ArrayList<>(similar.stream().map(Product::getId).toList());
        excludeIds.add(productId);
        List<Product> mostSold = productRepository.findMostSoldExcluding(excludeIds, limit - similar.size());
        similar.addAll(mostSold);
    }

    private void getSimilarProducts(Long productId, int limit, List<Product> similar, Product product) {
        List<Long> existingIds = new ArrayList<>(similar.stream().map(Product::getId).toList());
        existingIds.add(productId);
        List<Product> parentCategoryProducts = productRepository.findSimilarFromParentCategory(
                product.getCategory().getParent().getId(), existingIds, limit - similar.size());
        similar.addAll(parentCategoryProducts);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setSku(request.getSku());

        if (nonNull(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        product.setColorName(request.getColorName());
        product.setColorHex(request.getColorHex());
        product.setVideoUrl(request.getVideoUrl());
        product.setBrand(request.getBrand());
        product.setMaterial(request.getMaterial());

        product.setDiscountPercentage(nonNull(request.getDiscountPercentage())
                ? request.getDiscountPercentage()
                : ZERO);
        calculateDiscountPrice(product);

        Product saved = productRepository.save(product);
        linkColorVariants(saved);
        return toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ProductResponse toggleActive(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (validateCateogryActive(product)) {
            throw new RuntimeException("Cannot activate product because its category is not active");
        }

        product.setActive(!product.isActive());
        return toResponse(productRepository.save(product));
    }

    private static boolean validateCateogryActive(Product product) {
        return !product.isActive()
                && nonNull(product.getCategory())
                && !product.getCategory().isActive();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productRepository.delete(product);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ProductImageResponse addImage(Long productId, ProductImageRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (productImageRepository.countByProductIdAndIsPrimaryFalse(productId) >= 5) {
            throw new RuntimeException("Maximum 5 images per product");
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.getImageUrl())
                .displayOrder(nonNull(request.getDisplayOrder()) ? request.getDisplayOrder() : 0)
                .isPrimary(request.isPrimary())
                .build();

        ProductImage saved = productImageRepository.save(image);
        return ProductImageResponse.builder()
                .id(saved.getId())
                .imageUrl(saved.getImageUrl())
                .displayOrder(saved.getDisplayOrder())
                .isPrimary(saved.isPrimary())
                .build();
    }

    public ProductColorVariantResponse addColorVariant(Long productId, ProductColorVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Product variant = productRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant product not found"));

        if (colorVariantRepository.existsByProductIdAndVariantId(productId, request.getVariantId())) {
            throw new DuplicateResourceException("Variant already exists");
        }

        ProductColorVariant colorVariant = ProductColorVariant.builder()
                .product(product)
                .variant(variant)
                .build();

        colorVariantRepository.save(colorVariant);

        return ProductColorVariantResponse.builder()
                .variantId(variant.getId())
                .imageUrl(variant.getImageUrl())
                .build();
    }

    private void linkColorVariants(Product product) {
        if (isNull(product.getColorHex()) || product.getColorHex().isBlank()) return;

        String baseSku = product.getSku();
        List<Product> variants = productRepository.findBySkuContainingAndIdNot(baseSku, product.getId());

        for (Product variant : variants) {
            if (nonNull(variant.getColorHex()) && !variant.getColorHex().isBlank()) {
                if (!colorVariantRepository.existsByProductIdAndVariantId(product.getId(), variant.getId())) {
                    colorVariantRepository.save(ProductColorVariant.builder()
                            .product(product)
                            .variant(variant)
                            .build());
                }
                if (!colorVariantRepository.existsByProductIdAndVariantId(variant.getId(), product.getId())) {
                    colorVariantRepository.save(ProductColorVariant.builder()
                            .product(variant)
                            .variant(product)
                            .build());
                }
            }
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public void deleteImage(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        productImageRepository.delete(image);
    }

    public void deleteColorVariant(Long variantId) {
        ProductColorVariant variant = colorVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        colorVariantRepository.delete(variant);
    }

    private void calculateDiscountPrice(Product product) {
        if (nonNull(product.getDiscountPercentage()) && product.getDiscountPercentage().compareTo(ZERO) > 0) {
            BigDecimal discount = product.getPrice()
                    .multiply(product.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), 2, HALF_UP);
            product.setDiscountPrice(product.getPrice().subtract(discount));
        } else {
            product.setDiscountPrice(null);
        }
    }

    private ProductResponse toResponse(Product product) {
        List<ProductImageResponse> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream()
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .displayOrder(img.getDisplayOrder())
                        .isPrimary(img.isPrimary())
                        .build())
                .toList();

        List<ProductColorVariantResponse> colorVariants = colorVariantRepository
                .findByProductId(product.getId())
                .stream()
                .map(cv -> ProductColorVariantResponse.builder()
                        .variantId(cv.getVariant().getId())
                        .imageUrl(cv.getVariant().getImageUrl())
                        .build())
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sku(product.getSku())
                .imageUrl(product.getImageUrl())
                .videoUrl(product.getVideoUrl())
                .active(product.isActive())
                .categoryId(nonNull(product.getCategory()) ? product.getCategory().getId() : null)
                .categoryName(nonNull(product.getCategory()) ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .images(images)
                .colorName(product.getColorName())
                .colorHex(product.getColorHex())
                .colorVariants(colorVariants)
                .brand(product.getBrand())
                .averageRating(product.getAverageRating())
                .ratingCount(product.getRatingCount())
                .discountPercentage(product.getDiscountPercentage())
                .discountPrice(product.getDiscountPrice())
                .material(product.getMaterial())
                .salesCount(product.getSalesCount())
                .build();
    }
}
