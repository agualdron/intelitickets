package com.empresa.tickets.dto.ticket;

import com.empresa.tickets.entity.enums.EstadoTicket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEstadoRequest(@NotNull EstadoTicket estado, @Size(max = 255) String comentario) {
}
