package com.empresa.tickets.repository;

import com.empresa.tickets.entity.HistorialEstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstadoTicket, Long> {

	List<HistorialEstadoTicket> findAllByTicketIdOrderByFechaCambioAsc(Long ticketId);
}
