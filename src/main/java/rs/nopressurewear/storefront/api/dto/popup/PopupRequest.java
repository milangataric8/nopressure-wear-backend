package rs.nopressurewear.storefront.api.dto.popup;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopupRequest {

    @NotBlank(message = "validation.title.required")
    private String title;

    private String subtitle;
    private String content;
    private String mediaUrl;
    private String mediaType;
    private String buttonText;
    private String buttonLink;
    private String backgroundColor;
    private String textColor;
    private Boolean showOnce;

    @Min(value = 0, message = "validation.displayDuration.invalid")
    @Max(value = 120, message = "validation.displayDuration.invalid")
    private Integer displayDuration;
}