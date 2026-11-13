package com.empresa.tickets.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tickets")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String titulo;

	@Column(nullable = false, length = 2000)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CategoriaTicket categoria;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PrioridadTicket prioridad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoTicket estado = EstadoTicket.ABIERTO;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_solicitante_id", nullable = false)
	private Usuario usuarioSolicitante;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tecnico_asignado_id")
	private Usuario tecnicoAsignado;

	@Column(name = "resumen_ia", length = 500)
	private String resumenIA;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "etiquetas_ia", columnDefinition = "jsonb")
	private List<String> etiquetasIA = new ArrayList<>();

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private LocalDateTime fechaActualizacion;

	@PrePersist
	void inicializarTiempos() {
		LocalDateTime ahora = LocalDateTime.now();
		if (fechaCreacion == null) {
			fechaCreacion = ahora;
		}
		if (fechaActualizacion == null) {
			fechaActualizacion = ahora;
		}
	}

	@PreUpdate
	void actualizarFechaPersistida() {
		fechaActualizacion = LocalDateTime.now();
	}

	protected Ticket() {
	}

	public Ticket(String titulo, String descripcion, CategoriaTicket categoria, PrioridadTicket prioridad,
			Usuario usuarioSolicitante, String resumenIA, List<String> etiquetasIA) {
		this.titulo = titulo;
		this.descripcion = descripcion;
		this.categoria = categoria;
		this.prioridad = prioridad;
		this.estado = EstadoTicket.ABIERTO;
		this.usuarioSolicitante = usuarioSolicitante;
		this.resumenIA = resumenIA;
		this.etiquetasIA = new ArrayList<>(etiquetasIA);
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() { return titulo; }
	public String getDescripcion() { return descripcion; }
	public CategoriaTicket getCategoria() { return categoria; }
	public PrioridadTicket getPrioridad() { return prioridad; }
	public EstadoTicket getEstado() { return estado; }
	public Usuario getUsuarioSolicitante() { return usuarioSolicitante; }
	public Usuario getTecnicoAsignado() { return tecnicoAsignado; }
	public String getResumenIA() { return resumenIA; }
	public List<String> getEtiquetasIA() { return Collections.unmodifiableList(etiquetasIA); }
	public LocalDateTime getFechaCreacion() { return fechaCreacion; }
	public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

	public void asignarTecnico(Usuario tecnico) {
		this.tecnicoAsignado = tecnico;
	}

	public void cambiarEstado(EstadoTicket estado) {
		this.estado = estado;
	}
}
