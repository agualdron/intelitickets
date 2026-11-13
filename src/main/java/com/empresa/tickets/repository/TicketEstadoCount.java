package com.empresa.tickets.repository;

import com.empresa.tickets.entity.enums.EstadoTicket;

public record TicketEstadoCount(EstadoTicket valor, Long total) {
}
