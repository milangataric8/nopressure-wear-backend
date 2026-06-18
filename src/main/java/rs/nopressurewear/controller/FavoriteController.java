package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.favorite.FavoriteResponse;
import rs.nopressurewear.service.FavoriteService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<FavoriteResponse>> getUserFavorites(@PathVariable Long userId) {
        return ResponseEntity.ok(favoriteService.getUserFavorites(userId));
    }

    @GetMapping("/{userId}/count")
    public ResponseEntity<Map<String, Integer>> getCount(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("count", favoriteService.getCount(userId)));
    }

    @GetMapping("/{userId}/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> isFavorite(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(Map.of("favorited", favoriteService.isFavorite(userId, productId)));
    }

    @PostMapping("/{userId}/toggle/{productId}")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(favoriteService.toggle(userId, productId));
    }
}