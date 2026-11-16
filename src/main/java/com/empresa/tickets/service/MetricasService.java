package com.empresa.tickets.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.empresa.tickets.dto.ticket.MetricasTicketsResponse;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.exception.ReglaNegocioException;
import com.empresa.tickets.repository.TicketRepository;
import com.empresa.tickets.security.ActorAutenticadoProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricasService {

	private final TicketRepository ticketRepository;
	private final ActorAutenticadoProvider actorAutenticadoProvider;

	public MetricasService(TicketRepository ticketRepository, ActorAutenticadoProvider actorAutenticadoProvider) {
		this.ticketRepository = ticketRepository;
		this.actorAutenticadoProvider = actorAutenticadoProvider;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public MetricasTicketsResponse obtener(EstadoTicket estado) {
		if (estado != null && estado != EstadoTicket.ELIMINADO) {
			throw new ReglaNegocioException("El único filtro permitido para métricas es estado=ELIMINADO");
		}
		boolean soloEliminados = estado == EstadoTicket.ELIMINADO;
		if (soloEliminados && !esAdmin()) {
			throw new AccessDeniedException("Solo ADMIN puede consultar métricas de tickets eliminados");
		}
		var estados = todosLosEstados();
		var categorias = todosLasCategorias();
		var prioridades = todasLasPrioridades();
		var eliminado = EstadoTicket.ELIMINADO;
		ticketRepository.contarPorEstado(soloEliminados, eliminado)
				.forEach(conteo -> estados.put(conteo.valor(), conteo.total()));
		ticketRepository.contarPorCategoria(soloEliminados, eliminado)
				.forEach(conteo -> categorias.put(conteo.valor(), conteo.total()));
		ticketRepository.contarPorPrioridad(soloEliminados, eliminado)
				.forEach(conteo -> prioridades.put(conteo.valor(), conteo.total()));
		return new MetricasTicketsResponse(Map.copyOf(estados), Map.copyOf(categorias), Map.copyOf(prioridades));
	}

	private boolean esAdmin() {
		return actorAutenticadoProvider.obtenerActor().getRol() == Rol.ADMIN;
	}

	private EnumMap<EstadoTicket, Long> todosLosEstados() {
		return java.util.Arrays.stream(EstadoTicket.values()).collect(Collectors.toMap(
				estado -> estado, estado -> 0L, (left, right) -> left,
				() -> new EnumMap<>(EstadoTicket.class)));
	}

	private EnumMap<CategoriaTicket, Long> todosLasCategorias() {
		return java.util.Arrays.stream(CategoriaTicket.values()).collect(Collectors.toMap(
				categoria -> categoria, categoria -> 0L, (left, right) -> left,
				() -> new EnumMap<>(CategoriaTicket.class)));
	}

	private EnumMap<PrioridadTicket, Long> todasLasPrioridades() {
		return java.util.Arrays.stream(PrioridadTicket.values()).collect(Collectors.toMap(
				prioridad -> prioridad, prioridad -> 0L, (left, right) -> left,
				() -> new EnumMap<>(PrioridadTicket.class)));
	}
}
