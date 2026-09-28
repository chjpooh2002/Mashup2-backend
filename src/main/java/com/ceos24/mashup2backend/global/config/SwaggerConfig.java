package com.ceos24.mashup2backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swagger(){

        // Swagger UI 상단 설명
        Info info = new Info().title("CEOS24 MashUp 2팀").description("센트비 API 문서").version("0.0.1");

        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/").description("API 서버"));
    }
}
