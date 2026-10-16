package com.yitinsun.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final AppProperties appProperties;

    public OpenApiConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public OpenAPI urlShortenerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description(
                                "Create a short code from a long HTTP(S) URL. "
                                        + "Redirect (`GET /short/{shortCode}` → 302) is omitted from this UI.")
                        .version("0.1.0")
                        .contact(new Contact().name("yitinsun").url(appProperties.baseUrl()))
                        .license(new License().name("UNLICENSED")))
                .addServersItem(new Server()
                        .url(appProperties.baseUrl())
                        .description("This instance"));
    }
}
