package rs.nopressurewear.storefront.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.storefront.internal.domain.Popup;

import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

    Optional<Popup> findFirstByActiveTrueOrderByCreatedAtDesc();
}