package rs.nopressurewear.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.nopressurewear.dto.settings.StoreSettingsRequest;
import rs.nopressurewear.model.StoreSettings;
import rs.nopressurewear.repository.StoreSettingsRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreSettingsServiceTest {

    @Mock
    private StoreSettingsRepository storeSettingsRepository;

    @InjectMocks
    private StoreSettingsService storeSettingsService;

    @Test
    void update_ShouldNormalizeWhitespace_ForRichTextTaglineKey() {
        StoreSettings tagline = StoreSettings.builder().id(1L).key("store_tagline").label("Tagline").build();
        when(storeSettingsRepository.findById(1L)).thenReturn(Optional.of(tagline));
        when(storeSettingsRepository.save(any(StoreSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        StoreSettingsRequest request = new StoreSettingsRequest();
        request.setValue("<p>Postoji&nbsp;posebna&nbsp;vrsta&nbsp;samopouzdanja u tome, vidi str. 12.</p>");

        storeSettingsService.update(1L, request);

        ArgumentCaptor<StoreSettings> captor = ArgumentCaptor.forClass(StoreSettings.class);
        verify(storeSettingsRepository).save(captor.capture());
        String saved = captor.getValue().getValue();
        assertThat(saved).doesNotContain("&nbsp;posebna");
        assertThat(saved).contains("Postoji posebna vrsta samopouzdanja");
        assertThat(saved).contains("str.&nbsp;12");
    }

    @Test
    void update_ShouldLeaveNonRichTextValuesUntouched() {
        StoreSettings storeName = StoreSettings.builder().id(2L).key("store_name").label("Store name").build();
        when(storeSettingsRepository.findById(2L)).thenReturn(Optional.of(storeName));
        when(storeSettingsRepository.save(any(StoreSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        StoreSettingsRequest request = new StoreSettingsRequest();
        request.setValue("NoPressure  Wear");

        storeSettingsService.update(2L, request);

        ArgumentCaptor<StoreSettings> captor = ArgumentCaptor.forClass(StoreSettings.class);
        verify(storeSettingsRepository).save(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo("NoPressure  Wear");
    }
}
