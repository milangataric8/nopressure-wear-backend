package rs.nopressurewear.dto.review;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ReviewResponse {
    private Long id;
    private Integer rating;
    private String comment;
    private String userName;
    private LocalDateTime createdAt;
}