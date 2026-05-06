package rs.webshop.webshop_core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import rs.webshop.webshop_core.security.JwtFilter;
import rs.webshop.webshop_core.security.UserDetailsServiceImpl;

import java.util.List;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/coupons/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/products/**").permitAll()
                        .requestMatchers(GET, "/api/categories/**").permitAll()
                        .requestMatchers(POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(PATCH, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(DELETE, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/users/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(PUT, "/api/users/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(PATCH, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(GET, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(POST, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(PUT, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(DELETE, "/api/addresses/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(POST, "/api/upload/**").hasRole("ADMIN")
                        .requestMatchers(POST, "/api/coupons/validate").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

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
                "http://localhost:5173"
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