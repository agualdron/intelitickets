package com.empresa.tickets.repository;

import com.empresa.tickets.entity.enums.PrioridadTicket;

public record TicketPrioridadCount(PrioridadTicket valor, Long total) {
}
