package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.popup.PopupRequest;
import rs.nopressurewear.dto.popup.PopupResponse;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Popup;
import rs.nopressurewear.repository.PopupRepository;
import rs.nopressurewear.util.HtmlSanitizer;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class PopupService {

    private final PopupRepository popupRepository;

    public PopupResponse getActive() {
        Popup popup = popupRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElse(null);
        return nonNull(popup) ? toResponse(popup) : null;
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
                .content(HtmlSanitizer.sanitize(request.getContent()))
                .mediaUrl(request.getMediaUrl())
                .mediaType(nonNull(request.getMediaType()) ? request.getMediaType() : "IMAGE")
                .buttonText(request.getButtonText())
                .buttonLink(request.getButtonLink())
                .backgroundColor(nonNull(request.getBackgroundColor()) ? request.getBackgroundColor() : "#FFFFFF")
                .textColor(nonNull(request.getTextColor()) ? request.getTextColor() : "#000000")
                .active(true)
                .showOnce(nonNull(request.getShowOnce()) && request.getShowOnce())
                .displayDuration(nonNull(request.getDisplayDuration()) ? request.getDisplayDuration() : 0)
                .build();
        return toResponse(popupRepository.save(popup));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PopupResponse update(Long id, PopupRequest request) {
        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Popup not found"));

        popup.setTitle(request.getTitle());
        popup.setSubtitle(request.getSubtitle());
        popup.setContent(HtmlSanitizer.sanitize(request.getContent()));
        popup.setMediaUrl(request.getMediaUrl());
        popup.setMediaType(request.getMediaType());
        popup.setButtonText(request.getButtonText());
        popup.setButtonLink(request.getButtonLink());
        if (nonNull(request.getBackgroundColor())) popup.setBackgroundColor(request.getBackgroundColor());
        if (nonNull(request.getTextColor())) popup.setTextColor(request.getTextColor());
        if (nonNull(request.getShowOnce())) popup.setShowOnce(request.getShowOnce());
        popup.setDisplayDuration(nonNull(request.getDisplayDuration()) ? request.getDisplayDuration() : 0);

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
                .displayDuration(nonNull(popup.getDisplayDuration()) ? popup.getDisplayDuration() : 0)
                .createdAt(popup.getCreatedAt())
                .build();
    }
}