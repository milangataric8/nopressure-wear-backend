package rs.nopressure.wear.webshop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressure.wear.webshop.dto.category.CategoryRequest;
import rs.nopressure.wear.webshop.dto.category.CategoryResponse;
import rs.nopressure.wear.webshop.exception.DuplicateResourceException;
import rs.nopressure.wear.webshop.exception.ResourceNotFoundException;
import rs.nopressure.wear.webshop.model.Category;
import rs.nopressure.wear.webshop.repository.CategoryRepository;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        if (nonNull(request.getParentId())) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
        }

        return toResponse(categoryRepository.save(category));
    }

    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        if (nonNull(request.getParentId())) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return toResponse(categoryRepository.save(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(nonNull(category.getParent()) ? category.getParent().getId() : null)
                .parentName(nonNull(category.getParent()) ? category.getParent().getName() : null)
                .subcategories(
                        nonNull(category.getSubcategories())
                                ? category.getSubcategories().stream().map(this::toResponse).toList()
                                : List.of()
                )
                .build();
    }
}
