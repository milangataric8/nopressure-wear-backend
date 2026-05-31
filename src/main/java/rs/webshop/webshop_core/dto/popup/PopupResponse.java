package rs.webshop.webshop_core.dto.popup;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PopupResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String content;
    private String mediaUrl;
    private String mediaType;
    private String buttonText;
    private String buttonLink;
    private String backgroundColor;
    private String textColor;
    private Boolean active;
    private Boolean showOnce;
    private LocalDateTime createdAt;
}