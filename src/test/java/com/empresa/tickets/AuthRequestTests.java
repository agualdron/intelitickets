package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.empresa.tickets.dto.auth.LoginRequest;
import com.empresa.tickets.dto.auth.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class AuthRequestTests {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void trimsCorreoBeforeEmailValidation() {
		RegisterRequest registration = new RegisterRequest("Nueva Persona", "  nueva.persona@empresa.com ",
			"ClaveSegura123");
		LoginRequest login = new LoginRequest("  nueva.persona@empresa.com ", "ClaveSegura123");

		assertEquals("nueva.persona@empresa.com", registration.correo());
		assertEquals("nueva.persona@empresa.com", login.correo());
		assertTrue(validator.validate(registration).isEmpty());
		assertTrue(validator.validate(login).isEmpty());
	}
}
