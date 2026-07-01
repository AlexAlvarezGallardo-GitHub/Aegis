package com.aegis.identity.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("dev")
public class SwaggerConfig {

    @Bean
    public OpenAPI aegisIdentityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aegis Identity Service API")
                        .description("User registration, authentication, and identity management for the Aegis digital payment platform.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Aegis Platform Team")
                                .url("https://github.com/AlexAlvarezGallardo-GitHub/Aegis"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local development server")));
    }
}
