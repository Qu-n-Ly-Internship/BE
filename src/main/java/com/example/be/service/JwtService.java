package com.example.be.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24h
    private final String SECRET_KEY;

    public JwtService(@Value("${jwt.secret}") String secretKey) {
        this.SECRET_KEY = secretKey;
    }

    public String generateToken(String username) {
        try {
            logger.debug("Generating token for username: {}", username);
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }
            return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION )) // 24h
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                    .compact();
        } catch (Exception e) {
            logger.error("JWT generation error for username: {}", username, e);
            throw e;
        }
    }

    public String extractUsername(String token) {
        try {
            logger.debug("Extracting username from token: {}", token);
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token cannot be null or empty");
            }
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            throw e;
        }
    }
    public String extractRole(String token) {
        try {
            logger.debug("Extracting role from token: {}", token);
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token cannot be null or empty");
            }
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            logger.error("Error extracting role from token", e);
            throw e;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            logger.debug("Validating token: {}", token);
            if (token == null || token.isEmpty()) {
                return false;
            }
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }
}