package rs.nopressurewear.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import rs.nopressurewear.constants.ProductSize;

@Getter
@Setter
public class GuestOrderRequest {

    @NotBlank(message = "validation.fullNameRequired")
    private String customerFullName;

    @NotBlank(message = "validation.emailRequired")
    @Email(message = "validation.emailInvalid")
    private String customerEmail;

    @NotBlank(message = "validation.phoneRequired")
    private String customerPhone;

    @NotBlank(message = "validation.streetRequired")
    private String street;

    @NotBlank(message = "validation.cityRequired")
    private String city;

    private String postalCode;

    @NotBlank(message = "validation.countryRequired")
    private String country;

    @NotEmpty(message = "validation.required")
    @Valid
    private List<GuestOrderItem> items;

    private String couponCode;
    private String paymentMethod;

    @Getter
    @Setter
    public static class GuestOrderItem {
        @NotNull(message = "validation.required")
        private Long productId;
        @NotNull(message = "validation.required")
        @Min(value = 1, message = "validation.quantityInvalid")
        private Integer quantity;
        private ProductSize size;
    }
}