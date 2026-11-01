package rs.nopressurewear.identity.internal.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import rs.nopressurewear.storefront.api.SettingsQuery;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtFilterTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void expiredUploadTokenReturnsJson401WithoutCallingStorageOrController() throws Exception {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "local-test-secret-at-least-thirty-two-characters-long");
        ReflectionTestUtils.setField(jwt, "expiration", -60_000L);
        var user = org.springframework.security.core.userdetails.User.withUsername("admin@test.com")
                .password("unused").roles("ADMIN").build();
        assertRejected(jwt, jwt.generateToken(user));
    }

    @Test
    void malformedUploadTokenReturnsJson401() throws Exception {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "local-test-secret-at-least-thirty-two-characters-long");
        assertRejected(jwt, "not-a-jwt");
    }

    @Test
    void validTokenAuthenticatesAndContinuesUpload() throws Exception {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "local-test-secret-at-least-thirty-two-characters-long");
        ReflectionTestUtils.setField(jwt, "expiration", 60_000L);
        var user = org.springframework.security.core.userdetails.User.withUsername("admin@test.com")
                .password("unused").roles("ADMIN").build();
        var users = mock(UserDetailsServiceImpl.class);
        when(users.loadUserByUsername(user.getUsername())).thenReturn(user);
        var chain = mock(FilterChain.class);
        var filter = new JwtFilter(jwt, users, mock(SettingsQuery.class), new JwtAuthEntryPoint(new ObjectMapper()));
        var request = new MockHttpServletRequest("POST", "/api/upload/image");
        request.addHeader("Authorization", "Bearer " + jwt.generateToken(user));
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(user.getUsername());
        verify(chain).doFilter(request, response);
    }

    private void assertRejected(JwtUtil jwt, String token) throws Exception {
        var users = mock(UserDetailsServiceImpl.class);
        var settings = mock(SettingsQuery.class);
        var chain = mock(FilterChain.class);
        var filter = new JwtFilter(jwt, users, settings, new JwtAuthEntryPoint(new ObjectMapper()));
        var request = new MockHttpServletRequest("POST", "/api/upload/image");
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("INVALID_CREDENTIALS");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain, users, settings);
    }
}
