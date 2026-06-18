package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.model.Popup;

import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {
    Optional<Popup> findFirstByActiveTrueOrderByCreatedAtDesc();
}