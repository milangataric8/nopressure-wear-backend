package rs.nopressurewear.dto.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StoreSettingsResponse {
    private Long id;
    private String key;
    private String value;
    private String label;
}