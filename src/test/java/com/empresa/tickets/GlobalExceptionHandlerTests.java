package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.exception.AiAnalisisException;
import com.empresa.tickets.exception.GlobalExceptionHandler;
import com.empresa.tickets.exception.ProveedorAnalisisTemporalmenteIndisponibleException;
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

	@Test
	void unexpectedErrorUsesSafeGenericMessage() {
		var handler = new GlobalExceptionHandler();
		var request = new MockHttpServletRequest("GET", "/api/tickets");
		var response = handler.errorInterno(new IllegalStateException("database password leaked"), request);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("Se produjo un error interno.", response.getBody().message());
		assertFalse(response.getBody().message().contains("password"));
	}

	@Test
	void analysisErrorsUseSafeStatusSpecificMessages() {
		var handler = new GlobalExceptionHandler();
		var request = new MockHttpServletRequest("POST", "/api/tickets");
		var invalid = handler.errorAnalisis(new AiAnalisisException("provider secret"), request);
		var unavailable = handler.proveedorNoDisponible(
			new ProveedorAnalisisTemporalmenteIndisponibleException(new IllegalStateException("provider secret")), request);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, invalid.getStatusCode());
		assertEquals("No fue posible completar el análisis del ticket.", invalid.getBody().message());
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatusCode());
		assertEquals("El servicio de análisis no está disponible temporalmente.", unavailable.getBody().message());
	}
}
