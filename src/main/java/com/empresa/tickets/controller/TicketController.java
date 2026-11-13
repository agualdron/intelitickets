package com.empresa.tickets.controller;

import com.empresa.tickets.dto.ticket.ActualizarEstadoRequest;
import com.empresa.tickets.dto.ticket.AsignarTecnicoRequest;
import com.empresa.tickets.dto.ticket.CrearTicketRequest;
import com.empresa.tickets.dto.ticket.MetricasTicketsResponse;
import com.empresa.tickets.dto.ticket.TicketDetalleResponse;
import com.empresa.tickets.dto.ticket.TicketResponse;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.service.MetricasService;
import com.empresa.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/tickets")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

	private final TicketService ticketService;
	private final MetricasService metricasService;

	public TicketController(TicketService ticketService, MetricasService metricasService) {
		this.ticketService = ticketService;
		this.metricasService = metricasService;
	}

	@PostMapping
	public ResponseEntity<TicketResponse> crear(@Valid @RequestBody CrearTicketRequest request) {
		var ticket = ticketService.crear(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(ticket.id()).toUri();
		return ResponseEntity.created(location).body(ticket);
	}

	@GetMapping
	public Page<TicketResponse> listar(
			@RequestParam(required = false) EstadoTicket estado,
			@RequestParam(required = false) CategoriaTicket categoria,
			@RequestParam(required = false) PrioridadTicket prioridad,
			@RequestParam(required = false) Long usuarioId,
			@RequestParam(required = false) String q,
			@ParameterObject Pageable pageable) {
		return ticketService.listar(estado, categoria, prioridad, usuarioId, q, pageable);
	}

	@GetMapping("/{id}")
	public TicketDetalleResponse detalle(@PathVariable Long id) {
		return ticketService.detalle(id);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public TicketResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody ActualizarEstadoRequest request) {
		return ticketService.cambiarEstado(id, request);
	}

	@PatchMapping("/{id}/asignar")
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public TicketResponse asignar(@PathVariable Long id, @Valid @RequestBody AsignarTecnicoRequest request) {
		return ticketService.asignar(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		ticketService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/metricas")
	@PreAuthorize("hasAnyRole('ADMIN', 'SOPORTE')")
	public MetricasTicketsResponse metricas(@RequestParam(required = false) EstadoTicket estado) {
		return metricasService.obtener(estado);
	}
}
