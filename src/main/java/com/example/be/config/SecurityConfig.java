package com.example.be.config;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    // ======================= API CHAIN =======================
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // RẤT QUAN TRỌNG
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Cho phép toàn bộ /api/**
                )
                .oauth2Login(oauth -> oauth.disable()) // Không dùng OAuth2
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // ======================= WEB CHAIN =======================
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/oauth2/**",
                                "/oauth2/authorization/**",
                                "/login/oauth2/**",
                                "/login/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        // Cho phép H2 console chạy trong frame
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    // ======================= CORS =======================
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

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // ======================= Password encoder =======================
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
