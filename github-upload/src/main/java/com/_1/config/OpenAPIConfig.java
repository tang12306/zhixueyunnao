package com._1.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title("教务题库管理系统 API")
                .version("v1.0.0")
                .description("这是教务题库管理系统重构项目的API文档。")
                .termsOfService("http://example.com/terms/")
                .contact(new Contact().name("开发团队").url("http://example.com/contact").email("dev@example.com"))
                .license(new License().name("Apache 2.0").url("http://springdoc.org"));

        final String securitySchemeName = "bearerAuth";
        
        Components components = new Components()
                .addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入JWT Token进行认证，格式: Bearer {token}")
                );
        
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        return new OpenAPI()
                .info(info)
                .components(components)
                .addSecurityItem(securityRequirement);
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("00-all-apis")
                .packagesToScan("com._1") // 更新为项目主包名
                .build();
    }

    @Bean
    public GroupedOpenApi authApis() {
        return GroupedOpenApi.builder()
                .group("01-auth")
                // 假设用户认证相关 controller 在 com._1.feature_user_management.controller
                // 如果实际包结构不同，请修改
                .packagesToScan("com._1.feature_user_management.controller", "com._1.controller.auth") // 示例，根据实际controller位置调整
                .build();
    }
    
    // Add more groups as needed, for example:
    // @Bean
    // public GroupedOpenApi questionBankApis() {
    //     return GroupedOpenApi.builder()
    //             .group("02-question-bank")
    //             .packagesToScan("com._1.feature_question_bank.controller") // Adjust package as needed
    //             .build();
    // }

} 