package com.empresa.tickets.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration(proxyBeanMethods = false)
public class ResilienceConfig {

	@Bean
	RetryTemplate aiProviderRetryTemplate(
			@Value("${ai.analysis.retry.max-attempts:2}") int maxAttempts,
			@Value("${ai.analysis.retry.backoff-ms:250}") long backoffMs) {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new TransientProviderRetryPolicy(maxAttempts));

		ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
		long initialBackoffMs = Math.max(1, Math.min(2000, backoffMs));
		backOff.setInitialInterval(initialBackoffMs);
		backOff.setMultiplier(2.0);
		backOff.setMaxInterval(Math.max(2000, initialBackoffMs));
		retryTemplate.setBackOffPolicy(backOff);
		return retryTemplate;
	}

	private static final class TransientProviderRetryPolicy implements RetryPolicy {

		private final int maxAttempts;

		private TransientProviderRetryPolicy(int maxAttempts) {
			this.maxAttempts = Math.min(5, Math.max(1, maxAttempts));
		}

		@Override
		public boolean canRetry(RetryContext context) {
			return context.getRetryCount() < maxAttempts
				&& (context.getLastThrowable() == null || esTransitorio(context.getLastThrowable()));
		}

		@Override
		public RetryContext open(RetryContext parent) {
			return new RetryContextSupport(parent);
		}

		@Override
		public void close(RetryContext context) {
		}

		@Override
		public void registerThrowable(RetryContext context, Throwable throwable) {
			((RetryContextSupport) context).registerThrowable(throwable);
		}

		private boolean esTransitorio(Throwable throwable) {
			return TransientProviderFailureClassifier.isTransient(throwable);
		}
	}
}
