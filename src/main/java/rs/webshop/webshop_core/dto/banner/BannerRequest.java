package rs.webshop.webshop_core.dto.banner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rs.webshop.webshop_core.constants.MediaType;

@Getter
@Setter
public class BannerRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;
    private String mediaUrl;

    @NotNull(message = "Media type is required")
    private MediaType mediaType;

    private String buttonText;
    private String buttonLink;
    private Integer displayOrder;
}