package com.empresa.tickets.dto.ticket;

import java.util.Map;

import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;

public record MetricasTicketsResponse(
		Map<EstadoTicket, Long> porEstado,
		Map<CategoriaTicket, Long> porCategoria,
		Map<PrioridadTicket, Long> porPrioridad) {
}
