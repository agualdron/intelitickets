package com.empresa.tickets.entity;

import java.time.LocalDateTime;

import com.empresa.tickets.entity.enums.EstadoTicket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_estados_ticket")
public class HistorialEstadoTicket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_anterior", length = 20)
	private EstadoTicket estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_nuevo", nullable = false, length = 20)
	private EstadoTicket estadoNuevo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_modificador_id", nullable = false)
	private Usuario usuarioModificador;

	@Column(length = 255)
	private String comentario;

	@Column(name = "fecha_cambio", nullable = false, updatable = false)
	private LocalDateTime fechaCambio;

	@PrePersist
	void inicializarFechaCambio() {
		if (fechaCambio == null) {
			fechaCambio = LocalDateTime.now();
		}
	}

	protected HistorialEstadoTicket() {
	}

	public HistorialEstadoTicket(Ticket ticket, EstadoTicket estadoAnterior, EstadoTicket estadoNuevo,
			Usuario usuarioModificador, String comentario) {
		this.ticket = ticket;
		this.estadoAnterior = estadoAnterior;
		this.estadoNuevo = estadoNuevo;
		this.usuarioModificador = usuarioModificador;
		this.comentario = comentario;
	}

	public Long getId() {
		return id;
	}

	public Ticket getTicket() { return ticket; }
	public EstadoTicket getEstadoAnterior() { return estadoAnterior; }
	public EstadoTicket getEstadoNuevo() { return estadoNuevo; }
	public Usuario getUsuarioModificador() { return usuarioModificador; }
	public String getComentario() { return comentario; }
	public LocalDateTime getFechaCambio() { return fechaCambio; }
}
