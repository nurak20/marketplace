package com.oeurn_nurak.marketplace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI marketplaceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nurak MK27 Marketplace API")
                        .version("v1")
                        .description("Marketplace, management, payroll, and reporting endpoints. "
                                + "Call POST /api/v1/auth/login, then click Authorize and paste the "
                                + "returned token to try protected endpoints."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
