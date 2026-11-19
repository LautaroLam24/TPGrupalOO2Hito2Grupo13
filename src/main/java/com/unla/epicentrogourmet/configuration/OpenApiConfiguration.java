package com.unla.epicentrogourmet.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI epicentroGourmetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Epicentro Gourmet API")
                        .version("1.0")
                        .description("API REST del Hito 3 - Grupo 13, Orientación a Objetos 2, UNLa"));
    }
}
