package rs.nopressurewear.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotNull(message = "validation.required")
    private Long userId;

    @NotBlank(message = "validation.streetRequired")
    private String street;

    @NotBlank(message = "validation.cityRequired")
    private String city;

    @NotBlank(message = "validation.postalCodeRequired")
    private String postalCode;

    @NotBlank(message = "validation.countryRequired")
    private String country;
}