package rs.nopressurewear.identity.api.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendRequest {
    private String email;
}
