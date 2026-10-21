package rs.nopressurewear.identity.internal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rs.nopressurewear.platform.api.ErrorCode;
import rs.nopressurewear.platform.error.ApiError;
import rs.nopressurewear.identity.internal.security.JwtAuthEntryPoint;
import rs.nopressurewear.identity.internal.security.JwtFilter;
import rs.nopressurewear.identity.internal.security.OAuth2AuthenticationSuccessHandler;
import rs.nopressurewear.identity.internal.security.OAuth2UserService;
import rs.nopressurewear.identity.internal.security.PasswordChangeRequiredFilter;
import rs.nopressurewear.identity.internal.security.UserDetailsServiceImpl;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final ObjectMapper objectMapper;
    private final String frontendUrl;

    public SecurityConfig(JwtFilter jwtFilter,
                          PasswordChangeRequiredFilter passwordChangeRequiredFilter,
                          UserDetailsServiceImpl userDetailsService,
                          OAuth2UserService oAuth2UserService,
                          OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                          JwtAuthEntryPoint jwtAuthEntryPoint,
                          ObjectMapper objectMapper,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.jwtFilter = jwtFilter;
        this.passwordChangeRequiredFilter = passwordChangeRequiredFilter;
        this.userDetailsService = userDetailsService;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.objectMapper = objectMapper;
        this.frontendUrl = frontendUrl;
    }
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler())
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- must precede the broader /api/auth/** permitAll below: these two
                        // derive "who" from the caller's own token, so they require one ---
                        .requestMatchers(POST, "/api/auth/change-password").authenticated()
                        .requestMatchers(GET, "/api/auth/me").authenticated()

                        // --- public shop endpoints ---
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(GET, "/api/products/**").permitAll()
                        .requestMatchers(GET, "/api/categories/**").permitAll()
                        .requestMatchers(GET, "/api/banners/active").permitAll()
                        .requestMatchers(GET, "/api/settings/**").permitAll()
                        .requestMatchers(GET, "/api/filters/visible").permitAll()
                        .requestMatchers(GET, "/api/popups/active").permitAll()
                        .requestMatchers(GET, "/api/stores/active").permitAll()
                        .requestMatchers(GET, "/api/stores/product/**").permitAll()
                        .requestMatchers(GET, "/api/legal/**").permitAll()
                        .requestMatchers(GET, "/api/reviews/**").permitAll()
                        .requestMatchers(POST, "/api/payments/webhook").permitAll()
                        .requestMatchers(POST, "/api/payments/guest-payment-intent").permitAll()
                        .requestMatchers(POST, "/api/orders/guest-checkout").permitAll()

                        // --- SUPER_ADMIN only: must precede any broader admin rule ---
                        .requestMatchers("/api/reports/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/dashboard/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/employees/**").hasRole("SUPER_ADMIN")

                        // --- CATALOG: EMPLOYEE and up (role hierarchy lets ADMIN/SUPER_ADMIN through) ---
                        .requestMatchers(POST, "/api/products/**").hasRole("EMPLOYEE")
                        .requestMatchers(PUT, "/api/products/**").hasRole("EMPLOYEE")
                        .requestMatchers(DELETE, "/api/products/**").hasRole("EMPLOYEE")
                        .requestMatchers(PATCH, "/api/products/**").hasRole("EMPLOYEE")
                        .requestMatchers(POST, "/api/categories/**").hasRole("EMPLOYEE")
                        .requestMatchers(PUT, "/api/categories/**").hasRole("EMPLOYEE")
                        .requestMatchers(DELETE, "/api/categories/**").hasRole("EMPLOYEE")
                        .requestMatchers(POST, "/api/upload/**").hasRole("EMPLOYEE")

                        // --- customer self-service ---
                        .requestMatchers(GET, "/api/users/customers/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/users/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers(PUT, "/api/users/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers(DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(PATCH, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/addresses/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers(POST, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(PUT, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(DELETE, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(POST, "/api/coupons/validate").authenticated()
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()

                        // --- orders: admin views first, then customer self-access ---
                        .requestMatchers(GET, "/api/orders/all").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/orders/user/*/all").hasRole("ADMIN")
                        .requestMatchers("/api/orders/admin/**").hasRole("ADMIN")
                        .requestMatchers(PATCH, "/api/orders/*/status").hasRole("ADMIN")
                        .requestMatchers("/api/orders/**").authenticated()

                        // --- rest of MANAGEMENT: ADMIN and up ---
                        .requestMatchers("/api/coupons/**").hasRole("ADMIN")
                        .requestMatchers("/api/banners/**").hasRole("ADMIN")
                        .requestMatchers("/api/popups/**").hasRole("ADMIN")
                        .requestMatchers("/api/filters/**").hasRole("ADMIN")
                        .requestMatchers(PUT, "/api/settings/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/**").hasRole("ADMIN")
                        .requestMatchers("/api/failed-emails/**").hasRole("ADMIN")
                        .requestMatchers(PUT, "/api/legal/**").hasRole("ADMIN")
                        // store locations carry product stock — treated as catalog, EMPLOYEE and up
                        .requestMatchers("/api/stores/**").hasRole("EMPLOYEE")

                        // --- fail-closed default ---
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(passwordChangeRequiredFilter, JwtFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain sitemapFilterChain(HttpSecurity http) {
        http
                .securityMatcher("/sitemap.xml")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/oauth2/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorization")
                        )
                );

        return http.build();
    }

    /**
     * SUPER_ADMIN > ADMIN > EMPLOYEE. Spring Security auto-applies this bean to both
     * {@code authorizeHttpRequests} and method security ({@code @PreAuthorize}).
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPER_ADMIN").implies("ADMIN")
                .role("ADMIN").implies("EMPLOYEE")
                .build();
    }

    /** 403 as JSON, not an HTML error page — the frontend expects JSON, in the same {@link ApiError}
     * shape as {@link rs.nopressurewear.platform.error.GlobalExceptionHandler}'s AccessDeniedException handler. */
    @Bean
    public AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    new ApiError(403, ErrorCode.ACCESS_DENIED, "errors.accessDenied"));
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}