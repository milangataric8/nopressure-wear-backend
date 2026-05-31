package rs.webshop.webshop_core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.Popup;

import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {
    Optional<Popup> findFirstByActiveTrueOrderByCreatedAtDesc();
}