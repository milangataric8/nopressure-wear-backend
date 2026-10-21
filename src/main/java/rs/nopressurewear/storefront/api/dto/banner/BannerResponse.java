package rs.nopressurewear.storefront.api.dto.banner;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.storefront.api.MediaType;

@Getter
@Setter
@Builder
public class BannerResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String mediaUrl;
    private MediaType mediaType;
    private String mobileMediaUrl;
    private MediaType mobileMediaType;
    private String buttonText;
    private String buttonLink;
    private Integer displayOrder;
    private Boolean active;
    private Boolean displayTitle;
    private Integer displayDuration;
}
