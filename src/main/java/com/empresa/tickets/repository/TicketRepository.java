package com.empresa.tickets.repository;

import com.empresa.tickets.entity.Ticket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

	@Query("select new com.empresa.tickets.repository.TicketEstadoCount(t.estado, count(t)) "
			+ "from Ticket t where ((:soloEliminados = true and t.estado = :eliminado) "
			+ "or (:soloEliminados = false and t.estado <> :eliminado)) group by t.estado")
	List<TicketEstadoCount> contarPorEstado(@Param("soloEliminados") boolean soloEliminados,
			@Param("eliminado") EstadoTicket eliminado);

	@Query("select new com.empresa.tickets.repository.TicketCategoriaCount(t.categoria, count(t)) "
			+ "from Ticket t where ((:soloEliminados = true and t.estado = :eliminado) "
			+ "or (:soloEliminados = false and t.estado <> :eliminado)) group by t.categoria")
	List<TicketCategoriaCount> contarPorCategoria(@Param("soloEliminados") boolean soloEliminados,
			@Param("eliminado") EstadoTicket eliminado);

	@Query("select new com.empresa.tickets.repository.TicketPrioridadCount(t.prioridad, count(t)) "
			+ "from Ticket t where ((:soloEliminados = true and t.estado = :eliminado) "
			+ "or (:soloEliminados = false and t.estado <> :eliminado)) group by t.prioridad")
	List<TicketPrioridadCount> contarPorPrioridad(@Param("soloEliminados") boolean soloEliminados,
			@Param("eliminado") EstadoTicket eliminado);
}
