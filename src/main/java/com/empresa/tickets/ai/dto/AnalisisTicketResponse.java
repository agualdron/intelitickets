package com.empresa.tickets.ai.dto;

import java.util.List;
import java.util.Objects;

import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;

public record AnalisisTicketResponse(
	CategoriaTicket categoria,
	PrioridadTicket prioridad,
	String resumen,
	List<String> etiquetas
) {
	public AnalisisTicketResponse {
		Objects.requireNonNull(categoria, "categoria no puede ser null");
		Objects.requireNonNull(prioridad, "prioridad no puede ser null");
		Objects.requireNonNull(resumen, "resumen no puede ser null");
		etiquetas = List.copyOf(Objects.requireNonNull(etiquetas, "etiquetas no puede ser null"));
	}
}
