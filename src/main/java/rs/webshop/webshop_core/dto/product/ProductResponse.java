package rs.webshop.webshop_core.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String sku;
    private String imageUrl;
    private String videoUrl;
    private List<ProductImageResponse> images;
    private String colorName;
    private String colorHex;
    private List<ProductColorVariantResponse> colorVariants;
    private Boolean active;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String brand;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private BigDecimal discountPercentage;
    private BigDecimal discountPrice;
}
