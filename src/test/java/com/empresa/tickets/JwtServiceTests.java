package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class JwtServiceTests {

	private static final String SECRET = "phase-two-test-secret-with-at-least-32-bytes";

	@Test
	void generatesAndValidatesTokenWithCorreoAndRole() {
		JwtService jwtService = new JwtService(SECRET, 60_000);
		Usuario usuario = new Usuario("Administrador", "admin@empresa.com", "hash", Rol.ADMIN);

		String token = jwtService.generarToken(usuario);

		assertTrue(jwtService.esValido(token));
		assertEquals("admin@empresa.com", jwtService.extraerCorreo(token));
		assertEquals("ADMIN", jwtService.extraerRol(token));
	}

	@Test
	void rejectsExpiredAndTamperedTokens() {
		JwtService jwtService = new JwtService(SECRET, 60_000);
		long now = System.currentTimeMillis() - 10_000;
		String expired = Jwts.builder().subject("admin@empresa.com").issuedAt(new Date(now))
			.expiration(new Date(now + 1_000))
			.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
		String valid = jwtService.generarToken(new Usuario("Admin", "admin@empresa.com", "hash", Rol.ADMIN));
		String[] parts = valid.split("\\.");
		parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);

		assertFalse(jwtService.esValido(expired));
		assertFalse(jwtService.esValido(String.join(".", parts)));
	}

	@Test
	void rejectsInvalidJwtConfiguration() {
		assertStartupRejected("", 60_000);
		assertStartupRejected("too-short", 60_000);
		assertStartupRejected(SECRET, 0);
	}

	private void assertStartupRejected(String secret, long expirationMs) {
		new ApplicationContextRunner()
			.withUserConfiguration(JwtConfiguration.class)
			.withPropertyValues("security.jwt.secret=" + secret,
				"security.jwt.expiration-ms=" + expirationMs)
			.run(context -> {
				Throwable failure = context.getStartupFailure();
				assertNotNull(failure);
				while (failure != null && !(failure instanceof IllegalStateException)) {
					failure = failure.getCause();
				}
				assertTrue(failure instanceof IllegalStateException);
			});
	}

	@Configuration(proxyBeanMethods = false)
	@Import(JwtService.class)
	static class JwtConfiguration {
	}
}
