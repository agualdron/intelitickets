package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.empresa.tickets.entity.HistorialEstadoTicket;
import com.empresa.tickets.entity.Ticket;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.repository.HistorialEstadoRepository;
import com.empresa.tickets.repository.TicketRepository;
import com.empresa.tickets.repository.TicketSpecification;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.security.JwtService;
import com.empresa.tickets.service.MetricasService;
import com.empresa.tickets.service.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
	"security.jwt.secret=phase-four-integration-test-secret-with-32-bytes-minimum",
	"security.jwt.expiration-ms=86400000"
})
@AutoConfigureMockMvc
@Testcontainers
class TicketPersistenceIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired TicketRepository ticketRepository;
	@Autowired HistorialEstadoRepository historialRepository;
	@Autowired UsuarioRepository usuarioRepository;
	@Autowired TicketService ticketService;
	@Autowired MetricasService metricasService;
	@Autowired JwtService jwtService;
	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired PlatformTransactionManager transactionManager;
	private Usuario admin;
	private Usuario soporte;
	private Usuario laura;

	@BeforeEach
	void limpiarDatosDeTickets() {
		SecurityContextHolder.clearContext();
		historialRepository.deleteAll();
		ticketRepository.deleteAll();
		admin = usuarioRepository.findByCorreoIgnoreCase("admin@empresa.com").orElseThrow();
		soporte = usuarioRepository.findByCorreoIgnoreCase("soporte1@empresa.com").orElseThrow();
		laura = usuarioRepository.findByCorreoIgnoreCase("laura.gomez@empresa.com").orElseThrow();
	}

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void persisteEtiquetasJsonbYAplicaSpecificationPaginacionYOrden() {
		var primero = crearTicket("Red de oficina", "La conexión wifi falla en la sala de reuniones.",
				laura, CategoriaTicket.CONECTIVIDAD, PrioridadTicket.ALTA, EstadoTicket.ABIERTO,
				List.of("wifi", "red"));
		crearTicket("Acceso a correo", "No puedo acceder al correo corporativo.", laura,
				CategoriaTicket.ACCESOS, PrioridadTicket.MEDIA, EstadoTicket.EN_PROCESO, List.of("correo"));
		ticketRepository.flush();

		var recargado = ticketRepository.findById(primero.getId()).orElseThrow();
		assertEquals(List.of("wifi", "red"), recargado.getEtiquetasIA());
		assertEquals(2, jdbcTemplate.queryForObject("select count(*) from tickets", Integer.class));

		var page = ticketRepository.findAll(TicketSpecification.conFiltros(null, CategoriaTicket.CONECTIVIDAD,
				null, null, "WIFI", laura.getId(), Rol.USUARIO),
				PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "titulo")));
		assertEquals(1, page.getTotalElements());
		assertEquals("Red de oficina", page.getContent().getFirst().getTitulo());
	}

	@Test
	void cargaDetalleConHistorialLazyEnTransaccionDeServicio() {
		var ticket = crearTicket("Equipo bloqueado", "El equipo quedó bloqueado luego de actualizar.", laura,
				CategoriaTicket.HARDWARE, PrioridadTicket.ALTA, EstadoTicket.ABIERTO, List.of("equipo"));
		historialRepository.save(new HistorialEstadoTicket(ticket, null, EstadoTicket.ABIERTO, laura, null));
		historialRepository.save(new HistorialEstadoTicket(ticket, EstadoTicket.ABIERTO, EstadoTicket.EN_PROCESO,
				soporte, "Asignado"));
		ticketRepository.flush();
		historialRepository.flush();
		SecurityContextHolder.getContext().setAuthentication(auth(laura, Rol.USUARIO));

		var detalle = ticketService.detalle(ticket.getId());

		assertEquals(2, detalle.historial().size());
		assertEquals(EstadoTicket.ABIERTO, detalle.historial().getFirst().estadoNuevo());
		assertEquals("Asignado", detalle.historial().getLast().comentario());
	}

	@Test
	void metricasRellenanEnumsConCeroYExcluyenEliminadosSalvoPeticionAdmin() {
		crearTicket("Vigente", "Una descripción suficientemente larga para un ticket vigente.", laura,
				CategoriaTicket.SOFTWARE, PrioridadTicket.MEDIA, EstadoTicket.ABIERTO, List.of());
		crearTicket("Eliminado", "Una descripción suficientemente larga para un ticket eliminado.", laura,
				CategoriaTicket.HARDWARE, PrioridadTicket.CRITICA, EstadoTicket.ELIMINADO, List.of());
		SecurityContextHolder.getContext().setAuthentication(auth(admin, Rol.ADMIN));

		var vigentes = metricasService.obtener(null);
		var eliminados = metricasService.obtener(EstadoTicket.ELIMINADO);

		assertEquals(1L, vigentes.porEstado().get(EstadoTicket.ABIERTO));
		assertEquals(0L, vigentes.porEstado().get(EstadoTicket.ELIMINADO));
		assertEquals(0L, vigentes.porCategoria().get(CategoriaTicket.CONECTIVIDAD));
		assertEquals(1L, eliminados.porEstado().get(EstadoTicket.ELIMINADO));
		assertEquals(1L, eliminados.porPrioridad().get(PrioridadTicket.CRITICA));
		assertEquals(EstadoTicket.values().length, vigentes.porEstado().size());
		assertEquals(CategoriaTicket.values().length, vigentes.porCategoria().size());
		assertEquals(PrioridadTicket.values().length, vigentes.porPrioridad().size());
	}

	@Test
	void errorAlGuardarHistorialRevierteLaMutacionDelTicket() {
		var ticket = crearTicket("Fallo transaccional", "Descripción para verificar rollback de auditoría.", laura,
				CategoriaTicket.SOFTWARE, PrioridadTicket.MEDIA, EstadoTicket.ABIERTO, List.of());
		historialRepository.save(new HistorialEstadoTicket(ticket, null, EstadoTicket.ABIERTO, laura, null));
		ticketRepository.flush();
		historialRepository.flush();
		var tx = new TransactionTemplate(transactionManager);

		assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(status -> {
			var managed = ticketRepository.findById(ticket.getId()).orElseThrow();
			managed.cambiarEstado(EstadoTicket.EN_PROCESO);
			historialRepository.save(new HistorialEstadoTicket(managed, EstadoTicket.ABIERTO,
					EstadoTicket.EN_PROCESO, soporte, "x".repeat(256)));
			historialRepository.flush();
		}));

		assertEquals(EstadoTicket.ABIERTO, ticketRepository.findById(ticket.getId()).orElseThrow().getEstado());
		assertEquals(1, historialRepository.findAllByTicketIdOrderByFechaCambioAsc(ticket.getId()).size());
	}

	@Test
	void apiProtegeMetricasYExponeOpenApiBearer() throws Exception {
		var adminToken = jwtService.generarToken(admin);
		var userToken = jwtService.generarToken(laura);
		mockMvc.perform(get("/api/tickets/metricas").header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
		mockMvc.perform(get("/api/tickets/metricas").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$.porEstado.ABIERTO").value(0));
		mockMvc.perform(get("/api/tickets").param("estado", "INVALID")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.path").value("/api/tickets"));
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	void apiCreaListaActualizaYEliminaTicketsLogicamente() throws Exception {
		var adminToken = jwtService.generarToken(admin);
		var userToken = jwtService.generarToken(laura);
		String created = mockMvc.perform(post("/api/tickets")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"titulo":"Impresora de oficina","descripcion":"Una impresora de oficina no puede imprimir documentos desde ayer.",
					 "usuarioId":%d,"categoria":"HARDWARE","prioridad":"ALTA"}
					""".formatted(laura.getId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.resumenIA").value("Clasificación manual proporcionada por el usuario."))
			.andExpect(jsonPath("$.etiquetasIA[0]").value("clasificacion-manual"))
			.andReturn().getResponse().getContentAsString();
		long ticketId = objectMapper.readTree(created).get("id").asLong();

		mockMvc.perform(get("/api/tickets").param("q", "IMPRESORA")
				.header("Authorization", "Bearer " + userToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(ticketId));
		mockMvc.perform(patch("/api/tickets/{id}/estado", ticketId)
				.header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"EN_PROCESO\",\"comentario\":\"Revisión iniciada\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_PROCESO"));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/tickets/{id}", ticketId).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
		mockMvc.perform(get("/api/tickets").param("estado", "ELIMINADO")
				.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].estado").value("ELIMINADO"));
	}

	@Test
	void asignacionValidaTecnicoYRechazaDestinoInvalidoOEliminado() throws Exception {
		var adminToken = jwtService.generarToken(admin);
		String created = mockMvc.perform(post("/api/tickets")
				.header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"titulo":"Equipo no inicia","descripcion":"El computador del usuario no inicia desde la mañana.",
					 "categoria":"HARDWARE","prioridad":"MEDIA"}
					"""))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long ticketId = objectMapper.readTree(created).get("id").asLong();

		mockMvc.perform(patch("/api/tickets/{id}/asignar", ticketId)
				.header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tecnicoId\":%d}".formatted(soporte.getId())))
			.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_PROCESO"))
			.andExpect(jsonPath("$.tecnicoAsignado.id").value(soporte.getId()));

		mockMvc.perform(patch("/api/tickets/{id}/asignar", ticketId)
				.header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tecnicoId\":999999}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/tickets/{id}", ticketId).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isNoContent());
		mockMvc.perform(patch("/api/tickets/{id}/asignar", ticketId)
				.header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tecnicoId\":%d}".formatted(soporte.getId())))
			.andExpect(status().isBadRequest());
	}

	private Ticket crearTicket(String titulo, String descripcion, Usuario solicitante, CategoriaTicket categoria,
			PrioridadTicket prioridad, EstadoTicket estado, List<String> etiquetas) {
		var ticket = new Ticket(titulo, descripcion, categoria, prioridad, solicitante, "resumen", etiquetas);
		if (estado != EstadoTicket.ABIERTO) {
			ticket.cambiarEstado(estado);
		}
		return ticketRepository.save(ticket);
	}

	private UsernamePasswordAuthenticationToken auth(Usuario usuario, Rol rol) {
		return new UsernamePasswordAuthenticationToken(usuario.getCorreo(), "n/a",
				List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
	}
}
