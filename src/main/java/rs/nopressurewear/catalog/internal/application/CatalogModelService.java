package rs.nopressurewear.catalog.internal.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.catalog.api.ProductSize;
import rs.nopressurewear.catalog.api.dto.product.CatalogModelResponse;
import rs.nopressurewear.catalog.api.dto.product.ProductResponse;
import rs.nopressurewear.catalog.internal.persistence.CatalogModelRepository;
import rs.nopressurewear.catalog.internal.persistence.ProductRepository;
import rs.nopressurewear.catalog.internal.persistence.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogModelService {
    private final CatalogModelRepository repository;
    private final ProductRepository products;
    private final ProductVariantRepository variants;

    @Transactional(readOnly = true)
    public Page<CatalogModelResponse> search(Long categoryId, String search, BigDecimal minPrice, BigDecimal maxPrice,
                                            String brand, String colorName, String material, String gender,
                                            List<ProductSize> sizes, Pageable pageable) {
        var result = repository.findModels(categoryId, search, minPrice, maxPrice, brand, colorName, material, gender, sizes, pageable);
        if (result.isEmpty()) return new PageImpl<>(List.of(), pageable, result.getTotalElements());
        var ids = result.getContent().stream().map(CatalogModelRepository.Member::productId).toList();
        Map<Long, Integer> stock = new HashMap<>();
        for (Object[] row : variants.sumStockByProductIds(ids)) {
            stock.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        Map<Long, ProductResponse> options = new HashMap<>();
        for (var product : products.findCatalogOptions(ids)) {
            int quantity = stock.getOrDefault(product.getId(), 0);
            options.put(product.getId(), ProductResponse.builder()
                    .id(product.getId()).name(product.getName()).imageUrl(product.getImageUrl())
                    .colorName(product.getColorName()).colorHex(product.getColorHex())
                    .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
                    .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
                    .gender(product.getGender()).price(product.getPrice())
                    .discountPrice(product.getDiscountPrice()).discountPercentage(product.getDiscountPercentage())
                    .averageRating(product.getAverageRating()).ratingCount(product.getRatingCount())
                    .stockQuantity(quantity).totalStock(quantity).build());
        }
        Map<Long, List<ProductResponse>> groups = new LinkedHashMap<>();
        for (var member : result.getContent()) {
            ProductResponse option = options.get(member.productId());
            if (option == null) continue;
            groups.computeIfAbsent(member.modelId(), ignored -> new ArrayList<>()).add(option);
        }
        var content = groups.entrySet().stream().map(e -> new CatalogModelResponse(e.getKey(), e.getValue())).toList();
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }
}
