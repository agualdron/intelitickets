package com.empresa.tickets.dto.ticket;

import java.time.LocalDateTime;
import java.util.List;

import com.empresa.tickets.dto.usuario.UsuarioResumenResponse;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;

public record TicketDetalleResponse(
		Long id,
		String titulo,
		String descripcion,
		CategoriaTicket categoria,
		PrioridadTicket prioridad,
		EstadoTicket estado,
		UsuarioResumenResponse usuarioSolicitante,
		UsuarioResumenResponse tecnicoAsignado,
		String resumenIA,
		List<String> etiquetasIA,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaActualizacion,
		List<HistorialEstadoResponse> historial) {
}
