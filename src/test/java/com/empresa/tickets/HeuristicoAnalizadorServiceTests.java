package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.empresa.tickets.ai.HeuristicoAnalizadorService;
import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import org.junit.jupiter.api.Test;

class HeuristicoAnalizadorServiceTests {

	private final HeuristicoAnalizadorService service = new HeuristicoAnalizadorService();

	@Test
	void classifiesConnectivityAndUrgentMeetingWithAccentsAndCaseIgnored() {
		AnalisisTicketResponse response = service.analizar("WIFI caído", "Tengo una reunión URGENTE y no hay internet.");

		assertEquals(CategoriaTicket.CONECTIVIDAD, response.categoria());
		assertEquals(PrioridadTicket.CRITICA, response.prioridad());
		assertTrue(response.etiquetas().contains("analisis-heuristico"));
		assertTrue(response.etiquetas().size() <= 5);
	}

	@Test
	void mapsUrgentMeetingToHighAndBlockedAccessToCritical() {
		assertEquals(PrioridadTicket.ALTA, service.analizar("Reunión urgente", "Se requiere atención.").prioridad());

		AnalisisTicketResponse blocked = service.analizar("Usuario bloqueado", "No puede ingresar a la aplicación.");
		assertEquals(CategoriaTicket.ACCESOS, blocked.categoria());
		assertEquals(PrioridadTicket.CRITICA, blocked.prioridad());
	}

	@Test
	void returnsDeterministicDefaultsAndConciseSummary() {
		AnalisisTicketResponse response = service.analizar("Consulta general", "Necesito ayuda con una solicitud.");

		assertEquals(CategoriaTicket.OTRO, response.categoria());
		assertEquals(PrioridadTicket.BAJA, response.prioridad());
		assertEquals("Consulta general. Necesito ayuda con una solicitud.", response.resumen());
	}

	@Test
	void capsSummaryAndKeepsMandatoryLabelAsLastLabelWhenFull() {
		String longDescription = "internet wifi red urgente reunion caido bloqueado ".repeat(30);
		AnalisisTicketResponse response = service.analizar("Incidente", longDescription);

		assertEquals(500, response.resumen().length());
		assertEquals(5, response.etiquetas().size());
		assertEquals(response.etiquetas().size(), response.etiquetas().stream().distinct().count());
		assertEquals(1, response.etiquetas().stream().filter("analisis-heuristico"::equals).count());
		assertEquals("analisis-heuristico", response.etiquetas().getLast());
	}
}
