package com.empresa.tickets.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<ErrorResponse> recursoNoEncontrado(RecursoNoEncontradoException exception,
			HttpServletRequest request) {
		return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler({ReglaNegocioException.class, HttpMessageNotReadableException.class})
	public ResponseEntity<ErrorResponse> solicitudInvalida(Exception exception, HttpServletRequest request) {
		String message = exception instanceof HttpMessageNotReadableException
			? "El cuerpo de la solicitud no es válido."
			: exception.getMessage();
		return respuesta(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> argumentosInvalidos(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.map(this::mensajeCampo)
			.collect(Collectors.joining("; "));
		return respuesta(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> tipoDeParametroInvalido(MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		String message = "Parámetro '" + exception.getName() + "' inválido";
		return respuesta(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler({AccessDeniedException.class, AccesoDenegadoTicketException.class})
	public ResponseEntity<ErrorResponse> accesoDenegado(Exception exception, HttpServletRequest request) {
		return respuesta(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler({BadCredentialsException.class, AuthenticationCredentialsNotFoundException.class})
	public ResponseEntity<ErrorResponse> credencialesInvalidas(Exception exception,
			HttpServletRequest request) {
		return respuesta(HttpStatus.UNAUTHORIZED, "Autenticación requerida o inválida.", request);
	}

	@ExceptionHandler(ProveedorAnalisisTemporalmenteIndisponibleException.class)
	public ResponseEntity<ErrorResponse> proveedorNoDisponible(
			ProveedorAnalisisTemporalmenteIndisponibleException exception, HttpServletRequest request) {
		LOGGER.error("El proveedor de análisis no está disponible y falló la recuperación local.", exception);
		return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de análisis no está disponible temporalmente.",
			request);
	}

	@ExceptionHandler(AiAnalisisException.class)
	public ResponseEntity<ErrorResponse> errorAnalisis(AiAnalisisException exception, HttpServletRequest request) {
		LOGGER.error("Falló el análisis del ticket.", exception);
		return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible completar el análisis del ticket.", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> errorInterno(Exception exception, HttpServletRequest request) {
		LOGGER.error("Error inesperado al procesar la solicitud.", exception);
		return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Se produjo un error interno.", request);
	}

	private String mensajeCampo(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String message, HttpServletRequest request) {
		ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
