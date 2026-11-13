package com.empresa.tickets.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import com.empresa.tickets.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtService(@Value("${security.jwt.secret:}") String secret,
			@Value("${security.jwt.expiration-ms:86400000}") long expirationMs) {
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secret.isBlank() || secretBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
		}
		if (expirationMs <= 0) {
			throw new IllegalStateException("JWT_EXPIRATION_MS must be greater than zero");
		}
		this.signingKey = Keys.hmacShaKeyFor(secretBytes);
		this.expirationMs = expirationMs;
	}

	public String generarToken(Usuario usuario) {
		Date now = new Date();
		return Jwts.builder()
			.subject(usuario.getCorreo())
			.claim("role", usuario.getRol().name())
			.issuedAt(now)
			.expiration(new Date(now.getTime() + expirationMs))
			.signWith(signingKey)
			.compact();
	}

	public String extraerCorreo(String token) {
		return extraerClaims(token).getSubject();
	}

	public String extraerRol(String token) {
		return extraerClaims(token).get("role", String.class);
	}

	public boolean esValido(String token) {
		try {
			extraerClaims(token);
			return true;
		}
		catch (RuntimeException exception) {
			return false;
		}
	}

	private Claims extraerClaims(String token) {
		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}
}
