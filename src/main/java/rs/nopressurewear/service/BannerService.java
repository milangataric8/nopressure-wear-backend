package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.banner.BannerRequest;
import rs.nopressurewear.dto.banner.BannerResponse;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Banner;
import rs.nopressurewear.repository.BannerRepository;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<BannerResponse> getActive() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<BannerResponse> getAll(Pageable pageable) {
        return bannerRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<BannerResponse> search(String query, Boolean active, Pageable pageable) {
        return bannerRepository.findByFilters(query, active, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public BannerResponse create(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType())
                .buttonText(request.getButtonText())
                .buttonLink(request.getButtonLink())
                .displayOrder(nonNull(request.getDisplayOrder()) ? request.getDisplayOrder() : 0)
                .isActive(true)
                .displayTitle(request.getDisplayTitle() != null ? request.getDisplayTitle() : true)
                .build();
        return toResponse(bannerRepository.save(banner));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));

        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setMediaUrl(request.getMediaUrl());
        banner.setMediaType(request.getMediaType());
        banner.setButtonText(request.getButtonText());
        banner.setButtonLink(request.getButtonLink());
        if (nonNull(request.getDisplayOrder())) {
            banner.setDisplayOrder(request.getDisplayOrder());
        }
        banner.setDisplayTitle(request.getDisplayTitle() != null ? request.getDisplayTitle() : true);
        return toResponse(bannerRepository.save(banner));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public BannerResponse toggleActive(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        banner.setActive(!banner.isActive());
        return toResponse(bannerRepository.save(banner));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public void delete(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        bannerRepository.delete(banner);
    }

    private BannerResponse toResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .mediaUrl(banner.getMediaUrl())
                .mediaType(banner.getMediaType())
                .buttonText(banner.getButtonText())
                .buttonLink(banner.getButtonLink())
                .displayOrder(banner.getDisplayOrder())
                .active(banner.isActive())
                .displayTitle(banner.getDisplayTitle() != null ? banner.getDisplayTitle() : true)
                .build();
    }
}
