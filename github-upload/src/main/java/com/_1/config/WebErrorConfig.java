package com._1.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class WebErrorConfig {
    
    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        return new ErrorPageRegistrar() {
            @Override
            public void registerErrorPages(ErrorPageRegistry registry) {
                // 注册404错误页面
                registry.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"));
                
                // 注册500错误页面
                registry.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500"));
                
                // 注册默认错误页面
                registry.addErrorPages(new ErrorPage("/error"));
            }
        };
    }
} 