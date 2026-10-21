package rs.nopressurewear.catalog.api.dto.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FilterConfigResponse {
    private Long id;
    private String fieldName;
    private String displayName;
    private String filterType;
    private Boolean visible;
    private Integer displayOrder;
}