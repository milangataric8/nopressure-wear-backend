package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.settings.StoreSettingsRequest;
import rs.webshop.webshop_core.dto.settings.StoreSettingsResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.StoreSettings;
import rs.webshop.webshop_core.repository.StoreSettingsRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;

    public List<StoreSettingsResponse> getAll() {
        return storeSettingsRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Map<String, String> getAllAsMap() {
        return storeSettingsRepository.findAll()
                .stream()
                .collect(Collectors.toMap(StoreSettings::getKey, StoreSettings::getValue));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public StoreSettingsResponse update(Long id, StoreSettingsRequest request) {
        StoreSettings setting = storeSettingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found"));
        setting.setValue(request.getValue());
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