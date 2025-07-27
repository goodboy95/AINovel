package com.example.ainovel.config;

import com.example.ainovel.repository.UserRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the application, including JWT, CORS, and password encoding.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final UserRepository userRepository;

    @Value("${cors.allowed-origins:http://localhost:8080}")
    private String[] allowedOrigins;

    /**
     * Configures the main security filter chain.
     *
     * @param http The HttpSecurity to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/login", "/register", "/").permitAll()
                        .requestMatchers("/index.html", "/vite.svg", "/assets/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures CORS settings based on application properties.
     *
     * @return The CorsConfigurationSource.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Creates the main AuthenticationProvider bean.
     *
     * @return The configured DaoAuthenticationProvider.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager as a bean.
     *
     * @param config The AuthenticationConfiguration.
     * @return The AuthenticationManager.
     * @throws Exception If an error occurs.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the UserDetailsService bean to load user-specific data.
     *
     * @return The UserDetailsService implementation.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            log.debug("Attempting to load user by username: {}", username);
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("User not found: {}", username);
                        return new UsernameNotFoundException("User not found: " + username);
                    });
        };
    }

    /**
     * Defines the PasswordEncoder bean.
     *
     * @return The BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
