package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.filter.FilterConfigRequest;
import rs.webshop.webshop_core.dto.filter.FilterConfigResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.FilterConfig;
import rs.webshop.webshop_core.repository.FilterConfigRepository;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class FilterConfigService {

    private final FilterConfigRepository filterConfigRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public FilterConfigResponse create(String fieldName, String displayName, String filterType) {
        FilterConfig config = FilterConfig.builder()
                .fieldName(fieldName)
                .displayName(displayName)
                .filterType(filterType)
                .visible(true)
                .displayOrder(filterConfigRepository.findAllByOrderByDisplayOrderAsc().size() + 1)
                .build();
        return toResponse(filterConfigRepository.save(config));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        FilterConfig config = filterConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filter not found"));
        filterConfigRepository.delete(config);
    }

    public List<FilterConfigResponse> getVisible() {
        return filterConfigRepository.findByVisibleTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<FilterConfigResponse> getAll() {
        return filterConfigRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public FilterConfigResponse update(Long id, FilterConfigRequest request) {
        FilterConfig config = filterConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filter config not found"));

        if (nonNull(request.getVisible())) config.setVisible(request.getVisible());
        if (nonNull(request.getDisplayOrder())) config.setDisplayOrder(request.getDisplayOrder());

        return toResponse(filterConfigRepository.save(config));
    }

    private FilterConfigResponse toResponse(FilterConfig config) {
        return FilterConfigResponse.builder()
                .id(config.getId())
                .fieldName(config.getFieldName())
                .displayName(config.getDisplayName())
                .filterType(config.getFilterType())
                .visible(config.isVisible())
                .displayOrder(config.getDisplayOrder())
                .build();
    }
}