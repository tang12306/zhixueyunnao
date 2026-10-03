package com._1.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/error/**").permitAll()
                .requestMatchers("/api/test/**").permitAll() // 添加测试端点
                .requestMatchers("/api/auth/**").permitAll() // 添加API认证端点
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // AI生成题目相关API
                .requestMatchers("/api/ai/generate-question").permitAll()
                .requestMatchers("/api/ai/generate-batch-questions").permitAll()
                .requestMatchers("/api/generate-question").permitAll()
                .requestMatchers("/api/ai/save-questions").permitAll()
                .requestMatchers("/api/ai/generate-exam").permitAll()
                .requestMatchers("/api/ai/save-exam").permitAll()
                
                // 用户API - 需要认证
                .requestMatchers("/api/user/**").authenticated()

                // 科目管理API - 临时允许访问以支持前端
                .requestMatchers("/api/subjects/**").permitAll()
                // 章节管理API
                .requestMatchers("/api/subjects/{subjectId}/chapters").permitAll()
                .requestMatchers("/api/chapters/**").permitAll()

                // 试卷管理API - 临时允许访问
                .requestMatchers("/api/papers/**").permitAll()

                // 学生API端点 - 临时允许访问
                .requestMatchers("/student/api/**").permitAll()

                // 组织管理API - 临时允许访问
                .requestMatchers("/api/colleges/**").permitAll()
                .requestMatchers("/api/majors/**").permitAll()
                .requestMatchers("/api/classes/**").permitAll()

                // 系统设置API - 临时允许访问
                .requestMatchers("/api/settings/**").permitAll()

                // 学生管理API - 临时允许访问
                .requestMatchers("/api/students/**").permitAll()

                // 题目管理相关
                .requestMatchers("/questions/save").permitAll()
                .requestMatchers("/questions/delete/**").permitAll()
                .requestMatchers("/questions/edit/**").permitAll()
                .requestMatchers("/questions/new").permitAll()
                .requestMatchers("/questions").permitAll()
                .requestMatchers("/questions/chapters-by-subject").permitAll()
                .requestMatchers("/questions/import").permitAll()
                .requestMatchers("/questions/export").permitAll()
                .requestMatchers("/questions/export-template").permitAll()
                .requestMatchers(HttpMethod.GET, "/questions/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/questions/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/questions/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/questions/**").permitAll()
                
                // 学生特定路径，需要学生角色
                .requestMatchers("/student/**").hasRole("STUDENT")
                
                // 教师特定路径（如果存在），需要教师角色
                // .requestMatchers("/teacher/**").hasRole("TEACHER")
                
                .requestMatchers("/").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/login")
                .ignoringRequestMatchers("/api/auth/**") // 忽略API认证端点的CSRF
                .ignoringRequestMatchers("/api/ai/generate-question")
                .ignoringRequestMatchers("/api/ai/generate-batch-questions")
                .ignoringRequestMatchers("/api/generate-question")
                .ignoringRequestMatchers("/api/ai/save-questions")
                .ignoringRequestMatchers("/api/ai/generate-exam")
                .ignoringRequestMatchers("/api/ai/save-exam")
                .ignoringRequestMatchers("/api/subjects/**")
                .ignoringRequestMatchers("/api/subjects/{subjectId}/chapters")
                .ignoringRequestMatchers("/api/chapters/**")
                .ignoringRequestMatchers("/api/papers/**")
                .ignoringRequestMatchers("/questions/save")
                .ignoringRequestMatchers("/questions/delete/**")
                .ignoringRequestMatchers("/questions/edit/**")
                .ignoringRequestMatchers("/questions/import")
                .ignoringRequestMatchers("/questions/**")
                // 新增：忽略用户API的CSRF
                .ignoringRequestMatchers("/api/user/**")
                // 新增：忽略组织管理和系统设置API的CSRF
                .ignoringRequestMatchers("/api/colleges/**")
                .ignoringRequestMatchers("/api/majors/**")
                .ignoringRequestMatchers("/api/classes/**")
                .ignoringRequestMatchers("/api/settings/**")
                .ignoringRequestMatchers("/api/students/**") // 新增：忽略学生管理API的CSRF
                // 新增：忽略学生API的CSRF
                .ignoringRequestMatchers("/student/api/**")
                // 新增：忽略测试API的CSRF
                .ignoringRequestMatchers("/api/test/**")
            );
        
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 