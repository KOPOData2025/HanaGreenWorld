package com.kopo.hanacard.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("하나카드 API")
                        .description("하나카드 서버 - 카드, 하나머니 관리 API\n\n" +
                                "## 인증 방법\n" +
                                "1. **내부 서비스 인증**: `X-Internal-Service: true` 헤더 사용\n" +
                                "2. **고객 정보 인증**: `X-Customer-Id` 헤더에 고객 ID 포함\n" +
                                "3. **통합 API**: 하나그린세상 서버에서 호출하는 API (인증 불필요)")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("하나카드 개발팀")
                                .email("hanacard@kopo.ac.kr")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("하나카드 서버 (로컬)"),
                        new Server().url("http://localhost:8080").description("통합 서버 (하나그린세상)")
                ))
                .components(new Components()
                        .addSecuritySchemes("InternalServiceAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Internal-Service")
                                        .description("내부 서비스 인증 헤더 (값: true)"))
                        .addSecuritySchemes("CustomerInfoAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Customer-Id")
                                        .description("고객 ID 헤더 (예: 1, 2, 3)")))
                .addSecurityItem(new SecurityRequirement().addList("InternalServiceAuth"))
                .addSecurityItem(new SecurityRequirement().addList("CustomerInfoAuth"));
    }
}








