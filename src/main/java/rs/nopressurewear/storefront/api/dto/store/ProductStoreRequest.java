package rs.nopressurewear.storefront.api.dto.store;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStoreRequest {
    @NotNull
    private Long storeLocationId;
    private Boolean inStock;
}