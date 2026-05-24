package rs.webshop.webshop_core.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductAttributeRequest {
    @NotBlank
    private String key;
    @NotBlank
    private String value;
}