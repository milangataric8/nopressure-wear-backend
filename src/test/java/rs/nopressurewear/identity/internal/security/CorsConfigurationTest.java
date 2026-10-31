package rs.nopressurewear.identity.internal.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;
import rs.nopressurewear.identity.internal.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigurationTest {

    private String productionOrigins() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        String origins = yaml.getObject().getProperty("app.frontend-allowed-origins");
        return new MockEnvironment().withProperty("FRONTEND_URL", "https://nopressurewear.rs")
                .resolveRequiredPlaceholders(origins);
    }

    private SecurityConfig config(String origins) {
        return new SecurityConfig(null, null, null, null, null, null, null, origins);
    }

    private MockHttpServletResponse preflight(String origins, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/forgot-password");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CorsFilter(config(origins).corsConfigurationSource()).doFilter(request, response,
                (req, res) -> { throw new AssertionError("Preflight must finish before authentication or controller logic"); });
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://nopressurewear.rs", "https://www.nopressurewear.rs"})
    void productionOriginsAreAllowed(String origin) throws Exception {
        MockHttpServletResponse response = preflight(
                productionOrigins(), origin);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Headers")).isEqualTo("content-type");
        assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("3600");
    }

    @Test
    void unknownOriginIsRejected() throws Exception {
        MockHttpServletResponse response = preflight(
                "https://nopressurewear.rs,https://www.nopressurewear.rs", "https://example.com");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:5174"})
    void localDevelopmentOriginsAreAllowed(String origin) throws Exception {
        MockHttpServletResponse response = preflight("http://localhost:5173,http://localhost:5174", origin);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
    }

    @Test
    void originsAreTrimmedAndDeduplicatedAndEmptyEntriesAreIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/forgot-password");
        var cors = config(" https://nopressurewear.rs, ,https://www.nopressurewear.rs,https://nopressurewear.rs,")
                .corsConfigurationSource().getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly(
                "https://nopressurewear.rs", "https://www.nopressurewear.rs");
    }

    @Test
    void singleCanonicalOriginRemainsSupported() throws Exception {
        assertThat(preflight("http://localhost:5173", "http://localhost:5173").getStatus()).isEqualTo(200);
        assertThat(preflight("http://localhost:5173", "https://www.nopressurewear.rs").getStatus()).isEqualTo(403);
    }
}
