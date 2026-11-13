package com.empresa.tickets.repository;

import com.empresa.tickets.entity.enums.CategoriaTicket;

public record TicketCategoriaCount(CategoriaTicket valor, Long total) {
}
