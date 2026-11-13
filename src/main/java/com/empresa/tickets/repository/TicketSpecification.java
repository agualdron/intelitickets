package com.empresa.tickets.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.empresa.tickets.entity.Ticket;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.entity.enums.Rol;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecification {

	private TicketSpecification() {
	}

	public static Specification<Ticket> conFiltros(EstadoTicket estado, CategoriaTicket categoria,
			PrioridadTicket prioridad, Long usuarioId, String q, Long usuarioAutenticadoId, Rol rol) {
		return (root, query, builder) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			if (estado != null) {
				predicates.add(builder.equal(root.get("estado"), estado));
			}
			if (categoria != null) {
				predicates.add(builder.equal(root.get("categoria"), categoria));
			}
			if (prioridad != null) {
				predicates.add(builder.equal(root.get("prioridad"), prioridad));
			}
			if (rol == Rol.USUARIO) {
				predicates.add(builder.equal(root.get("usuarioSolicitante").get("id"), usuarioAutenticadoId));
			}
			else if (usuarioId != null) {
				predicates.add(builder.equal(root.get("usuarioSolicitante").get("id"), usuarioId));
			}
			if (estado != EstadoTicket.ELIMINADO || rol != Rol.ADMIN) {
				predicates.add(builder.notEqual(root.get("estado"), EstadoTicket.ELIMINADO));
			}
			if (q != null && !q.isBlank()) {
				String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("titulo")), pattern),
						builder.like(builder.lower(root.get("descripcion")), pattern)));
			}
			return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}
}
