package com.empresa.tickets.exception;

public class ProveedorAnalisisTemporalmenteIndisponibleException extends AiAnalisisException {

	public ProveedorAnalisisTemporalmenteIndisponibleException(Throwable cause) {
		super("El análisis de tickets no está disponible temporalmente.", cause);
	}
}
