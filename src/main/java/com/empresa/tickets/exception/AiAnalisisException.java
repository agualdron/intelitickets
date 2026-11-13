package com.empresa.tickets.exception;

public class AiAnalisisException extends RuntimeException {

	public AiAnalisisException(String message) {
		super(message);
	}

	public AiAnalisisException(String message, Throwable cause) {
		super(message, cause);
	}
}
