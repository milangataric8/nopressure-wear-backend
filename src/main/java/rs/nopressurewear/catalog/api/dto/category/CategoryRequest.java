package rs.nopressurewear.catalog.api.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "validation.name.required")
    @Size(max = 100, message = "validation.name.maxLength")
    private String name;

    @Size(max = 5000, message = "validation.description.maxLength")
    private String description;

    private Long parentId;
}
