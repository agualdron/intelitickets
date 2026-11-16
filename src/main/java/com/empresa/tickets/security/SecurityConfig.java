package com.empresa.tickets.security;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.empresa.tickets.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	DaoAuthenticationProvider daoAuthenticationProvider(CustomUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	UrlBasedCorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origin}") String allowedOrigin) {
		var cors = new CorsConfiguration();
		cors.setAllowedOrigins(List.of(validarAllowedOrigin(allowedOrigin)));
		cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		cors.setAllowCredentials(false);
		cors.setMaxAge(3600L);

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

	private static String validarAllowedOrigin(String allowedOrigin) {
		if (allowedOrigin == null || allowedOrigin.isBlank()) {
			throw new IllegalStateException("CORS_ALLOWED_ORIGIN debe contener un origin HTTP(S) válido");
		}
		try {
			URI origin = URI.create(allowedOrigin);
			String scheme = origin.getScheme();
			boolean httpScheme = scheme != null
					&& (scheme.toLowerCase(Locale.ROOT).equals("http") || scheme.toLowerCase(Locale.ROOT).equals("https"));
			boolean hasNoPathOrSuffix = (origin.getRawPath() == null || origin.getRawPath().isEmpty())
					&& origin.getRawQuery() == null && origin.getRawFragment() == null;
			if (!origin.isAbsolute() || !httpScheme || origin.getHost() == null || origin.getRawUserInfo() != null
					|| !hasNoPathOrSuffix) {
				throw new IllegalStateException("CORS_ALLOWED_ORIGIN debe ser un origin HTTP(S), sin ruta ni wildcard");
			}
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("CORS_ALLOWED_ORIGIN debe contener un origin HTTP(S) válido", exception);
		}
		return allowedOrigin;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			CustomUserDetailsService userDetailsService, DaoAuthenticationProvider daoAuthenticationProvider,
			ObjectMapper objectMapper, UrlBasedCorsConfigurationSource corsConfigurationSource) throws Exception {
		JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.authenticationProvider(daoAuthenticationProvider)
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint((request, response, exception) -> writeError(objectMapper, request, response,
						HttpStatus.UNAUTHORIZED, "Autenticación requerida"))
				.accessDeniedHandler((request, response, exception) -> writeError(objectMapper, request, response,
						HttpStatus.FORBIDDEN, "Acceso denegado")))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.anyRequest().authenticated())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	private void writeError(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
			HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(LocalDateTime.now(), status.value(),
			status.getReasonPhrase(), message, request.getRequestURI()));
	}
}
