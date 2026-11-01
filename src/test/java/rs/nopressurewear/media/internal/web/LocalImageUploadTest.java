package rs.nopressurewear.media.internal.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.nopressurewear.media.internal.application.FileStorageService;
import rs.nopressurewear.media.internal.application.RemoveBgService;
import rs.nopressurewear.media.internal.storage.LocalStorageService;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LocalImageUploadTest {
    @TempDir Path directory;

    @Test
    void uploadsOriginalImageWithRemovalDisabled() throws Exception {
        FileStorageService files = new FileStorageService();
        ReflectionTestUtils.setField(files, "uploadDir", directory.resolve("products").toString());
        RemoveBgService removal = new RemoveBgService();
        LocalStorageService storage = new LocalStorageService(files, removal);
        byte[] contents = {1, 2, 3, 4};
        MockMvcBuilders.standaloneSetup(new FileUploadController(storage, removal)).build()
                .perform(multipart("/api/upload/image")
                        .file(new MockMultipartFile("file", "product.png", "image/png", contents))
                        .param("removeBackground", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isString());
        try (var stored = Files.list(directory.resolve("products"))) {
            var paths = stored.toList();
            assertThat(paths).hasSize(1);
            assertThat(Files.readAllBytes(paths.getFirst())).isEqualTo(contents);
        }
    }
}
