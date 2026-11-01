package rs.nopressurewear.media.internal.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import rs.nopressurewear.media.internal.application.FileStorageService;
import rs.nopressurewear.media.internal.application.RemoveBgService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BackgroundRemovalFlagTest {
    private final MockMultipartFile file = new MockMultipartFile(
            "file", "product.jpg", "image/jpeg", new byte[]{1, 2, 3});

    @Test
    void disabledLocalRemovalStoresOriginalEvenWhenRequested() {
        FileStorageService files = mock(FileStorageService.class);
        RemoveBgService removal = mock(RemoveBgService.class);
        when(files.storeFile(file)).thenReturn("/uploads/products/original.jpg");

        assertThat(new LocalStorageService(files, removal).store(file, true))
                .isEqualTo("/uploads/products/original.jpg");
        verify(removal, never()).removeBackground(any());
        verify(files).storeFile(file);
    }

    @Test
    void disabledProductionRemovalUploadsOriginalEvenWhenRequested() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        RemoveBgService removal = mock(RemoveBgService.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(file.getBytes()), anyMap())).thenReturn(Map.of("secure_url", "https://images.test/original.jpg"));

        assertThat(new CloudinaryStorageService(cloudinary, removal).store(file, true))
                .isEqualTo("https://images.test/original.jpg");
        verify(removal, never()).removeBackground(any());
        verify(uploader).upload(eq(file.getBytes()), anyMap());
    }

    @Test
    void enabledProductionRemovalUploadsProcessedBytes() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        RemoveBgService removal = mock(RemoveBgService.class);
        byte[] processed = {4, 5, 6};
        when(removal.isEnabled()).thenReturn(true);
        when(removal.removeBackground(file)).thenReturn(processed);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(processed), anyMap())).thenReturn(Map.of("secure_url", "https://images.test/processed.png"));

        assertThat(new CloudinaryStorageService(cloudinary, removal).store(file, true))
                .isEqualTo("https://images.test/processed.png");
        verify(removal).removeBackground(file);
        verify(uploader).upload(eq(processed), anyMap());
    }

    @Test
    void serviceRejectsDirectCallsWhenDisabledBeforeReadingFile() {
        RemoveBgService removal = new RemoveBgService();
        assertThat(removal.isEnabled()).isFalse();
        assertThatThrownBy(() -> removal.removeBackground(null))
                .isInstanceOf(IllegalStateException.class).hasMessage("Background removal is disabled");
    }

    @Test
    void enabledServiceRequiresApiKey() {
        RemoveBgService removal = new RemoveBgService();
        ReflectionTestUtils.setField(removal, "enabled", true);
        assertThatThrownBy(() -> removal.removeBackground(file))
                .isInstanceOf(IllegalStateException.class).hasMessage("Background removal API key is not configured");
    }
}
