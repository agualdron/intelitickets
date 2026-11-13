package com.empresa.tickets.ai;

import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.exception.AiAnalisisException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SpringAiAnalizadorService implements AnalizadorTicketService {

	private final ChatClient chatClient;
	private final HeuristicoAnalizadorService heuristicoAnalizadorService;

	public SpringAiAnalizadorService(ChatClient chatClient,
			HeuristicoAnalizadorService heuristicoAnalizadorService) {
		this.chatClient = chatClient;
		this.heuristicoAnalizadorService = heuristicoAnalizadorService;
	}

	@Override
	@Retryable(retryFor = Exception.class, maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public AnalisisTicketResponse analizar(String titulo, String descripcion) {
		AnalisisTicketResponse response = chatClient.prompt()
			.system("Analiza solicitudes de soporte técnico. Devuelve solo una clasificación estructurada. "
				+ "categoria debe ser uno de HARDWARE, SOFTWARE, CONECTIVIDAD, ACCESOS, OTRO; "
				+ "prioridad debe ser uno de BAJA, MEDIA, ALTA, CRITICA. "
				+ "criterios de categoría: CONECTIVIDAD (red, internet, Wi-Fi, VPN, latencia), ACCESOS (credenciales, contraseñas, permisos, bloqueos de cuenta, MFA), HARDWARE (fallos físicos, equipos, periféricos, energía, impresoras), SOFTWARE (errores de aplicaciones, SO, instalaciones, licencias, ofimática), OTRO (consultas administrativas o casos generales no técnicos). "
				+ "criterios de prioridad: CRITICA (parada total, impacto masivo a múltiples usuarios o brecha de seguridad), ALTA (impide trabajar a un usuario o afecta una entrega/reunión inminente), MEDIA (incidente operativo puntual donde el usuario aún puede trabajar parcialmente), BAJA (consultas, dudas, mejoras o tareas planificadas sin urgencia). "
				+ "Escribe un resumen breve y no más de cinco etiquetas útiles.")
			.user("Título: " + valor(titulo) + "\nDescripción: " + valor(descripcion))
			.call()
			.entity(AnalisisTicketResponse.class);

		if (response == null || response.resumen().isBlank() || response.etiquetas().size() > 5) {
			throw new AiAnalisisException("El proveedor devolvió un análisis incompleto o inválido.");
		}
		return response;
	}

	@Recover
	public AnalisisTicketResponse recuperar(Exception exception, String titulo, String descripcion) {
		return heuristicoAnalizadorService.analizar(titulo, descripcion);
	}

	private String valor(String texto) {
		return texto == null ? "" : texto;
	}
}
