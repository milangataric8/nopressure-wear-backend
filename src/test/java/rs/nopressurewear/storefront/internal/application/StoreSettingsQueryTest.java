package rs.nopressurewear.storefront.internal.application;

import org.junit.jupiter.api.Test;
import rs.nopressurewear.storefront.internal.domain.StoreSettings;
import rs.nopressurewear.storefront.internal.persistence.StoreSettingsRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoreSettingsQueryTest {
    private final StoreSettingsRepository repository = mock(StoreSettingsRepository.class);
    private final StoreSettingsQuery query = new StoreSettingsQuery(repository);

    @Test
    void returnsValueWithoutExposingEntity() {
        when(repository.findByKey("delivery_fee")).thenReturn(Optional.of(
                StoreSettings.builder().key("delivery_fee").value("400").build()));

        assertThat(query.findValue("delivery_fee")).contains("400");
    }

    @Test
    void missingOrNullValuesAllowCallerDefaults() {
        when(repository.findByKey("missing")).thenReturn(Optional.empty());
        when(repository.findByKey("unset")).thenReturn(Optional.of(
                StoreSettings.builder().key("unset").build()));

        assertThat(query.findValue("missing")).isEmpty();
        assertThat(query.findValue("unset")).isEmpty();
    }
}
