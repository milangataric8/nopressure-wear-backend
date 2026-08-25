package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.dto.legal.LegalContentResponse;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.LegalContent;
import rs.nopressurewear.repository.LegalContentRepository;
import rs.nopressurewear.util.HtmlSanitizer;

@Service
@RequiredArgsConstructor
public class LegalContentService {

    private final LegalContentRepository repository;

    public LegalContentResponse get(String type, String lang) {
        LegalContent lc = repository.findByTypeIgnoreCaseAndLanguageIgnoreCase(type, lang)
                .orElseThrow(() -> new ResourceNotFoundException("Legal content not found: " + type + "/" + lang));
        return toResponse(lc);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public LegalContentResponse update(String type, String lang, String content) {
        LegalContent lc = repository.findByTypeIgnoreCaseAndLanguageIgnoreCase(type, lang)
                .orElseThrow(() -> new ResourceNotFoundException("Legal content not found: " + type + "/" + lang));
        lc.setContent(HtmlSanitizer.sanitize(content));
        return toResponse(repository.save(lc));
    }

    private LegalContentResponse toResponse(LegalContent lc) {
        return LegalContentResponse.builder()
                .type(lc.getType())
                .language(lc.getLanguage())
                .content(lc.getContent())
                .lastUpdated(lc.getLastUpdated())
                .build();
    }
}
