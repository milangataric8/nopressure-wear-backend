package rs.nopressurewear.identity.internal.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;
import rs.nopressurewear.identity.internal.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigurationTest {
    private static final String ORIGINS =
            "https://nopressurewear.rs,https://www.nopressurewear.rs,http://localhost:5174";

    private SecurityConfig config(String origins) {
        return new SecurityConfig(null, null, null, null, null, null, null, origins);
    }

    private MockHttpServletResponse preflight(String allowed, String origin) throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/api/auth/forgot-password");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "authorization,content-type");
        var response = new MockHttpServletResponse();
        new CorsFilter(config(allowed).corsConfigurationSource()).doFilter(request, response,
                (req, res) -> { throw new AssertionError("Preflight must finish before controller logic"); });
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://nopressurewear.rs", "https://www.nopressurewear.rs", "http://localhost:5174"})
    void requestedOriginsAreAllowed(String origin) throws Exception {
        var response = preflight(ORIGINS, origin);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Headers")).contains("authorization", "content-type");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://localhost:5175", "https://nopressurewear.rs.evil.example"})
    void unlistedOriginsRemainRejected(String origin) throws Exception {
        var response = preflight(ORIGINS, origin);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void allowlistIsNormalized() {
        var request = new MockHttpServletRequest("OPTIONS", "/api/auth/forgot-password");
        var cors = config(" https://nopressurewear.rs, ,http://localhost:5174,https://nopressurewear.rs,")
                .corsConfigurationSource().getCorsConfiguration(request);
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("https://nopressurewear.rs", "http://localhost:5174");
    }

    @Test
    void singleOriginConfigurationStillWorks() throws Exception {
        assertThat(preflight("https://nopressurewear.rs", "https://nopressurewear.rs").getStatus()).isEqualTo(200);
        assertThat(preflight("https://nopressurewear.rs", "http://localhost:5174").getStatus()).isEqualTo(403);
    }
}
