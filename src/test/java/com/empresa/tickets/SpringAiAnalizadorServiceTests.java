package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.empresa.tickets.ai.HeuristicoAnalizadorService;
import com.empresa.tickets.ai.AnalizadorTicketService;
import com.empresa.tickets.ai.SpringAiAnalizadorService;
import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.config.AiConfig;
import com.empresa.tickets.config.ResilienceConfig;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class SpringAiAnalizadorServiceTests {

	@Test
	void mapsStructuredChatClientOutput() {
		AnalisisTicketResponse expected = new AnalisisTicketResponse(CategoriaTicket.SOFTWARE,
			PrioridadTicket.ALTA, "La aplicación falla durante el inicio.", List.of("aplicacion"));
		ChatClient chatClient = chatClientReturning(expected);
		SpringAiAnalizadorService service = new SpringAiAnalizadorService(chatClient,
			new HeuristicoAnalizadorService());

		assertEquals(expected, service.analizar("Error al iniciar", "La aplicación se cierra."));
	}

	@Test
	void retriesChatFailureThenRecoversThroughHeuristicService() {
		ChatClient chatClient = chatClientFailing();

		new ApplicationContextRunner()
			.withUserConfiguration(RetryTestConfiguration.class)
			.withBean(ChatClient.class, () -> chatClient)
			.withBean(HeuristicoAnalizadorService.class, HeuristicoAnalizadorService::new)
			.run(context -> {
				assertNull(context.getStartupFailure(), () -> "Context startup failed: " + context.getStartupFailure());
				AnalizadorTicketService service = context.getBean("springAiAnalizadorService",
					AnalizadorTicketService.class);
				AnalisisTicketResponse response = service.analizar("WiFi caído", "No hay internet en la oficina.");

				assertEquals(CategoriaTicket.CONECTIVIDAD, response.categoria());
				assertEquals(PrioridadTicket.CRITICA, response.prioridad());
				assertEquals("analisis-heuristico", response.etiquetas().getLast());
				assertEquals(1, response.etiquetas().stream().filter("analisis-heuristico"::equals).count());
				verify(chatClient, times(2)).prompt();
			});
	}

	@Test
	void recoversThroughHeuristicServiceWhenChatClientTimesOut() {
		ChatClient chatClient = chatClientFailing(new IllegalStateException("provider timeout",
				new TimeoutException("chat request timed out")));

		new ApplicationContextRunner()
			.withUserConfiguration(RetryTestConfiguration.class)
			.withBean(ChatClient.class, () -> chatClient)
			.withBean(HeuristicoAnalizadorService.class, HeuristicoAnalizadorService::new)
			.run(context -> {
				assertNull(context.getStartupFailure(), () -> "Context startup failed: " + context.getStartupFailure());
				AnalizadorTicketService service = context.getBean("springAiAnalizadorService",
					AnalizadorTicketService.class);
				AnalisisTicketResponse response = service.analizar("WiFi caído", "No hay internet en la oficina.");

				assertEquals(CategoriaTicket.CONECTIVIDAD, response.categoria());
				assertEquals(PrioridadTicket.CRITICA, response.prioridad());
				assertEquals("analisis-heuristico", response.etiquetas().getLast());
				assertEquals(1, response.etiquetas().stream().filter("analisis-heuristico"::equals).count());
				verify(chatClient, times(2)).prompt();
			});
	}

	@Test
	void invalidStructuredOutputIsRetriedAndRecovered() {
		AnalisisTicketResponse tooManyLabels = new AnalisisTicketResponse(CategoriaTicket.OTRO,
			PrioridadTicket.MEDIA, "Solicitud", List.of("a", "b", "c", "d", "e", "f"));
		ChatClient chatClient = chatClientReturning(tooManyLabels);

		new ApplicationContextRunner()
			.withUserConfiguration(RetryTestConfiguration.class)
			.withBean(ChatClient.class, () -> chatClient)
			.withBean(HeuristicoAnalizadorService.class, HeuristicoAnalizadorService::new)
			.run(context -> {
				assertNull(context.getStartupFailure(), () -> "Context startup failed: " + context.getStartupFailure());
				AnalizadorTicketService service = context.getBean("springAiAnalizadorService",
					AnalizadorTicketService.class);
				AnalisisTicketResponse response = service.analizar("Usuario bloqueado", "No puedo ingresar.");

				assertEquals(CategoriaTicket.ACCESOS, response.categoria());
				assertEquals(PrioridadTicket.CRITICA, response.prioridad());
				verify(chatClient, times(2)).prompt();
			});
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
		when(request.user(anyString())).thenReturn(request);
		when(request.call()).thenReturn(call);
		when(call.entity(AnalisisTicketResponse.class)).thenReturn(response);
		return chatClient;
	}

	private ChatClient chatClientFailing() {
		return chatClientFailing(new IllegalStateException("provider timeout"));
	}

	private ChatClient chatClientFailing(RuntimeException failure) {
		ChatClient chatClient = mock(ChatClient.class);
		ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
		ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
		when(chatClient.prompt()).thenReturn(request);
		when(request.system(anyString())).thenReturn(request);
		when(request.user(anyString())).thenReturn(request);
		when(request.call()).thenReturn(call);
		when(call.entity(AnalisisTicketResponse.class)).thenThrow(failure);
		return chatClient;
	}

	@Configuration(proxyBeanMethods = false)
	@Import(ResilienceConfig.class)
	static class RetryTestConfiguration {

		@Bean
		SpringAiAnalizadorService springAiAnalizadorService(ChatClient chatClient,
				HeuristicoAnalizadorService heuristicoAnalizadorService) {
			return new SpringAiAnalizadorService(chatClient, heuristicoAnalizadorService);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OpenAiCommonProperties.class)
	static class AiTimeoutConfiguration {
	}
}
