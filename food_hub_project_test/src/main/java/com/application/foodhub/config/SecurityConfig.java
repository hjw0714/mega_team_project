package com.application.foodhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable())  // CORS 비활성화
            .csrf(csrf -> csrf.disable())  // CSRF 비활성화
            .formLogin(form -> form.disable())  // 폼 로그인 비활성화
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));  // X-Frame-Options 비활성화

        return http.build();
    }
}

