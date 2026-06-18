package rs.nopressurewear.dto.popup;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopupRequest {

    @NotBlank(message = "Title is required")
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
}