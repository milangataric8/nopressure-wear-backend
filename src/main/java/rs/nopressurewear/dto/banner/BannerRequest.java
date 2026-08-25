package rs.nopressurewear.dto.banner;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    private String mobileMediaUrl;
    private MediaType mobileMediaType;

    private String buttonText;
    private String buttonLink;
    private Integer displayOrder;
    private Boolean displayTitle;

    @Min(value = 0, message = "Duration cannot be negative")
    @Max(value = 60, message = "Duration cannot exceed 60 seconds")
    private Integer displayDuration;
}