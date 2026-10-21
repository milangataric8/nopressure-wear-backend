package rs.nopressurewear.identity.internal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import rs.nopressurewear.platform.error.ApiError;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpMethod.OPTIONS;
import static rs.nopressurewear.platform.api.ErrorCode.PASSWORD_CHANGE_REQUIRED;

/**
 * Backend half of "prinudna promena lozinke pri prvom logovanju" — the frontend redirect is
 * only UX. As long as the caller's token carries {@code pwdChange: true}, every request is
 * rejected except the handful needed to actually change the password, regardless of what the
 * caller's role would otherwise be allowed to do.
 * <p>
 * Runs after {@link JwtFilter} but does not depend on it having authenticated the request — it
 * reads the claim straight off the token, so it applies equally to a request that would
 * otherwise hit a {@code permitAll()} endpoint.
 */
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private static final Set<String> ALLOWED_WHEN_PASSWORD_CHANGE_REQUIRED = Set.of(
            "/api/auth/change-password",
            "/api/auth/me",
            "/api/auth/logout",
            "/api/auth/refresh",
            "/api/settings/map"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private boolean isAllowed(String uri) {
        return ALLOWED_WHEN_PASSWORD_CHANGE_REQUIRED.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // CORS preflight carries no Authorization header of its own, but skip explicitly so a
        // browser never sees a 403 here before it gets to find that out.
        if (OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION);
        if (isNull(authHeader) || !authHeader.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        boolean mustChangePassword = jwtUtil.isPasswordChangeRequired(token);

        if (mustChangePassword && !isAllowed(request.getRequestURI())) {
            response.setStatus(SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), new ApiError(
                    SC_FORBIDDEN,
                    PASSWORD_CHANGE_REQUIRED,
                    "errors.passwordChangeRequired"));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
