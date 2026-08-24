package rs.nopressurewear.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import rs.nopressurewear.dto.category.CategoryRequest;
import rs.nopressurewear.dto.category.CategoryResponse;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.security.JwtUtil;
import rs.nopressurewear.security.UserDetailsServiceImpl;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.nopressurewear.service.CategoryService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private StoreSettingsRepository storeSettingsRepository;

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_ShouldReturn201_WhenValidRequest() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");
        request.setDescription("Electronic devices");

        CategoryResponse response = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .build();

        when(categoryService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_ShouldReturn400_WhenInvalidRequest() throws Exception {
        CategoryRequest request = new CategoryRequest();

        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getAll_ShouldReturn200_WithListOfCategories() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .build();

        when(categoryService.getAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Electronics"));
    }

    @Test
    @WithMockUser
    void getById_ShouldReturn200_WhenExists() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .build();

        when(categoryService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_ShouldReturn204_WhenExists() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAll_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().is3xxRedirection());
    }
}