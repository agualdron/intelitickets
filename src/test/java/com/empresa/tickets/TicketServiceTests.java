package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.empresa.tickets.exception.AiAnalisisException;
import com.empresa.tickets.mapper.TicketMapper;
import com.empresa.tickets.repository.HistorialEstadoRepository;
import com.empresa.tickets.repository.TicketRepository;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.security.ActorAutenticadoProvider;
import com.empresa.tickets.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceTests {

	@Mock TicketRepository ticketRepository;
	@Mock UsuarioRepository usuarioRepository;
	@Mock HistorialEstadoRepository historialRepository;
	@Mock AnalizadorTicketService analizador;
	@Mock ActorAutenticadoProvider actorAutenticadoProvider;
	private TicketService service;
	private Usuario usuario;

	@BeforeEach
	void setUp() {
		service = new TicketService(ticketRepository, usuarioRepository, historialRepository, analizador,
				new TicketMapper(), actorAutenticadoProvider);
		usuario = new Usuario("Laura Gómez", "laura.gomez@empresa.com", "hash", Rol.USUARIO);
		when(actorAutenticadoProvider.obtenerActor()).thenReturn(usuario);
	}

	@Test
	void creacionManualNoInvocaAnalizadorYRegistraEstadoInicial() {
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var request = new CrearTicketRequest("No conecta mi equipo", "El equipo no conecta a la red desde hoy.",
				null, CategoriaTicket.HARDWARE, PrioridadTicket.ALTA);

		var response = service.crear(request);

		assertEquals(CategoriaTicket.HARDWARE, response.categoria());
		assertEquals(PrioridadTicket.ALTA, response.prioridad());
		assertEquals("Clasificación manual proporcionada por el usuario.", response.resumenIA());
		assertEquals(List.of("clasificacion-manual"), response.etiquetasIA());
		verify(analizador, never()).analizar(any(), any());
		var savedTicket = ArgumentCaptor.forClass(Ticket.class);
		verify(ticketRepository).save(savedTicket.capture());
		assertEquals(CategoriaTicket.HARDWARE, savedTicket.getValue().getCategoria());
		assertEquals(PrioridadTicket.ALTA, savedTicket.getValue().getPrioridad());
		var event = ArgumentCaptor.forClass(HistorialEstadoTicket.class);
		verify(historialRepository).save(event.capture());
		assertEquals(null, event.getValue().getEstadoAnterior());
		assertEquals(EstadoTicket.ABIERTO, event.getValue().getEstadoNuevo());
	}

	@Test
	void clasificacionParcialConservaCategoriaManualYCompletaPrioridadConIA() {
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(analizador.analizar("No conecta mi equipo", "El equipo no conecta a la red desde hoy."))
				.thenReturn(new AnalisisTicketResponse(CategoriaTicket.CONECTIVIDAD, PrioridadTicket.CRITICA,
						"No conecta", List.of("red")));
		var request = new CrearTicketRequest("No conecta mi equipo", "El equipo no conecta a la red desde hoy.",
				null, CategoriaTicket.HARDWARE, null);

		var response = service.crear(request);

		assertEquals(CategoriaTicket.HARDWARE, response.categoria());
		assertEquals(PrioridadTicket.CRITICA, response.prioridad());
		assertEquals("No conecta", response.resumenIA());
		assertEquals(List.of("red"), response.etiquetasIA());
		verify(analizador).analizar(request.titulo(), request.descripcion());
		var savedTicket = ArgumentCaptor.forClass(Ticket.class);
		verify(ticketRepository).save(savedTicket.capture());
		assertEquals(CategoriaTicket.HARDWARE, savedTicket.getValue().getCategoria());
		assertEquals(PrioridadTicket.CRITICA, savedTicket.getValue().getPrioridad());
	}

	@Test
	void failedAiAnalysisDoesNotPersistTicketOrHistory() {
		when(analizador.analizar(any(), any())).thenThrow(new AiAnalisisException("safe error"));
		var request = new CrearTicketRequest("No conecta mi equipo", "El equipo no conecta a la red desde hoy.",
			null, null, null);

		assertThrows(AiAnalisisException.class, () -> service.crear(request));
		verify(ticketRepository, never()).save(any(Ticket.class));
		verify(historialRepository, never()).save(any(HistorialEstadoTicket.class));
	}
}
