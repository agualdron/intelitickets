package com.empresa.tickets.dto.auth;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Email @Size(max = 150) String correo,
		@NotBlank @Size(min = 8, max = 72) String password) {

	public LoginRequest {
		correo = correo == null ? null : correo.trim();
	}

	@AssertTrue(message = "password debe ocupar entre 8 y 72 bytes UTF-8")
	public boolean isPasswordEncodingLengthValid() {
		return password != null && password.getBytes(StandardCharsets.UTF_8).length >= 8
			&& password.getBytes(StandardCharsets.UTF_8).length <= 72;
	}
}
