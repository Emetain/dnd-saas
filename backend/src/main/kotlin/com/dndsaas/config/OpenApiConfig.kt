package com.dndsaas.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI / Swagger configuration.
 *
 * Swagger UI is served at:  http://localhost:8080/api/swagger-ui.html
 * Raw OpenAPI JSON at:      http://localhost:8080/api/v3/api-docs
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun dndSaasOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("D&D SaaS API")
                    .description(
                        "AI-powered campaign management platform for Dungeon Masters. " +
                            "Manage campaigns, sessions and campaign memory (NPCs, locations, " +
                            "quests, items) with AI generators that use campaign context.",
                    )
                    .version("v0.0.1")
                    .contact(Contact().name("D&D SaaS"))
                    .license(License().name("Proprietary")),
            )
}

