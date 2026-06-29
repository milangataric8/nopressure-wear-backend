package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.category.CategoryRequest;
import rs.nopressurewear.dto.category.CategoryResponse;
import rs.nopressurewear.exception.DuplicateResourceException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Category;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.repository.CategoryRepository;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductService productService;

    @CacheEvict(value = "categories", allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
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

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    public Page<CategoryResponse> getAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::toResponse);
    }

    @Cacheable("categories")
    public List<CategoryResponse> getActive() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<CategoryResponse> search(String search, Boolean active, Pageable pageable) {
        return categoryRepository.findByFilters(search, active, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = "categories", allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public CategoryResponse toggleActive(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setActive(!category.isActive());
        categoryRepository.save(category);

        toggleActiveSubcategories(id);
        toggleActiveCategoryProducts(id);

        return toResponse(category);
    }

    private void toggleActiveSubcategories(Long parentCategoryId) {
        List<Category> subcategories = categoryRepository.findAllByParentId(parentCategoryId);
        for (Category subcategory : subcategories) {
            subcategory.setActive(!subcategory.isActive());
            categoryRepository.save(subcategory);

            toggleActiveCategoryProducts(subcategory.getId());
        }
    }

    private void toggleActiveCategoryProducts(Long id) {
        List<Product> products = productService.getByCategory(id);
        for (Product product : products) {
            productService.toggleActive(product.getId());
        }
    }

    @CacheEvict(value = "categories", allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
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

    @CacheEvict(value = "categories", allEntries = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
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
                .active(category.isActive())
                .build();
    }
}
