package rs.nopressurewear.dto.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.constants.Gender;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "validation.nameRequired")
    @Size(max = 255, message = "validation.nameTooLong")
    private String name;

    private String description;

    @NotNull(message = "validation.priceRequired")
    @DecimalMin(value = "0.01", message = "validation.priceInvalid")
    private BigDecimal price;

    @Min(value = 0, message = "validation.stockInvalid")
    private Integer stockQuantity;

    @NotBlank(message = "validation.skuRequired")
    private String sku;

    private String imageUrl;

    private Long categoryId;

    private String colorName;

    private String colorHex;

    private String videoUrl;

    private String brand;

    private BigDecimal discountPercentage;

    private String material;

    private List<ProductVariantRequest> variants;

    private Gender gender;
}