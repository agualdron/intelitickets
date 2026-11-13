package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import com.empresa.tickets.ai.AnalisisTicketValidator;
import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.exception.AiAnalisisException;
import org.junit.jupiter.api.Test;

class AnalisisTicketValidatorTests {

	@Test
	void normalizesTagsDeduplicatesPreservesRelevanceAndCapsAtFive() {
		var result = AnalisisTicketValidator.validar(new AnalisisTicketResponse(CategoriaTicket.CONECTIVIDAD,
			PrioridadTicket.MEDIA, "Conexión a la red", List.of(" Conexión Wi_Fi ", "conexion-wi-fi", "Red",
				"VPN", "Internet", "Soporte")));

		assertEquals(List.of("conexion-wi-fi", "red", "vpn", "internet", "soporte"), result.etiquetas());
	}

	@Test
	void discardsLabelsOutsideLengthBoundsAndRequiresOneValidLabel() {
		var result = AnalisisTicketValidator.validar(new AnalisisTicketResponse(CategoriaTicket.OTRO,
			PrioridadTicket.BAJA, "Consulta", List.of("a", "x".repeat(31), "ok")));
		assertEquals(List.of("ok"), result.etiquetas());

		assertThrows(AiAnalisisException.class, () -> AnalisisTicketValidator.validar(
			new AnalisisTicketResponse(CategoriaTicket.OTRO, PrioridadTicket.BAJA, "Consulta", List.of("a"))));
	}

	@Test
	void rejectsBlankOrOverlongSummary() {
		assertThrows(AiAnalisisException.class, () -> AnalisisTicketValidator.validar(
			new AnalisisTicketResponse(CategoriaTicket.OTRO, PrioridadTicket.BAJA, "  ", List.of("soporte"))));
		assertThrows(AiAnalisisException.class, () -> AnalisisTicketValidator.validar(
			new AnalisisTicketResponse(CategoriaTicket.OTRO, PrioridadTicket.BAJA, "x".repeat(501), List.of("soporte"))));
	}
}
