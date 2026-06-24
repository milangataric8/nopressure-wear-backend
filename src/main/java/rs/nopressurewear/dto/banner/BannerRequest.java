package rs.nopressurewear.dto.banner;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.constants.MediaType;

@Getter
@Setter
public class BannerRequest {

    private String title;

    private String subtitle;
    private String mediaUrl;

    @NotNull(message = "Media type is required")
    private MediaType mediaType;

    private String buttonText;
    private String buttonLink;
    private Integer displayOrder;
    private Boolean displayTitle;
}