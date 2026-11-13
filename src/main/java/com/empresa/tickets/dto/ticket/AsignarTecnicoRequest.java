package com.empresa.tickets.dto.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(@NotNull @Positive Long tecnicoId) {
}
