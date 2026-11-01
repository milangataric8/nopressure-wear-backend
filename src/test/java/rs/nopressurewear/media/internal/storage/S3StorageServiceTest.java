package rs.nopressurewear.media.internal.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import rs.nopressurewear.media.internal.application.RemoveBgService;
import rs.nopressurewear.media.internal.config.S3Properties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {
    private S3Client s3;
    private RemoveBgService removal;
    private S3StorageService storage;
    private final byte[] original = {1, 2, 3};
    private final MockMultipartFile image = new MockMultipartFile("file", "untrusted original.JPG", "image/jpeg", original);
    private static final String UUID_KEY = "products/550e8400-e29b-41d4-a716-446655440000.jpg";

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        removal = mock(RemoveBgService.class);
        storage = new S3StorageService(s3, new S3Properties("media-bucket", "eu-north-1",
                "https://media.test/", "products"), removal);
    }

    @Test
    void uploadsOriginalImageAndReturnsPublicUrl() throws Exception {
        String url = storage.store(image, false);
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo("media-bucket");
        assertThat(request.getValue().key()).matches("products/[0-9a-f-]{36}\\.jpg");
        assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            assertThat(stream.readAllBytes()).isEqualTo(original);
        }
        assertThat(url).isEqualTo("https://media.test/" + request.getValue().key());
        verifyNoInteractions(removal);
    }

    @Test
    void uploadsProcessedPngOnlyWhenRequestedAndEnabled() throws Exception {
        byte[] processed = {5, 6, 7};
        when(removal.isEnabled()).thenReturn(true);
        when(removal.removeBackground(image)).thenReturn(processed);
        String url = storage.store(image, true);
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(request.capture(), body.capture());
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
        assertThat(url).endsWith(".png");
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            assertThat(stream.readAllBytes()).isEqualTo(processed);
        }
    }

    @Test
    void disabledRemovalStillUploadsOriginal() throws Exception {
        storage.store(image, true);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(any(PutObjectRequest.class), body.capture());
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            assertThat(stream.readAllBytes()).isEqualTo(original);
        }
        verify(removal, never()).removeBackground(any());
    }

    @Test
    void uploadsVideoWithConfiguredNestedPrefix() {
        storage = new S3StorageService(s3, new S3Properties("media-bucket", "eu-north-1",
                "https://media.test/media/", "/catalog/products/"), removal);
        String url = storage.store(new MockMultipartFile("file", "clip.mp4", "video/mp4", original), false);
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().key()).startsWith("catalog/products/").endsWith(".mp4");
        assertThat(request.getValue().contentType()).isEqualTo("video/mp4");
        assertThat(url).startsWith("https://media.test/media/catalog/products/");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"../../bad", "no-extension", "file.bad-extension"})
    void missingOrUnsafeExtensionFallsBackToBin(String filename) {
        String url = storage.store(new MockMultipartFile("file", filename, null, original), false);
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(url).endsWith(".bin");
        assertThat(request.getValue().contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void deletesOnlyOwnedGeneratedKey() {
        storage.delete("https://media.test/" + UUID_KEY);
        verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("media-bucket").key(UUID_KEY).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "https://res.cloudinary.com/demo/image/upload/a.jpg",
            "https://media.test.evil.com/products/550e8400-e29b-41d4-a716-446655440000.jpg",
            "https://media.test@evil.com/products/550e8400-e29b-41d4-a716-446655440000.jpg",
            "https://evil.com/a.jpg", "/uploads/products/a.jpg", "not a url",
            "https://media.test/products/../a.jpg", "https://media.test/products/%2e%2e/a.jpg",
            "https://media.test/other/550e8400-e29b-41d4-a716-446655440000.jpg",
            "https://media.test/products/550e8400-e29b-41d4-a716-446655440000.jpg?query=1",
            "http://media.test/products/550e8400-e29b-41d4-a716-446655440000.jpg"
    })
    void refusesForeignOrAmbiguousDeletion(String url) {
        storage.delete(url);
        verifyNoInteractions(s3);
    }

    @Test
    void deletionFailuresDoNotBreakCaller() {
        when(s3.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(403).build());
        assertThatCode(() -> storage.delete("https://media.test/" + UUID_KEY)).doesNotThrowAnyException();
    }

    @Test
    void uploadFailurePropagatesWithoutLeakingProviderMessage() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("do-not-expose-provider-details").statusCode(403).build());
        assertThatThrownBy(() -> storage.store(image, false))
                .isInstanceOf(IllegalStateException.class).hasMessage("S3 media upload failed");
    }

    @Test
    void deleteRespectsBasePathAndNestedPrefix() {
        storage = new S3StorageService(s3, new S3Properties("media-bucket", "eu-north-1",
                "https://media.test/media", "catalog/products"), removal);
        storage.delete("https://media.test/media/catalog/" + UUID_KEY);
        verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("media-bucket").key("catalog/" + UUID_KEY).build());
    }
}
