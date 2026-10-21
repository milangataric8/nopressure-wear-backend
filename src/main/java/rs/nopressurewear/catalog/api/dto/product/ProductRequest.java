package rs.nopressurewear.catalog.api.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.catalog.api.Gender;
import rs.nopressurewear.storefront.api.dto.store.ProductStoreRequest;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "validation.name.required")
    @Size(max = 255, message = "validation.name.maxLength")
    private String name;

    private String description;

    @NotNull(message = "validation.price.required")
    @DecimalMin(value = "0.01", message = "validation.price.invalid")
    private BigDecimal price;

    // Stock is per-variant only (see ProductVariantRequest); product-level stock no longer exists.

    @NotBlank(message = "validation.sku.required")
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

    /**
     * Products to link as color variants of this one, on create only.
     * {@code null} = not provided, leave relations untouched; empty list = no relations.
     */
    private List<Long> colorVariantIds;

    /**
     * Store availability rows to create for this product, on create only.
     * {@code null} = not provided, leave relations untouched; empty list = no relations.
     */
    @Valid
    private List<ProductStoreRequest> stores;
}