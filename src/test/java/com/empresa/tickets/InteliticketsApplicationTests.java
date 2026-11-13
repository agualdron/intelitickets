package com.empresa.tickets;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
	"security.jwt.secret=phase-two-test-secret-with-at-least-32-bytes",
	"security.jwt.expiration-ms=86400000"
})
@Testcontainers
@AutoConfigureMockMvc
@Import(InteliticketsApplicationTests.RoleProbe.class)
class InteliticketsApplicationTests {

	private static final String JWT_SECRET = "phase-two-test-secret-with-at-least-32-bytes";

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void seededAccountsCanLogInAndUseTheirJwt() throws Exception {
		Map<String, String> accounts = Map.of(
			"admin@empresa.com", "ADMIN",
			"soporte1@empresa.com", "SOPORTE",
			"soporte2@empresa.com", "SOPORTE",
			"laura.gomez@empresa.com", "USUARIO");

		for (Map.Entry<String, String> account : accounts.entrySet()) {
			String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("correo", account.getKey(), "password", "Password123!"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.correo").value(account.getKey()))
			.andExpect(jsonPath("$.rol").value(account.getValue()))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andReturn().getResponse().getContentAsString();
			String token = objectMapper.readTree(response).get("token").asText();

			mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
			mockMvc.perform(get("/test/principal").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(account.getKey()))
				.andExpect(jsonPath("$.authorities[0]").value("ROLE_" + account.getValue()));
			if (account.getValue().equals("ADMIN")) {
				mockMvc.perform(get("/test/admin-only").header("Authorization", "Bearer " + token))
					.andExpect(status().isOk());
			}
			else {
				mockMvc.perform(get("/test/admin-only").header("Authorization", "Bearer " + token))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.status").value(403))
					.andExpect(jsonPath("$.path").value("/test/admin-only"));
			}
		}
	}

	@Test
	void registrationCreatesUserWithUserRoleAndReturnsUsableTokenWithoutPassword() throws Exception {
		String registration = """
			{"nombre":"Nueva Persona","correo":"  nueva.persona@empresa.com ","password":"ClaveSegura123"}
			""";
		String response = mockMvc.perform(post("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.content(registration))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rol").value("USUARIO"))
			.andExpect(jsonPath("$.correo").value("nueva.persona@empresa.com"))
			.andExpect(jsonPath("$.token", not(blankOrNullString())))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andReturn().getResponse().getContentAsString();
		String token = objectMapper.readTree(response).get("token").asText();
		mockMvc.perform(get("/hello").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.path").value("/api/auth/register"));
	}

	@Test
	void protectedRoutesRejectMissingMalformedExpiredAndTamperedTokens() throws Exception {
		mockMvc.perform(get("/hello"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.path").value("/hello"));
		mockMvc.perform(get("/hello").header("Authorization", "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/hello").header("Authorization", "Bearer "
			+ tokenWithExpiration(System.currentTimeMillis() - 1000)))
			.andExpect(status().isUnauthorized());

		String response = mockMvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"admin@empresa.com\",\"password\":\"Password123!\"}"))
			.andReturn().getResponse().getContentAsString();
		String[] tokenParts = objectMapper.readTree(response).get("token").asText().split("\\.");
		tokenParts[2] = (tokenParts[2].startsWith("A") ? "B" : "A") + tokenParts[2].substring(1);
		mockMvc.perform(get("/hello").header("Authorization", "Bearer " + String.join(".", tokenParts)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticationRoutesArePublicAndInvalidCredentialsUseCommonErrorBody() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"admin@empresa.com\",\"password\":\"incorrecta123\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("Unauthorized"))
			.andExpect(jsonPath("$.path").value("/api/auth/login"));
	}

	private String tokenWithExpiration(long expiration) {
		long now = System.currentTimeMillis() - 10_000;
		return Jwts.builder().subject("admin@empresa.com").claim("role", "ADMIN")
			.issuedAt(new Date(now)).expiration(new Date(expiration))
			.signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8))).compact();
	}

	@RestController
	static class RoleProbe {

		@GetMapping("/test/principal")
		Map<String, Object> principal(Authentication authentication) {
			return Map.of("name", authentication.getName(), "authorities",
				authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
		}

		@GetMapping("/test/admin-only")
		@PreAuthorize("hasRole('ADMIN')")
		String adminOnly() {
			return "admin";
		}
	}
}
