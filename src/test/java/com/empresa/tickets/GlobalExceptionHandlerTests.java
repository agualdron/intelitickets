package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTests {

	@Test
	void queryParameterEnumInvalidReturnsCommonBadRequest() {
		var handler = new GlobalExceptionHandler();
		var request = new MockHttpServletRequest("GET", "/api/tickets");
		var exception = new MethodArgumentTypeMismatchException("INVALID", EstadoTicket.class, "estado", null, null);

		var response = handler.tipoDeParametroInvalido(exception, (HttpServletRequest) request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(400, response.getBody().status());
		assertEquals("Bad Request", response.getBody().error());
		assertEquals("/api/tickets", response.getBody().path());
	}
}
