package rs.nopressurewear.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private Integer displayOrder;
    private boolean isPrimary;
}
