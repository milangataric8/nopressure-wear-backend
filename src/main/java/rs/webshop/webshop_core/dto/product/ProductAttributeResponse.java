package rs.webshop.webshop_core.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductAttributeResponse {
    private Long id;
    private String key;
    private String value;
}