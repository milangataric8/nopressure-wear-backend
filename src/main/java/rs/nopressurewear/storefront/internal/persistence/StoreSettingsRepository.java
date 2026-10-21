package rs.nopressurewear.storefront.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.nopressurewear.storefront.internal.domain.StoreSettings;

import java.util.Optional;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {

    Optional<StoreSettings> findByKey(String key);
}