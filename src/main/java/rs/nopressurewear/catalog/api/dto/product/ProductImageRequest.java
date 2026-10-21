package rs.nopressurewear.catalog.api.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageRequest {
    @NotBlank
    private String imageUrl;
    private Integer displayOrder;
    private boolean isPrimary;
}