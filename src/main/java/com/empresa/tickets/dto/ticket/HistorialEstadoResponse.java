package com.empresa.tickets.dto.ticket;

import java.time.LocalDateTime;

import com.empresa.tickets.dto.usuario.UsuarioResumenResponse;
import com.empresa.tickets.entity.enums.EstadoTicket;

public record HistorialEstadoResponse(
		Long id,
		EstadoTicket estadoAnterior,
		EstadoTicket estadoNuevo,
		UsuarioResumenResponse usuarioModificador,
		String comentario,
		LocalDateTime fechaCambio) {
}
