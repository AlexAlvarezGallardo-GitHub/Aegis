package com.aegis.wallet.infrastructure.config;

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
    public OpenAPI aegisWalletOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aegis Wallet Service API")
                        .description("Wallet management API for the Aegis digital payment platform. Handles wallet creation, balance queries, and ledger tracking.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Aegis Platform Team")
                                .url("https://github.com/AlexAlvarezGallardo-GitHub/Aegis"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Local development server")));
    }
}
