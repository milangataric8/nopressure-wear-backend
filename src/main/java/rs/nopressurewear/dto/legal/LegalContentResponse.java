package rs.nopressurewear.dto.legal;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LegalContentResponse {
    private String type;
    private String language;
    private String content;
    private LocalDateTime lastUpdated;
}
