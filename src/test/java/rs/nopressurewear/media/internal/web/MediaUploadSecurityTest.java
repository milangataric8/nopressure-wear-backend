package rs.nopressurewear.media.internal.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import rs.nopressurewear.media.internal.storage.StorageProvider;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class MediaUploadSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean StorageProvider storage;

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EMPLOYEE"})
    void imageUploadPreservesMultipartAndResponseContract(String role) throws Exception {
        var file = new MockMultipartFile("file", "product.png", "image/png", new byte[]{1, 2});
        when(storage.store(any(), eq(false))).thenReturn("https://media.test/products/image.png");
        mvc.perform(multipart("/api/upload/image").file(file).param("removeBackground", "false")
                        .with(user("staff").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("https://media.test/products/image.png"));
        verify(storage).store(any(), eq(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EMPLOYEE"})
    void videoUploadUsesSameSelectedProvider(String role) throws Exception {
        var file = new MockMultipartFile("file", "product.mp4", "video/mp4", new byte[]{1, 2});
        when(storage.store(any(), eq(false))).thenReturn("https://media.test/products/video.mp4");
        mvc.perform(multipart("/api/upload/video").file(file).with(user("staff").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("https://media.test/products/video.mp4"));
        verify(storage).store(any(), eq(false));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customersCannotUploadImagesOrVideosOrReadStaffFeatures() throws Exception {
        mvc.perform(multipart("/api/upload/image").file("file", new byte[]{1})).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/upload/video").file("file", new byte[]{1})).andExpect(status().isForbidden());
        mvc.perform(get("/api/upload/features")).andExpect(status().isForbidden());
        verifyNoInteractions(storage);
    }

    @Test
    void anonymousRequestsRemainUnauthorized() throws Exception {
        mvc.perform(multipart("/api/upload/image").file("file", new byte[]{1})).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/upload/features")).andExpect(status().isUnauthorized());
        verifyNoInteractions(storage);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void backgroundRemovalFeatureIsIndependentOfProvider() throws Exception {
        mvc.perform(get("/api/upload/features")).andExpect(status().isOk())
                .andExpect(jsonPath("$.removeBackgroundEnabled").value(false));
        verifyNoInteractions(storage);
    }
}
