package rs.nopressurewear.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotNull(message = "validation.userId.required")
    private Long userId;

    @NotBlank(message = "validation.street.required")
    private String street;

    @NotBlank(message = "validation.city.required")
    private String city;

    @NotBlank(message = "validation.postalCode.required")
    private String postalCode;

    @NotBlank(message = "validation.country.required")
    private String country;

    /** Whether to make this the user's main address. No validation — {@code false} is the valid default. */
    private boolean main;
}