package com.example.be.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/oauth2/authorization/**",
                                "/login/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // CV endpoints - USER có thể upload, HR có thể approve/reject
                        // CV endpoints - Tất cả authenticated users có thể upload
                        .requestMatchers("/api/cvs/upload", "/api/cvs/my").authenticated()
                        .requestMatchers("/api/cvs/pending", "/api/cvs/all", "/api/cvs/*/approve", "/api/cvs/*/reject")
                        .hasAnyRole("HR", "ADMIN")

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Document endpoints
                        .requestMatchers("/api/documents/**", "/api/intern-documents/**").permitAll()

                        // Profile endpoints
                        .requestMatchers("/api/profiles/**", "/api/intern-profiles/**").permitAll()

                        // Intern endpoints
                        .requestMatchers("/api/interns/**").permitAll()

                        // Tất cả request khác cần authentication
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login.disable())
                .httpBasic(Customizer.withDefaults());

        // Cho phép H2 console chạy trong frame
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}