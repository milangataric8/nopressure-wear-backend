package rs.nopressurewear.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {
    private Long id;
    private String token;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}