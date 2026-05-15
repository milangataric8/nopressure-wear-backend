package rs.webshop.webshop_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.webshop.webshop_core.dto.banner.BannerRequest;
import rs.webshop.webshop_core.dto.banner.BannerResponse;
import rs.webshop.webshop_core.service.BannerService;

import java.util.List;

import static java.util.Objects.nonNull;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/active")
    public ResponseEntity<List<BannerResponse>> getActive() {
        return ResponseEntity.ok(bannerService.getActive());
    }

    @GetMapping
    public ResponseEntity<Page<BannerResponse>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "title") Pageable pageable) {
        if (nonNull(search) && !search.isBlank()) {
            return ResponseEntity.ok(bannerService.search(search, pageable));
        }
        return ResponseEntity.ok(bannerService.getAll(pageable));
    }

    @PostMapping
    public ResponseEntity<BannerResponse> create(@Valid @RequestBody BannerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BannerResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<BannerResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(bannerService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}