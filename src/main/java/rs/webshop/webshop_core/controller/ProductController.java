package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.product.ProductRequest;
import rs.webshop.webshop_core.dto.product.ProductResponse;
import rs.webshop.webshop_core.service.ProductService;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(CREATED).body(productService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(
            @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<ProductResponse>> getActive(
            @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(productService.getActive(pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(productService.getByCategory(categoryId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deactivate(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(productService.activate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}