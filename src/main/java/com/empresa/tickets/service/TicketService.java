package com.empresa.tickets.service;

import java.util.List;
import java.util.Set;

import com.empresa.tickets.ai.AnalizadorTicketService;
import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.dto.ticket.ActualizarEstadoRequest;
import com.empresa.tickets.dto.ticket.AsignarTecnicoRequest;
import com.empresa.tickets.dto.ticket.CrearTicketRequest;
import com.empresa.tickets.dto.ticket.TicketDetalleResponse;
import com.empresa.tickets.dto.ticket.TicketResponse;
import com.empresa.tickets.entity.HistorialEstadoTicket;
import com.empresa.tickets.entity.Ticket;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.exception.AccesoDenegadoTicketException;
import com.empresa.tickets.exception.RecursoNoEncontradoException;
import com.empresa.tickets.exception.ReglaNegocioException;
import com.empresa.tickets.mapper.TicketMapper;
import com.empresa.tickets.repository.HistorialEstadoRepository;
import com.empresa.tickets.repository.TicketRepository;
import com.empresa.tickets.repository.TicketSpecification;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.security.ActorAutenticadoProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

	private static final String RESUMEN_MANUAL = "Clasificación manual proporcionada por el usuario.";
	private static final Set<String> CAMPOS_ORDENABLES = Set.of("id", "titulo", "descripcion", "categoria",
			"prioridad", "estado", "fechaCreacion", "fechaActualizacion");
	private final TicketRepository ticketRepository;
	private final UsuarioRepository usuarioRepository;
	private final HistorialEstadoRepository historialRepository;
	private final AnalizadorTicketService analizador;
	private final TicketMapper mapper;
	private final ActorAutenticadoProvider actorAutenticadoProvider;

	public TicketService(TicketRepository ticketRepository, UsuarioRepository usuarioRepository,
			HistorialEstadoRepository historialRepository, AnalizadorTicketService analizador, TicketMapper mapper,
			ActorAutenticadoProvider actorAutenticadoProvider) {
		this.ticketRepository = ticketRepository;
		this.usuarioRepository = usuarioRepository;
		this.historialRepository = historialRepository;
		this.analizador = analizador;
		this.mapper = mapper;
		this.actorAutenticadoProvider = actorAutenticadoProvider;
	}

	@Transactional
	public TicketResponse crear(CrearTicketRequest request) {
		var actor = usuarioAutenticado();
		var solicitante = resolverSolicitante(request.usuarioId(), actor);
		var categoria = request.categoria();
		var prioridad = request.prioridad();
		String resumen;
		List<String> etiquetas;
		if (categoria != null && prioridad != null) {
			resumen = RESUMEN_MANUAL;
			etiquetas = List.of("clasificacion-manual");
		}
		else {
			AnalisisTicketResponse analisis = analizador.analizar(request.titulo(), request.descripcion());
			categoria = categoria == null ? analisis.categoria() : categoria;
			prioridad = prioridad == null ? analisis.prioridad() : prioridad;
			resumen = analisis.resumen();
			etiquetas = analisis.etiquetas();
		}

		var ticket = ticketRepository.save(new Ticket(request.titulo(), request.descripcion(), categoria, prioridad,
				solicitante, resumen, etiquetas));
		historialRepository.save(new HistorialEstadoTicket(ticket, null, EstadoTicket.ABIERTO, actor, null));
		return mapper.aResponse(ticket);
	}

	@Transactional(readOnly = true)
	public Page<TicketResponse> listar(EstadoTicket estado, CategoriaTicket categoria, PrioridadTicket prioridad,
			Long usuarioId, String q, Pageable pageable) {
		if (pageable.getSort().stream().anyMatch(order -> !CAMPOS_ORDENABLES.contains(order.getProperty()))) {
			throw new ReglaNegocioException("Campo de ordenamiento inválido");
		}
		var actor = usuarioAutenticado();
		var page = ticketRepository.findAll(TicketSpecification.conFiltros(estado, categoria, prioridad, usuarioId, q,
				actor.getId(), actor.getRol()), pageable);
		return page.map(mapper::aResponse);
	}

	@Transactional(readOnly = true)
	public TicketDetalleResponse detalle(Long ticketId) {
		var actor = usuarioAutenticado();
		var ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado"));
		if (actor.getRol() == Rol.USUARIO && !ticket.getUsuarioSolicitante().getId().equals(actor.getId())) {
			throw new AccesoDenegadoTicketException("No tiene acceso a este ticket");
		}
		var historial = historialRepository.findAllByTicketIdOrderByFechaCambioAsc(ticketId);
		return mapper.aDetalle(ticket, historial);
	}

	@Transactional
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public TicketResponse cambiarEstado(Long ticketId, ActualizarEstadoRequest request) {
		var actor = usuarioAutenticado();
		var ticket = buscarTicket(ticketId);
		transicionar(ticket, request.estado(), actor, request.comentario());
		return mapper.aResponse(ticket);
	}

	@Transactional
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public TicketResponse asignar(Long ticketId, AsignarTecnicoRequest request) {
		var ticket = buscarTicket(ticketId);
		if (ticket.getEstado() == EstadoTicket.ELIMINADO) {
			throw new ReglaNegocioException("No se puede asignar un ticket eliminado");
		}
		var tecnico = usuarioRepository.findById(request.tecnicoId())
				.orElseThrow(() -> new ReglaNegocioException("El técnico indicado no existe"));
		if (tecnico.getRol() != Rol.SOPORTE) {
			throw new ReglaNegocioException("El usuario destino debe tener rol SOPORTE");
		}
		if (!tecnico.isActivo()) {
			throw new ReglaNegocioException("No se puede asignar un usuario inactivo");
		}
		var actor = usuarioAutenticado();
		var estadoAnterior = ticket.getEstado();
		ticket.asignarTecnico(tecnico);
		if (estadoAnterior == EstadoTicket.ABIERTO) {
			ticket.cambiarEstado(EstadoTicket.EN_PROCESO);
			guardarHistorial(ticket, estadoAnterior, EstadoTicket.EN_PROCESO, actor, null);
		}
		return mapper.aResponse(ticket);
	}

	@Transactional
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public void eliminar(Long ticketId) {
		var actor = usuarioAutenticado();
		var ticket = buscarTicket(ticketId);
		transicionar(ticket, EstadoTicket.ELIMINADO, actor, null);
	}

	private void transicionar(Ticket ticket, EstadoTicket destino, Usuario actor, String comentario) {
		EstadoTicket origen = ticket.getEstado();
		if (!transicionPermitida(origen, destino)) {
			throw new ReglaNegocioException("Transición de estado no permitida: " + origen + " → " + destino);
		}
		ticket.cambiarEstado(destino);
		guardarHistorial(ticket, origen, destino, actor, comentario);
	}

	private boolean transicionPermitida(EstadoTicket origen, EstadoTicket destino) {
		if (origen == EstadoTicket.ELIMINADO || origen == destino) {
			return false;
		}
		if (destino == EstadoTicket.ELIMINADO) {
			return true;
		}
		return switch (origen) {
			case ABIERTO -> destino == EstadoTicket.EN_PROCESO;
			case EN_PROCESO -> destino == EstadoTicket.RESUELTO;
			case RESUELTO -> destino == EstadoTicket.CERRADO;
			case CERRADO, ELIMINADO -> false;
		};
	}

	private void guardarHistorial(Ticket ticket, EstadoTicket anterior, EstadoTicket nuevo, Usuario actor,
			String comentario) {
		historialRepository.save(new HistorialEstadoTicket(ticket, anterior, nuevo, actor, comentario));
	}

	private Ticket buscarTicket(Long ticketId) {
		return ticketRepository.findById(ticketId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado"));
	}

	private Usuario resolverSolicitante(Long usuarioId, Usuario actor) {
		if (actor.getRol() == Rol.USUARIO) {
			if (usuarioId != null && !usuarioId.equals(actor.getId())) {
				throw new ReglaNegocioException("Un usuario solo puede crear tickets para sí mismo");
			}
			return actor;
		}
		if (usuarioId == null) {
			return actor;
		}
		var solicitante = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Usuario solicitante no encontrado"));
		if (!solicitante.isActivo()) {
			throw new ReglaNegocioException("No se puede crear un ticket para un usuario inactivo");
		}
		return solicitante;
	}

	private Usuario usuarioAutenticado() {
		return actorAutenticadoProvider.obtenerActor();
	}
}
