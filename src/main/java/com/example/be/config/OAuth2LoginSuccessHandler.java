package com.example.be.config;

import com.example.be.entity.User;
import com.example.be.service.AuthService;
import com.example.be.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    private final JwtService jwtService;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            logger.debug("OAuth2 attributes: {}", oAuth2User.getAttributes());

            User user = authService.processOAuthPostLogin(oAuth2User);
            logger.debug("Processed user: {}", user);

            String token = jwtService.generateToken(user.getEmail());
            logger.debug("Generated token: {}", token);

            String redirectUrl = "http://localhost:5173/oauth2/callback?token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8);
            logger.debug("Redirecting to: {}", redirectUrl);

            if (response.isCommitted()) {
                logger.error("Response already committed, cannot redirect");
                response.sendError(500, "Cannot redirect: Response already committed");
                return;
            }
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            logger.error("OAuth2 success handler error", e);
            response.sendError(500, "OAuth2 login failed: " + e.getMessage());
        }
    }
}