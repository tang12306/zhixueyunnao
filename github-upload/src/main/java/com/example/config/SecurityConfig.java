package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/student/login", "/student/logout").permitAll()
                .requestMatchers("/student/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/student/login")
                .defaultSuccessUrl("/student/exams")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/student/logout")
                .logoutSuccessUrl("/student/login")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());
        
        return http.build();
    }
} 