package com.silvaldeweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI silvaldeWebOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Silvalde Web API")
                        .description("REST API for the Silvalde Web store and admin panel. "
                                + "All endpoints require a valid JWT bearer token except "
                                + "POST /api/auth/login and the /actuator/health probes.")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Silvalde Web")
                                .url("https://github.com/anomalyco/silvaldeWeb"))
                        .license(new License().name("Private")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT returned by POST /api/auth/login")));
    }
}
