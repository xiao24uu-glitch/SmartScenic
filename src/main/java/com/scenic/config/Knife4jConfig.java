package com.scenic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("景区门票销售及入场系统 API")
                        .version("1.0.0")
                        .description("基于SpringBoot 3 + Vue 3的景区门票销售及入场系统")
                        .contact(new Contact()
                                .name("SmartScenic")
                        ));
    }
}
