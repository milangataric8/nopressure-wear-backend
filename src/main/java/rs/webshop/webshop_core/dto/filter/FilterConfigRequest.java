package rs.webshop.webshop_core.dto.filter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterConfigRequest {
    private Boolean visible;
    private Integer displayOrder;
}