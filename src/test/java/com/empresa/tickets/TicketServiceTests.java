package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.empresa.tickets.ai.AnalizadorTicketService;
import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.dto.ticket.CrearTicketRequest;
import com.empresa.tickets.entity.HistorialEstadoTicket;
import com.empresa.tickets.entity.Ticket;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.mapper.TicketMapper;
import com.empresa.tickets.repository.HistorialEstadoRepository;
import com.empresa.tickets.repository.TicketRepository;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.service.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TicketServiceTests {

	@Mock TicketRepository ticketRepository;
	@Mock UsuarioRepository usuarioRepository;
	@Mock HistorialEstadoRepository historialRepository;
	@Mock AnalizadorTicketService analizador;
	private TicketService service;
	private Usuario usuario;

	@BeforeEach
	void setUp() {
		service = new TicketService(ticketRepository, usuarioRepository, historialRepository, analizador,
				new TicketMapper());
		usuario = new Usuario("Laura Gómez", "laura.gomez@empresa.com", "hash", Rol.USUARIO);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				usuario.getCorreo(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
		when(usuarioRepository.findByCorreoIgnoreCase(usuario.getCorreo())).thenReturn(Optional.of(usuario));
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void creacionManualNoInvocaAnalizadorYRegistraEstadoInicial() {
		var request = new CrearTicketRequest("No conecta mi equipo", "El equipo no conecta a la red desde hoy.",
				null, CategoriaTicket.HARDWARE, PrioridadTicket.ALTA);

		var response = service.crear(request);

		assertEquals(CategoriaTicket.HARDWARE, response.categoria());
		assertEquals(PrioridadTicket.ALTA, response.prioridad());
		assertEquals("Clasificación manual proporcionada por el usuario.", response.resumenIA());
		assertEquals(List.of("clasificacion-manual"), response.etiquetasIA());
		verify(analizador, never()).analizar(any(), any());
		var event = ArgumentCaptor.forClass(HistorialEstadoTicket.class);
		verify(historialRepository).save(event.capture());
		assertEquals(null, event.getValue().getEstadoAnterior());
		assertEquals(EstadoTicket.ABIERTO, event.getValue().getEstadoNuevo());
	}

	@Test
	void clasificacionParcialConservaCategoriaManualYCompletaPrioridadConIA() {
		when(analizador.analizar("No conecta mi equipo", "El equipo no conecta a la red desde hoy."))
				.thenReturn(new AnalisisTicketResponse(CategoriaTicket.CONECTIVIDAD, PrioridadTicket.CRITICA,
						"No conecta", List.of("red")));
		var request = new CrearTicketRequest("No conecta mi equipo", "El equipo no conecta a la red desde hoy.",
				null, CategoriaTicket.HARDWARE, null);

		var response = service.crear(request);

		assertEquals(CategoriaTicket.HARDWARE, response.categoria());
		assertEquals(PrioridadTicket.CRITICA, response.prioridad());
		assertEquals("No conecta", response.resumenIA());
		assertNotNull(response.etiquetasIA());
		verify(analizador).analizar(request.titulo(), request.descripcion());
	}
}
