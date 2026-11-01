package rs.nopressurewear.media.internal.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.nopressurewear.media.internal.application.RemoveBgService;
import rs.nopressurewear.media.internal.storage.StorageProvider;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UploadFeaturesTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exposesConfiguredFlagWithoutProcessingImages(boolean enabled) throws Exception {
        RemoveBgService removal = mock(RemoveBgService.class);
        StorageProvider storage = mock(StorageProvider.class);
        when(removal.isEnabled()).thenReturn(enabled);
        MockMvcBuilders.standaloneSetup(new FileUploadController(storage, removal)).build()
                .perform(get("/api/upload/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removeBackgroundEnabled").value(enabled));
        verifyNoInteractions(storage);
        verify(removal, never()).removeBackground(any());
    }
}
