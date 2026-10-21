package rs.nopressurewear.identity.internal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import rs.nopressurewear.identity.internal.domain.User;
import rs.nopressurewear.storefront.api.SettingsQuery;
import rs.nopressurewear.identity.internal.persistence.UserRepository;

import java.io.IOException;

import static rs.nopressurewear.identity.api.Role.CUSTOMER;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SettingsQuery storeSettingsRepository;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil,
                                              UserRepository userRepository,
                                              SettingsQuery storeSettingsRepository,
                                              @Value("${app.frontend-url}") String frontendUrl) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        boolean loginEnabled = storeSettingsRepository.findValue("login_enabled")
                .map(s -> !"false".equalsIgnoreCase(s))
                .orElse(true);

        if (!loginEnabled && user.getRole() == CUSTOMER) {
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/login?error=login_disabled");
            return;
        }

        String token = jwtUtil.generateToken(user);

        String redirectUrl = frontendUrl + "/oauth2/callback" +
                "?token=" + token +
                "&id=" + user.getId() +
                "&email=" + user.getEmail() +
                "&role=" + user.getRole().name() +
                "&firstName=" + user.getFirstName() +
                "&lastName=" + user.getLastName();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}