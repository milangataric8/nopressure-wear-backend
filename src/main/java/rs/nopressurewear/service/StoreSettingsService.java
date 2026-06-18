package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.settings.StoreSettingsRequest;
import rs.nopressurewear.dto.settings.StoreSettingsResponse;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.StoreSettings;
import rs.nopressurewear.repository.StoreSettingsRepository;

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
                .collect(Collectors.toMap(
                        StoreSettings::getKey,
                        s -> s.getValue() != null ? s.getValue() : ""
                ));
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