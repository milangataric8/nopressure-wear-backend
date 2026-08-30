package rs.nopressurewear.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@Transactional
class ProductCreateNestedRelationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class SecurityMockMvcConfig implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(SecurityMockMvcConfigurers.springSecurity());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithNestedPayload_returns201_andRelationsReadableViaGetEndpoints() throws Exception {
        long variantId = createProduct("{\"name\":\"Variant Tee\",\"price\":19.99,\"sku\":\"TEE-VAR\"}");

        String storeJson = objectMapper.writeValueAsString(Map.of(
                "name", "Central", "street", "Main 1", "city", "Novi Sad", "country", "Serbia"));
        String storeResp = mockMvc.perform(post("/api/stores").contentType(APPLICATION_JSON).content(storeJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long storeId = objectMapper.readTree(storeResp).get("id").asLong();

        String body = ("{\"name\":\"Main Tee\",\"price\":29.99,\"sku\":\"TEE-MAIN\","
                + "\"colorVariantIds\":[%d],"
                + "\"stores\":[{\"storeLocationId\":%d,\"inStock\":true}]}").formatted(variantId, storeId);

        String createResp = mockMvc.perform(post("/api/products").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stores[0].storeLocationId").value((int) storeId))
                .andExpect(jsonPath("$.stores[0].inStock").value(true))
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(get("/api/products/{id}/variants", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == " + variantId + ")]").exists());

        mockMvc.perform(get("/api/stores/product/{id}/all", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeLocationId").value((int) storeId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithInvalidVariantId_returns400_andCreatesNoProduct() throws Exception {
        String body = "{\"name\":\"Bad Tee\",\"price\":29.99,\"sku\":\"TEE-BAD\",\"colorVariantIds\":[999999]}";

        mockMvc.perform(post("/api/products").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/products")
                        .param("search", "Bad Tee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.empty()));
    }

    private long createProduct(String json) throws Exception {
        String resp = mockMvc.perform(post("/api/products").contentType(APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asLong();
    }
}
