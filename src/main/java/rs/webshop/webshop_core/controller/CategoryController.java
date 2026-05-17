package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.category.CategoryRequest;
import rs.webshop.webshop_core.dto.category.CategoryResponse;
import rs.webshop.webshop_core.service.CategoryService;

import java.util.List;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(CREATED).body(categoryService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "name") Pageable pageable) {
        if ((nonNull(search) && !search.isBlank())
                || nonNull(active)) {
            return ResponseEntity.ok(categoryService.search(search, active, pageable));
        }
        return ResponseEntity.ok(categoryService.getAll(pageable));
    }

    @GetMapping("/roots")
    public ResponseEntity<List<CategoryResponse>> getRoots() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CategoryResponse>> getActive() {
        return ResponseEntity.ok(categoryService.getActive());
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CategoryResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.toggleActive(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}