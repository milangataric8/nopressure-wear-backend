package rs.webshop.webshop_core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.webshop.webshop_core.model.StoreSettings;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
    Optional<StoreSettings> findByKey(String key);
    List<StoreSettings> findAll();
}