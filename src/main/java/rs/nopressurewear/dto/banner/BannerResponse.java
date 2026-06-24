package rs.nopressurewear.dto.banner;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.constants.MediaType;

@Getter
@Setter
@Builder
public class BannerResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String mediaUrl;
    private MediaType mediaType;
    private String buttonText;
    private String buttonLink;
    private Integer displayOrder;
    private Boolean active;
    private Boolean displayTitle;
}
