package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.empresa.tickets.ai.HeuristicoAnalizadorService;
import com.empresa.tickets.ai.SpringAiAnalizadorService;
import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.config.AiConfig;
import com.empresa.tickets.config.ResilienceConfig;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.exception.AiAnalisisException;
import com.empresa.tickets.exception.ProveedorAnalisisTemporalmenteIndisponibleException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.support.RetryTemplate;

class SpringAiAnalizadorServiceTests {

	@Test
	void mapsAndNormalizesStructuredChatClientOutput() {
		AnalisisTicketResponse output = new AnalisisTicketResponse(CategoriaTicket.SOFTWARE,
			PrioridadTicket.ALTA, "La aplicación falla durante el inicio.", List.of("Conexión Wi_Fi", "conexion-wi-fi"));
		ChatClient chatClient = chatClientReturning(output);
		SpringAiAnalizadorService service = new SpringAiAnalizadorService(chatClient,
			new HeuristicoAnalizadorService(), new RetryTemplate());

		AnalisisTicketResponse response = service.analizar("Error al iniciar", "La aplicación se cierra.");

		assertEquals(CategoriaTicket.SOFTWARE, response.categoria());
		assertEquals(List.of("conexion-wi-fi"), response.etiquetas());
	}

	@Test
	void retriesOnlyTransientProviderFailuresThenRecoversThroughHeuristicService() {
		ChatClient chatClient = chatClientFailing(new IllegalStateException("provider timeout",
			new TimeoutException("chat request timed out")));

		new ApplicationContextRunner()
			.withUserConfiguration(RetryTestConfiguration.class)
			.withPropertyValues("ai.analysis.retry.max-attempts=2", "ai.analysis.retry.backoff-ms=1")
			.withBean(ChatClient.class, () -> chatClient)
			.withBean(HeuristicoAnalizadorService.class, HeuristicoAnalizadorService::new)
			.run(context -> {
				assertNull(context.getStartupFailure(), () -> "Context startup failed: " + context.getStartupFailure());
				SpringAiAnalizadorService service = context.getBean(SpringAiAnalizadorService.class);
				AnalisisTicketResponse response = service.analizar("WiFi caído", "No hay internet en la oficina.");

				assertEquals(CategoriaTicket.CONECTIVIDAD, response.categoria());
				assertEquals(PrioridadTicket.CRITICA, response.prioridad());
				assertEquals("analisis-heuristico", response.etiquetas().getLast());
				verify(chatClient, times(2)).prompt();
			});
	}

	@Test
	void doesNotRetryPermanentProviderFailures() {
		ChatClient chatClient = chatClientFailing(new IllegalStateException("invalid provider configuration"));

		new ApplicationContextRunner()
			.withUserConfiguration(RetryTestConfiguration.class)
			.withPropertyValues("ai.analysis.retry.max-attempts=3", "ai.analysis.retry.backoff-ms=1")
			.withBean(ChatClient.class, () -> chatClient)
			.withBean(HeuristicoAnalizadorService.class, HeuristicoAnalizadorService::new)
			.run(context -> {
				SpringAiAnalizadorService service = context.getBean(SpringAiAnalizadorService.class);
				AnalisisTicketResponse response = service.analizar("WiFi caído", "No hay internet.");
				assertTrue(response.etiquetas().contains("analisis-heuristico"));
				verify(chatClient).prompt();
			});
	}

	@Test
	void invalidProviderOutputFallsBackImmediatelyWithoutRetry() {
		AnalisisTicketResponse invalid = new AnalisisTicketResponse(CategoriaTicket.OTRO, PrioridadTicket.MEDIA,
			"Solicitud válida", List.of());
		ChatClient chatClient = chatClientReturning(invalid);

		new ApplicationContextRunner()
			.withUserConfiguration(RetryTestConfiguration.class)
			.withPropertyValues("ai.analysis.retry.max-attempts=3", "ai.analysis.retry.backoff-ms=1")
			.withBean(ChatClient.class, () -> chatClient)
			.withBean(HeuristicoAnalizadorService.class, HeuristicoAnalizadorService::new)
			.run(context -> {
				SpringAiAnalizadorService service = context.getBean(SpringAiAnalizadorService.class);
				AnalisisTicketResponse response = service.analizar("Usuario bloqueado", "No puedo ingresar.");

				assertEquals(CategoriaTicket.ACCESOS, response.categoria());
				assertEquals(PrioridadTicket.CRITICA, response.prioridad());
				assertTrue(response.etiquetas().contains("analisis-heuristico"));
				verify(chatClient).prompt();
			});
	}

	@Test
	void nullProviderOutputFallsBackAndFallbackFailureIsSafe() {
		ChatClient chatClient = chatClientReturning(null);
		HeuristicoAnalizadorService brokenHeuristic = mock(HeuristicoAnalizadorService.class);
		when(brokenHeuristic.analizar(anyString(), anyString())).thenThrow(new IllegalStateException("secret detail"));
		SpringAiAnalizadorService service = new SpringAiAnalizadorService(chatClient, brokenHeuristic,
			new RetryTemplate());

		org.junit.jupiter.api.Assertions.assertThrows(AiAnalisisException.class,
			() -> service.analizar("Ticket", "Descripción"));
		verify(chatClient).prompt();
		verify(brokenHeuristic).analizar("Ticket", "Descripción");
	}

	@Test
	void exhaustedTransientProviderAndFallbackFailureUsesUnavailableException() {
		ChatClient chatClient = chatClientFailing(new IllegalStateException("provider timeout",
			new TimeoutException("chat request timed out")));
		HeuristicoAnalizadorService brokenHeuristic = mock(HeuristicoAnalizadorService.class);
		when(brokenHeuristic.analizar(anyString(), anyString())).thenThrow(new IllegalStateException("secret detail"));

		new ApplicationContextRunner()
			.withUserConfiguration(RetryTestConfiguration.class)
			.withPropertyValues("ai.analysis.retry.max-attempts=2", "ai.analysis.retry.backoff-ms=1")
			.withBean(ChatClient.class, () -> chatClient)
			.withBean(HeuristicoAnalizadorService.class, () -> brokenHeuristic)
			.run(context -> {
				SpringAiAnalizadorService service = context.getBean(SpringAiAnalizadorService.class);
				org.junit.jupiter.api.Assertions.assertThrows(ProveedorAnalisisTemporalmenteIndisponibleException.class,
					() -> service.analizar("Ticket", "Descripción"));
				verify(chatClient, times(2)).prompt();
			});
	}

	@Test
	void promptHasVersionedRulesAndSeparatesTicketFields() {
		ChatClient chatClient = chatClientReturning(new AnalisisTicketResponse(CategoriaTicket.SOFTWARE,
			PrioridadTicket.MEDIA, "Error de aplicación", List.of("software")));
		ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
		ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
		when(chatClient.prompt()).thenReturn(request);
		when(request.system(anyString())).thenReturn(request);
		when(request.user(any(Consumer.class))).thenReturn(request);
		when(request.call()).thenReturn(call);
		when(call.entity(AnalisisTicketResponse.class)).thenReturn(new AnalisisTicketResponse(CategoriaTicket.SOFTWARE,
			PrioridadTicket.MEDIA, "Error de aplicación", List.of("software")));
		new SpringAiAnalizadorService(chatClient, new HeuristicoAnalizadorService(), new RetryTemplate())
			.analizar("Título", "Descripción");

		org.mockito.ArgumentCaptor<String> systemPrompt = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.ArgumentCaptor<Consumer<PromptUserSpec>> userPrompt = org.mockito.ArgumentCaptor.forClass(Consumer.class);
		verify(request).system(systemPrompt.capture());
		verify(request).user(userPrompt.capture());
		assertTrue(systemPrompt.getValue().contains("ticket-analysis-v1"));
		assertTrue(systemPrompt.getValue().contains("CONECTIVIDAD"));
		assertTrue(systemPrompt.getValue().contains("2 a 30"));

		PromptUserSpec userSpec = mock(PromptUserSpec.class);
		when(userSpec.text(anyString())).thenReturn(userSpec);
		when(userSpec.param(anyString(), any())).thenReturn(userSpec);
		userPrompt.getValue().accept(userSpec);
		org.mockito.ArgumentCaptor<String> userTemplate = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(userSpec).text(userTemplate.capture());
		assertTrue(userTemplate.getValue().contains("<TITULO>{titulo}</TITULO>"));
		assertTrue(userTemplate.getValue().contains("<DESCRIPCION>{descripcion}</DESCRIPCION>"));
	}

	@Test
	void bindsConfiguredAiTimeoutFromEnvironmentVariable() {
		new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(AiTimeoutConfiguration.class)
			.withPropertyValues("AI_TIMEOUT_MS=2750")
			.run(context -> {
				OpenAiCommonProperties properties = context.getBean(OpenAiCommonProperties.class);
				assertEquals(Duration.ofMillis(2750), properties.getTimeout());
			});
	}

	@Test
	void createsChatClientFromSpringAiAutoConfiguredBuilder() {
		ChatClient.Builder builder = mock(ChatClient.Builder.class);
		ChatClient expected = mock(ChatClient.class);
		when(builder.build()).thenReturn(expected);

		new ApplicationContextRunner()
			.withUserConfiguration(AiConfig.class)
			.withBean(ChatClient.Builder.class, () -> builder)
			.run(context -> assertEquals(expected, context.getBean(ChatClient.class)));
	}

	private ChatClient chatClientReturning(AnalisisTicketResponse response) {
		ChatClient chatClient = mock(ChatClient.class);
		ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
		ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
		when(chatClient.prompt()).thenReturn(request);
		when(request.system(anyString())).thenReturn(request);
		when(request.user(any(Consumer.class))).thenReturn(request);
		when(request.call()).thenReturn(call);
		when(call.entity(AnalisisTicketResponse.class)).thenReturn(response);
		return chatClient;
	}

	private ChatClient chatClientFailing(RuntimeException failure) {
		ChatClient chatClient = mock(ChatClient.class);
		ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
		ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
		when(chatClient.prompt()).thenReturn(request);
		when(request.system(anyString())).thenReturn(request);
		when(request.user(any(Consumer.class))).thenReturn(request);
		when(request.call()).thenReturn(call);
		when(call.entity(AnalisisTicketResponse.class)).thenThrow(failure);
		return chatClient;
	}

	@Configuration(proxyBeanMethods = false)
	@Import(ResilienceConfig.class)
	static class RetryTestConfiguration {

		@Bean
		SpringAiAnalizadorService springAiAnalizadorService(ChatClient chatClient,
				HeuristicoAnalizadorService heuristicoAnalizadorService, RetryTemplate retryTemplate) {
			return new SpringAiAnalizadorService(chatClient, heuristicoAnalizadorService, retryTemplate);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OpenAiCommonProperties.class)
	static class AiTimeoutConfiguration {
	}
}
