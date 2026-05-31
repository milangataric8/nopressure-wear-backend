package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.webshop.webshop_core.dto.popup.PopupRequest;
import rs.webshop.webshop_core.dto.popup.PopupResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Popup;
import rs.webshop.webshop_core.repository.PopupRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PopupService {

    private final PopupRepository popupRepository;

    public PopupResponse getActive() {
        Popup popup = popupRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElse(null);
        return popup != null ? toResponse(popup) : null;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<PopupResponse> getAll() {
        return popupRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PopupResponse create(PopupRequest request) {
        Popup popup = Popup.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType() != null ? request.getMediaType() : "IMAGE")
                .buttonText(request.getButtonText())
                .buttonLink(request.getButtonLink())
                .backgroundColor(request.getBackgroundColor() != null ? request.getBackgroundColor() : "#FFFFFF")
                .textColor(request.getTextColor() != null ? request.getTextColor() : "#000000")
                .active(true)
                .showOnce(request.getShowOnce() != null && request.getShowOnce())
                .build();
        return toResponse(popupRepository.save(popup));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PopupResponse update(Long id, PopupRequest request) {
        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Popup not found"));

        popup.setTitle(request.getTitle());
        popup.setSubtitle(request.getSubtitle());
        popup.setContent(request.getContent());
        popup.setMediaUrl(request.getMediaUrl());
        popup.setMediaType(request.getMediaType());
        popup.setButtonText(request.getButtonText());
        popup.setButtonLink(request.getButtonLink());
        if (request.getBackgroundColor() != null) popup.setBackgroundColor(request.getBackgroundColor());
        if (request.getTextColor() != null) popup.setTextColor(request.getTextColor());
        if (request.getShowOnce() != null) popup.setShowOnce(request.getShowOnce());

        return toResponse(popupRepository.save(popup));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PopupResponse toggleActive(Long id) {
        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Popup not found"));
        popup.setActive(!popup.isActive());
        return toResponse(popupRepository.save(popup));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Popup not found"));
        popupRepository.delete(popup);
    }

    private PopupResponse toResponse(Popup popup) {
        return PopupResponse.builder()
                .id(popup.getId())
                .title(popup.getTitle())
                .subtitle(popup.getSubtitle())
                .content(popup.getContent())
                .mediaUrl(popup.getMediaUrl())
                .mediaType(popup.getMediaType())
                .buttonText(popup.getButtonText())
                .buttonLink(popup.getButtonLink())
                .backgroundColor(popup.getBackgroundColor())
                .textColor(popup.getTextColor())
                .active(popup.isActive())
                .showOnce(popup.isShowOnce())
                .createdAt(popup.getCreatedAt())
                .build();
    }
}