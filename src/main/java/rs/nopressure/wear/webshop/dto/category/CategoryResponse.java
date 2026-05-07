package rs.nopressure.wear.webshop.dto.category;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> subcategories;
}
