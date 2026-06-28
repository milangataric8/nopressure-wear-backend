package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.LegalContent;

import java.util.Optional;

@Repository
public interface LegalContentRepository extends JpaRepository<LegalContent, Long> {

    Optional<LegalContent> findByTypeIgnoreCaseAndLanguageIgnoreCase(String type, String language);
}
