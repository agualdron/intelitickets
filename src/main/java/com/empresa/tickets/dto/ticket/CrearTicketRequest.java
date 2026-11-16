package com.empresa.tickets.dto.ticket;

import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearTicketRequest(
		@NotBlank @Size(min = 5, max = 100) String titulo,
		@NotBlank @Size(min = 20, max = 2000) String descripcion,
		Long usuarioId,
		CategoriaTicket categoria,
		PrioridadTicket prioridad) {
}
