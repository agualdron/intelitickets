package com.empresa.tickets.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI ticketsOpenApi() {
		return new OpenAPI()
				.info(new Info().title("InteliTickets API").description("API de gestión inteligente de tickets")
						.version("1.0.0"))
				.components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
						.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
	}
}
