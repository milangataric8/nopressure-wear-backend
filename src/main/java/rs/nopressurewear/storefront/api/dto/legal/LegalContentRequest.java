package rs.nopressurewear.storefront.api.dto.legal;

import lombok.Data;

@Data
public class LegalContentRequest {
    private String language;
    private String content;
}
