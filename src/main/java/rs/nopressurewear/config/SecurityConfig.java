package rs.nopressurewear.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rs.nopressurewear.security.*;

import java.util.List;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(GET, "/api/products/**").permitAll()
                        .requestMatchers(POST, "/api/products/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(PUT, "/api/products/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(DELETE, "/api/products/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(PATCH, "/api/products/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(GET, "/api/products/featured").permitAll()
                        .requestMatchers(GET, "/api/products/*/similar").permitAll()
                        .requestMatchers(GET, "/api/categories/**").permitAll()
                        .requestMatchers(POST, "/api/categories/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(PUT, "/api/categories/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(DELETE, "/api/categories/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(GET, "/api/users/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers(PUT, "/api/users/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers(DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(PATCH, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/addresses/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers(POST, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(PUT, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(DELETE, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(POST, "/api/upload/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(POST, "/api/upload/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(POST, "/api/coupons/validate").authenticated()
                        .requestMatchers("/api/coupons/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(GET, "/api/banners/active").permitAll()
                        .requestMatchers("/api/banners/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/api/employees/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/orders/guest-checkout").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers(GET, "/api/users/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(GET, "/api/settings/**").permitAll()
                        .requestMatchers(PUT, "/api/settings/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/filters/visible").permitAll()
                        .requestMatchers("/api/filters/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/payments/guest-payment-intent").permitAll()
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers(GET, "/api/popups/active").permitAll()
                        .requestMatchers("/api/popups/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/stores/active").permitAll()
                        .requestMatchers(GET, "/api/stores/product/**").permitAll()
                        .requestMatchers("/api/stores/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers(GET, "/api/reviews/**").permitAll()
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/api/notifications/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
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

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174"
        ));
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