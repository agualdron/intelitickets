package com.empresa.tickets.repository;

import com.empresa.tickets.entity.HistorialEstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstadoTicket, Long> {
}
