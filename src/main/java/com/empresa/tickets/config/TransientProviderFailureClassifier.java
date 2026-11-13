package com.empresa.tickets.config;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

public final class TransientProviderFailureClassifier {

	private TransientProviderFailureClassifier() {
	}

	public static boolean isTransient(Throwable throwable) {
		for (Throwable actual = throwable; actual != null; actual = actual.getCause()) {
			if (actual instanceof ResourceAccessException || actual instanceof HttpServerErrorException
					|| actual instanceof SocketTimeoutException || actual instanceof SocketException
					|| actual instanceof IOException || actual instanceof TimeoutException) {
				return true;
			}
			if (actual instanceof RestClientResponseException responseException) {
				int status = responseException.getStatusCode().value();
				if (status == 429 || status >= 500) {
					return true;
				}
			}
		}
		return false;
	}
}
