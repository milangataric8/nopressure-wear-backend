package rs.nopressurewear.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.StoreSettingsRepository;
import rs.nopressurewear.repository.UserRepository;

import java.io.IOException;

import static rs.nopressurewear.constants.Role.CUSTOMER;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil,
                                              UserRepository userRepository,
                                              StoreSettingsRepository storeSettingsRepository,
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

        boolean loginEnabled = storeSettingsRepository.findByKey("login_enabled")
                .map(s -> !"false".equalsIgnoreCase(s.getValue()))
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