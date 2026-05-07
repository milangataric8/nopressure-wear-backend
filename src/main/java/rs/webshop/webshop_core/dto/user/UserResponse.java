package rs.webshop.webshop_core.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.webshop.webshop_core.constants.Role;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}