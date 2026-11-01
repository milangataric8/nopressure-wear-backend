package rs.nopressurewear.identity.internal.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import rs.nopressurewear.identity.api.Role;
import rs.nopressurewear.identity.internal.domain.User;
import rs.nopressurewear.identity.internal.persistence.UserRepository;
import rs.nopressurewear.storefront.api.SettingsQuery;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OAuth2FrontendRedirectTest {
    @Test
    void googleLoginReturnsToConfiguredLocalFrontend() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        UserRepository users = mock(UserRepository.class);
        SettingsQuery settings = mock(SettingsQuery.class);
        User user = User.builder().id(7L).email("oauth@test.invalid")
                .firstName("Test").lastName("User").role(Role.CUSTOMER).emailVerified(true).build();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(settings.findValue("login_enabled")).thenReturn(Optional.of("true"));
        when(jwt.generateToken(user)).thenReturn("test-token");
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn(user.getEmail());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        var response = new MockHttpServletResponse();
        new OAuth2AuthenticationSuccessHandler(jwt, users, settings, "http://localhost:5173")
                .onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).startsWith("http://localhost:5173/oauth2/callback?token=test-token");
        assertThat(response.getRedirectedUrl()).contains("&id=7");
    }
}
