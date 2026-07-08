package com.template.shared.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Kotlin Spring Modulith Template API")
                .description("모듈러 모놀리스 템플릿 REST API")
                .version("v1"),
        )
}
