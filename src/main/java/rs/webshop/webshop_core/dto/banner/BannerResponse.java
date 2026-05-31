package rs.webshop.webshop_core.dto.banner;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.webshop.webshop_core.constants.MediaType;

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
}
