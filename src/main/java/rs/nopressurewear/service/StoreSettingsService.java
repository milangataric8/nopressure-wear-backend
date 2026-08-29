package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.settings.StoreSettingsRequest;
import rs.nopressurewear.dto.settings.StoreSettingsResponse;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.StoreSettings;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.util.HtmlSanitizer;
import rs.nopressurewear.util.HtmlTextSanitizer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;

    /**
     * Keys whose value is rich text entered through the admin WYSIWYG editor. They get
     * XSS-sanitized and whitespace-normalized on save. Add a key here when a new
     * rich-text setting is introduced.
     */
    private static final Set<String> RICH_TEXT_KEYS = Set.of("store_tagline");

    public List<StoreSettingsResponse> getAll() {
        return storeSettingsRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable("settings")
    public Map<String, String> getAllAsMap() {
        return storeSettingsRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        StoreSettings::getKey,
                        s -> nonNull(s.getValue()) ? s.getValue() : ""
                ));
    }

    @CacheEvict(value = "settings", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreSettingsResponse update(Long id, StoreSettingsRequest request) {
        StoreSettings setting = storeSettingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found"));
        String value = RICH_TEXT_KEYS.contains(setting.getKey())
                ? HtmlTextSanitizer.normalizeWhitespace(HtmlSanitizer.sanitize(request.getValue()))
                : request.getValue();
        setting.setValue(value);
        return toResponse(storeSettingsRepository.save(setting));
    }

    private StoreSettingsResponse toResponse(StoreSettings setting) {
        return StoreSettingsResponse.builder()
                .id(setting.getId())
                .key(setting.getKey())
                .value(setting.getValue())
                .label(setting.getLabel())
                .build();
    }
}