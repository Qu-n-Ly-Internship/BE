package com.example.be.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("User Info: " + oAuth2User.getAttributes());

        // Lấy email từ attributes
        String email = (String) oAuth2User.getAttribute("email");

        // Giả sử bạn inject AuthService để lấy role (hoặc query DB trực tiếp)
        // Ở đây tôi giả sử role là "ROLE_INTERN" mặc định, bạn có thể query user từ DB
        // dựa trên email
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("INTERN")); // Thay bằng role thực từ DB nếu cần

        // Return DefaultOAuth2User với authorities
        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "email");
    }
}