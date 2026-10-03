package com._1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        config.addAllowedOrigin("http://localhost:8080"); // Spring Boot后端端口
        config.addAllowedOrigin("http://localhost:8081"); // Vue CLI默认端口
        config.addAllowedOrigin("http://localhost:8082"); // Vue前端端口
        config.addAllowedOrigin("http://localhost:8083"); // Vue前端端口
        config.addAllowedOrigin("http://localhost:8084"); // Vue前端当前运行端口
        config.addAllowedOrigin("http://localhost:3000"); // 添加可能的开发服务器端口
        config.setAllowCredentials(true); 
        config.addAllowedHeader("*"); 
        config.addAllowedMethod("*"); 
        config.setMaxAge(3600L); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 