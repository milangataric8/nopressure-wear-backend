package rs.nopressurewear.storefront.internal.web;

import rs.nopressurewear.platform.util.LocaleResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.storefront.api.dto.legal.LegalContentRequest;
import rs.nopressurewear.storefront.api.dto.legal.LegalContentResponse;
import rs.nopressurewear.storefront.internal.application.LegalContentService;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
public class LegalContentController {

    private final LegalContentService service;

    @GetMapping("/{type}")
    public ResponseEntity<LegalContentResponse> get(
            @PathVariable String type,
            @RequestParam(defaultValue = "sr") String lang) {
        return ResponseEntity.ok(service.get(type, LocaleResolver.normalize(lang).getLanguage()));
    }

    @PutMapping("/{type}")
    public ResponseEntity<LegalContentResponse> update(
            @PathVariable String type,
            @RequestBody LegalContentRequest request) {
        return ResponseEntity.ok(service.update(type, request.getLanguage(), request.getContent()));
    }
}
