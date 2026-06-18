package rs.nopressurewear.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.UserRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user);

        String redirectUrl = "http://localhost:5173/oauth2/callback" +
                "?token=" + token +
                "&id=" + user.getId() +
                "&email=" + user.getEmail() +
                "&role=" + user.getRole().name() +
                "&firstName=" + user.getFirstName() +
                "&lastName=" + user.getLastName();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}