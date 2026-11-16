package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.empresa.tickets.entity.HistorialEstadoTicket;
import com.empresa.tickets.entity.Ticket;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.EstadoTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.repository.HistorialEstadoRepository;
import com.empresa.tickets.repository.TicketRepository;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
	"security.jwt.secret=bk-user-management-test-secret-with-32-bytes-minimum",
	"security.jwt.expiration-ms=86400000",
	"CORS_ALLOWED_ORIGIN=http://localhost:5173"
})
@AutoConfigureMockMvc
class UsuarioManagementIntegrationTests extends AbstractIntegrationTest {

	@Autowired UsuarioRepository usuarioRepository;
	@Autowired TicketRepository ticketRepository;
	@Autowired HistorialEstadoRepository historialRepository;
	@Autowired JwtService jwtService;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;
	@Autowired JdbcTemplate jdbcTemplate;

	private final List<String> createdEmails = new ArrayList<>();
	private final List<Long> createdTicketIds = new ArrayList<>();
	private Usuario admin;
	private Usuario soporte;
	private Usuario segundoSoporte;
	private Usuario laura;

	@BeforeEach
	void cargarUsuariosSeed() {
		SecurityContextHolder.clearContext();
		admin = usuarioRepository.findByCorreoIgnoreCase("admin@empresa.com").orElseThrow();
		soporte = usuarioRepository.findByCorreoIgnoreCase("soporte1@empresa.com").orElseThrow();
		segundoSoporte = usuarioRepository.findByCorreoIgnoreCase("soporte2@empresa.com").orElseThrow();
		laura = usuarioRepository.findByCorreoIgnoreCase("laura.gomez@empresa.com").orElseThrow();
	}

	@AfterEach
	void limpiarDatosDePrueba() {
		for (Long ticketId : createdTicketIds) {
			historialRepository.deleteAll(historialRepository.findAllByTicketIdOrderByFechaCambioAsc(ticketId));
			ticketRepository.deleteById(ticketId);
		}
		for (String correo : createdEmails) {
			usuarioRepository.findByCorreoIgnoreCase(correo).ifPresent(usuarioRepository::delete);
		}
		createdTicketIds.clear();
		createdEmails.clear();
		SecurityContextHolder.clearContext();
	}

	@Test
	void adminPuedeListarCrearYDesactivarConservandoReferencias() throws Exception {
		var adminToken = token(admin);
		mockMvc.perform(get("/api/usuarios").header("Authorization", adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].password").doesNotExist());

		String correo = nuevoCorreo();
		var response = mockMvc.perform(post("/api/usuarios").header("Authorization", adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nombre":"Nueva Soporte","correo":"%s","password":"Password123!"}
					""".formatted(correo.toUpperCase())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.rol").value("SOPORTE"))
			.andExpect(jsonPath("$.correo").value(correo))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.token").doesNotExist())
			.andReturn();
		createdEmails.add(correo);

		var creado = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
		assertTrue(passwordEncoder.matches("Password123!", creado.getPassword()));
		assertFalse("Password123!".equals(creado.getPassword()));
		assertEquals(Rol.SOPORTE, creado.getRol());
		assertTrue(creado.isActivo());
		assertTrue(objectMapper.readTree(response.getResponse().getContentAsString()).has("id"));

		var ticket = ticketRepository.save(new Ticket("BK baja lógica conserva referencias",
				"Ticket asociado a una cuenta que se desactiva lógicamente.", CategoriaTicket.SOFTWARE,
				PrioridadTicket.MEDIA, creado, "Resumen", List.of()));
		createdTicketIds.add(ticket.getId());
		var history = historialRepository.save(new HistorialEstadoTicket(ticket, null, EstadoTicket.ABIERTO, creado,
				"Registro histórico asociado"));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/usuarios/{id}", creado.getId()).header("Authorization", adminToken))
			.andExpect(status().isNoContent());
		assertFalse(usuarioRepository.findById(creado.getId()).orElseThrow().isActivo());
		assertEquals(creado.getId(), ticketRepository.findById(ticket.getId()).orElseThrow()
				.getUsuarioSolicitante().getId());
		assertEquals(history.getId(), historialRepository.findAllByTicketIdOrderByFechaCambioAsc(ticket.getId())
				.getFirst().getId());

		mockMvc.perform(get("/api/usuarios").header("Authorization", adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.correo == '%s')]".formatted(correo)).doesNotExist());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/usuarios/{id}", creado.getId()).header("Authorization", adminToken))
			.andExpect(status().isNoContent());
	}

	@Test
	void lasOperacionesAdministrativasRechazanSOPORTEUSUARIOYAnonimo() throws Exception {
		var supportToken = token(soporte);
		var userToken = token(laura);
		var body = "{\"nombre\":\"Otro soporte\",\"correo\":\"otro@example.com\",\"password\":\"Password123!\"}";

		for (String token : List.of(supportToken, userToken)) {
			mockMvc.perform(get("/api/usuarios").header("Authorization", token))
				.andExpect(status().isForbidden());
			mockMvc.perform(post("/api/usuarios").header("Authorization", token)
					.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
			mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
					.delete("/api/usuarios/{id}", laura.getId()).header("Authorization", token))
				.andExpect(status().isForbidden());
		}
		mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/usuarios/{id}", laura.getId())).andExpect(status().isUnauthorized());
	}

	@Test
	void directorioSoporteListaTodasLasCuentasActivasIncluidaLaPropia() throws Exception {
		String correo = nuevoCorreo();
		var inactivo = usuarioRepository.save(new Usuario("Soporte inactivo", correo,
				passwordEncoder.encode("Password123!"), Rol.SOPORTE));
		createdEmails.add(correo);
		jdbcTemplate.update("UPDATE usuarios SET activo = FALSE WHERE id = ?", inactivo.getId());

		var supportToken = token(soporte);
		var result = mockMvc.perform(get("/api/usuarios/soporte").header("Authorization", supportToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].password").doesNotExist())
			.andReturn();

		var ids = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
		var listedIds = new ArrayList<Long>();
		for (var item : ids) {
			listedIds.add(item.get("id").asLong());
		}
		assertTrue(listedIds.contains(soporte.getId()));
		assertTrue(listedIds.contains(segundoSoporte.getId()));
		assertFalse(listedIds.contains(inactivo.getId()));

		mockMvc.perform(get("/api/usuarios/soporte").header("Authorization", token(admin)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/usuarios/soporte").header("Authorization", token(laura)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/usuarios/soporte")).andExpect(status().isUnauthorized());
	}

	@Test
	void directoriosPaginadosLimitanTamanoYSoloIncluyenUsuariosActivos() throws Exception {
		mockMvc.perform(get("/api/usuarios/soporte").param("size", "2500")
				.header("Authorization", token(soporte)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(1000))
			.andExpect(jsonPath("$.content.length()").value(2));
		mockMvc.perform(get("/api/usuarios").param("size", "2500").header("Authorization", token(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(1000));
	}

	@Test
	void duplicadosErroresYOpenApiNoPublicaCambioDeRol() throws Exception {
		var adminToken = token(admin);
		String duplicado = nuevoCorreo();
		createdEmails.add(duplicado);
		usuarioRepository.save(new Usuario("Existente", duplicado, passwordEncoder.encode("Password123!"), Rol.USUARIO));
		mockMvc.perform(post("/api/usuarios").header("Authorization", adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"nombre":"Existente","correo":"%s","password":"Password123!"}
					""".formatted(duplicado)))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/usuarios/{id}", 99999999L).header("Authorization", adminToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/usuarios/soporte'].get.security[0].bearerAuth").exists())
			.andExpect(jsonPath("$.paths['/api/usuarios/{id}/rol']").doesNotExist());
	}

	@Test
	void tokenYCredencialesPreviasNoFuncionanTrasDesactivarCuenta() throws Exception {
		var adminToken = token(admin);
		String correo = nuevoCorreo();
		createdEmails.add(correo);
		var created = mockMvc.perform(post("/api/usuarios").header("Authorization", adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("""
					{"nombre":"Soporte revocable","correo":"%s","password":"Password123!"}
					""".formatted(correo)))
			.andExpect(status().isCreated()).andReturn();
		long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
		var cuenta = usuarioRepository.findById(id).orElseThrow();
		var oldToken = token(cuenta);

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/usuarios/{id}", id).header("Authorization", adminToken))
			.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"correo":"%s","password":"Password123!"}
				""".formatted(correo)))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/usuarios/soporte").header("Authorization", oldToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void soportePuedeAsignarseUnTicketDelDirectorioYAvanzarloAEnProceso() throws Exception {
		var ticket = ticketRepository.save(new Ticket("BK user management asignación",
				"Ticket de prueba para comprobar asignación desde el directorio.", CategoriaTicket.SOFTWARE,
				PrioridadTicket.MEDIA, laura, "Resumen", List.of()));
		createdTicketIds.add(ticket.getId());
		var supportToken = token(soporte);

		var directory = mockMvc.perform(get("/api/usuarios/soporte").header("Authorization", supportToken))
			.andExpect(status().isOk()).andReturn();
		var users = objectMapper.readTree(directory.getResponse().getContentAsString()).get("content");
		long selectedId = -1;
		for (var item : users) {
			if (item.get("id").asLong() == soporte.getId()) {
				selectedId = item.get("id").asLong();
			}
		}
		assertEquals(soporte.getId(), selectedId);

		mockMvc.perform(patch("/api/tickets/{id}/asignar", ticket.getId())
				.header("Authorization", supportToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tecnicoId\":" + selectedId + "}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("EN_PROCESO"))
			.andExpect(jsonPath("$.tecnicoAsignado.id").value(soporte.getId()));

		assertEquals(EstadoTicket.EN_PROCESO, ticketRepository.findById(ticket.getId()).orElseThrow().getEstado());
		List<HistorialEstadoTicket> history = historialRepository.findAllByTicketIdOrderByFechaCambioAsc(ticket.getId());
		assertEquals(1, history.size());
		assertEquals(EstadoTicket.EN_PROCESO, history.getFirst().getEstadoNuevo());
	}

	@Test
	void noPermiteAsignarNiCrearTicketParaCuentasInactivas() throws Exception {
		String correo = nuevoCorreo();
		var inactivo = usuarioRepository.save(new Usuario("Usuario inactivo", correo,
				passwordEncoder.encode("Password123!"), Rol.SOPORTE));
		createdEmails.add(correo);
		jdbcTemplate.update("UPDATE usuarios SET activo = FALSE WHERE id = ?", inactivo.getId());
		var ticket = ticketRepository.save(new Ticket("BK user management técnico inactivo",
				"Ticket para probar que una cuenta inactiva no se asigna.", CategoriaTicket.SOFTWARE,
				PrioridadTicket.MEDIA, laura, "Resumen", List.of()));
		createdTicketIds.add(ticket.getId());
		var adminToken = token(admin);

		mockMvc.perform(patch("/api/tickets/{id}/asignar", ticket.getId()).header("Authorization", adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":" + inactivo.getId() + "}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/tickets").header("Authorization", adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("""
					{"titulo":"Ticket para cuenta inactiva","descripcion":"Solicitud creada para validar el rechazo de solicitante inactivo.","usuarioId":%d,"categoria":"SOFTWARE","prioridad":"MEDIA"}
					""".formatted(inactivo.getId())))
			.andExpect(status().isBadRequest());
	}

	private String token(Usuario usuario) {
		return "Bearer " + jwtService.generarToken(usuario);
	}

	private String nuevoCorreo() {
		return "bk-user-management-" + UUID.randomUUID() + "@example.com";
	}
}
