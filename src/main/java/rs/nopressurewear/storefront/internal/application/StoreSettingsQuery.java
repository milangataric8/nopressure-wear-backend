package rs.nopressurewear.storefront.internal.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.storefront.api.SettingsQuery;
import rs.nopressurewear.storefront.internal.domain.StoreSettings;
import rs.nopressurewear.storefront.internal.persistence.StoreSettingsRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StoreSettingsQuery implements SettingsQuery {
    private final StoreSettingsRepository repository;

    @Override
    public Optional<String> findValue(String key) {
        return repository.findByKey(key).map(StoreSettings::getValue);
    }
}
