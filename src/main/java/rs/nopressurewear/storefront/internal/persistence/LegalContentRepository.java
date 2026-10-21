package rs.nopressurewear.storefront.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.storefront.internal.domain.LegalContent;

import java.util.Optional;

@Repository
public interface LegalContentRepository extends JpaRepository<LegalContent, Long> {

    Optional<LegalContent> findByTypeIgnoreCaseAndLanguageIgnoreCase(String type, String language);
}
