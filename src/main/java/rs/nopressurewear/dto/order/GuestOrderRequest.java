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

    @NotBlank(message = "validation.customerFullName.required")
    private String customerFullName;

    @NotBlank(message = "validation.customerEmail.required")
    @Email(message = "validation.customerEmail.invalid")
    private String customerEmail;

    @NotBlank(message = "validation.customerPhone.required")
    private String customerPhone;

    @NotBlank(message = "validation.street.required")
    private String street;

    @NotBlank(message = "validation.city.required")
    private String city;

    private String postalCode;

    @NotBlank(message = "validation.country.required")
    private String country;

    @NotEmpty(message = "validation.items.required")
    @Valid
    private List<GuestOrderItem> items;

    private String couponCode;
    private String paymentMethod;

    @Getter
    @Setter
    public static class GuestOrderItem {
        @NotNull(message = "validation.productId.required")
        private Long productId;
        @NotNull(message = "validation.quantity.required")
        @Min(value = 1, message = "validation.quantity.min")
        private Integer quantity;
        private ProductSize size;
    }
}