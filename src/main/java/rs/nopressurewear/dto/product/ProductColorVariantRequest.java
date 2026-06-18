package rs.nopressurewear.dto.product;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductColorVariantRequest {
    @NotNull
    private Long variantId;
}