package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.product.*;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Category;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.ProductImage;
import rs.nopressurewear.repository.*;
import rs.nopressurewear.util.HtmlSanitizer;

import rs.nopressurewear.model.ProductVariant;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductColorVariantRepository colorVariantRepository;
    private final ProductVariantRepository productVariantRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Transactional
    public ProductResponse create(ProductRequest request) {

        Product product = Product.builder()
                .name(request.getName())
                .description(HtmlSanitizer.sanitize(request.getDescription()))
                .price(request.getPrice())
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

        List<ProductVariant> variants = buildVariants(saved, request.getVariants());
        productVariantRepository.saveAll(variants);
        int total = variants.stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum();
        saved.setStockQuantity(total);
        productRepository.save(saved);

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
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(HtmlSanitizer.sanitize(request.getDescription()));
        product.setPrice(request.getPrice());
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

        productVariantRepository.deleteByProductId(id);
        List<ProductVariant> variants = buildVariants(product, request.getVariants());
        productVariantRepository.saveAll(variants);
        int total = variants.stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum();
        product.setStockQuantity(total);

        Product saved = productRepository.save(product);
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

        productImageRepository.deleteByProductId(id);
        colorVariantRepository.deleteByProductId(id);
        colorVariantRepository.deleteByVariantId(id);
        
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

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public void deleteImage(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        productImageRepository.delete(image);
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
                .map(cv -> {
                    Product v = cv.getVariant();
                    return ProductColorVariantResponse.builder()
                            .productId(v.getId())
                            .name(v.getName())
                            .colorName(v.getColorName())
                            .colorHex(v.getColorHex())
                            .imageUrl(v.getImageUrl())
                            .sku(v.getSku())
                            .isCurrent(v.getId().equals(product.getId()))
                            .build();
                })
                .toList();

        List<ProductVariantResponse> variants = productVariantRepository.findByProductId(product.getId())
                .stream()
                .sorted(Comparator.comparingInt(v -> v.getSize().ordinal()))
                .map(v -> ProductVariantResponse.builder()
                        .id(v.getId())
                        .size(v.getSize())
                        .stockQuantity(v.getStockQuantity())
                        .sku(v.getSku())
                        .inStock(v.getStockQuantity() != null && v.getStockQuantity() > 0)
                        .build())
                .toList();

        int totalStock = variants.stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                .sum();

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
                .variants(variants)
                .totalStock(totalStock)
                .build();
    }

    private List<ProductVariant> buildVariants(Product product, List<ProductVariantRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        return requested.stream()
                .filter(v -> v.getSize() != null)
                .map(v -> ProductVariant.builder()
                        .product(product)
                        .size(v.getSize())
                        .stockQuantity(v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                        .sku(v.getSku())
                        .build())
                .toList();
    }
}
