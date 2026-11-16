package com.empresa.tickets.mapper;

import java.util.List;

import com.empresa.tickets.dto.ticket.HistorialEstadoResponse;
import com.empresa.tickets.dto.ticket.TicketDetalleResponse;
import com.empresa.tickets.dto.ticket.TicketResponse;
import com.empresa.tickets.dto.usuario.UsuarioResumenResponse;
import com.empresa.tickets.entity.HistorialEstadoTicket;
import com.empresa.tickets.entity.Ticket;
import com.empresa.tickets.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

	public TicketResponse aResponse(Ticket ticket) {
		return new TicketResponse(ticket.getId(), ticket.getTitulo(), ticket.getDescripcion(), ticket.getCategoria(),
				ticket.getPrioridad(), ticket.getEstado(), aResumen(ticket.getUsuarioSolicitante()),
				aResumen(ticket.getTecnicoAsignado()), ticket.getResumenIA(), List.copyOf(ticket.getEtiquetasIA()),
				ticket.getFechaCreacion(), ticket.getFechaActualizacion());
	}

	public TicketDetalleResponse aDetalle(Ticket ticket, List<HistorialEstadoTicket> historial) {
		var response = aResponse(ticket);
		return new TicketDetalleResponse(response.id(), response.titulo(), response.descripcion(), response.categoria(),
				response.prioridad(), response.estado(), response.usuarioSolicitante(), response.tecnicoAsignado(),
				response.resumenIA(), response.etiquetasIA(), response.fechaCreacion(), response.fechaActualizacion(),
				historial.stream().map(this::aHistorial).toList());
	}

	public HistorialEstadoResponse aHistorial(HistorialEstadoTicket historial) {
		return new HistorialEstadoResponse(historial.getId(), historial.getEstadoAnterior(), historial.getEstadoNuevo(),
				aResumen(historial.getUsuarioModificador()), historial.getComentario(), historial.getFechaCambio());
	}

	private UsuarioResumenResponse aResumen(Usuario usuario) {
		if (usuario == null) {
			return null;
		}
		return new UsuarioResumenResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol());
	}
}
