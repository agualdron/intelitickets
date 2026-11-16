package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.empresa.tickets.dto.auth.RegisterRequest;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.exception.RecursoNoEncontradoException;
import com.empresa.tickets.exception.ReglaNegocioException;
import com.empresa.tickets.mapper.UsuarioMapper;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTests {

	@Mock UsuarioRepository usuarioRepository;
	@Mock PasswordEncoder passwordEncoder;
	private UsuarioService service;

	@BeforeEach
	void setUp() {
		service = new UsuarioService(usuarioRepository, new UsuarioMapper(), passwordEncoder);
	}

	@Test
	void crearSoporteNormalizaCorreoYCodificaPassword() {
		when(usuarioRepository.existsByCorreoIgnoreCase("soporte.nuevo@example.com")).thenReturn(false);
		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.crear(new RegisterRequest("  Ana Soporte ", " SOPORTE.NUEVO@example.com ",
				"Password123!"));

		var saved = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(saved.capture());
		assertEquals("Ana Soporte", saved.getValue().getNombre());
		assertEquals("soporte.nuevo@example.com", saved.getValue().getCorreo());
		assertEquals("encoded-password", saved.getValue().getPassword());
		assertEquals(Rol.SOPORTE, saved.getValue().getRol());
		assertTrue(saved.getValue().isActivo());
		assertEquals("soporte.nuevo@example.com", response.correo());
		assertEquals(Rol.SOPORTE, response.rol());
	}

	@Test
	void correoDuplicadoNoPersisteUsuario() {
		when(usuarioRepository.existsByCorreoIgnoreCase("soporte@example.com")).thenReturn(true);

		assertThrows(ReglaNegocioException.class,
				() -> service.crear(new RegisterRequest("Ana", "soporte@example.com", "Password123!")));

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void eliminarDesactivaLaCuentaYEsIdempotente() {
		var usuario = new Usuario("Ana", "ana@example.com", "encoded", Rol.USUARIO);
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		service.eliminar(10L);
		assertFalse(usuario.isActivo());

		service.eliminar(10L);
		assertFalse(usuario.isActivo());
	}

	@Test
	void eliminarUsuarioInexistenteProduce404() {
		when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(999L));
	}

	@Test
	void listadosFiltranPorEstadoActivoYMapeanResumen() {
		var soporte = new Usuario("Soporte", "soporte@example.com", "secret-hash", Rol.SOPORTE);
		var page = new PageImpl<>(List.of(soporte), PageRequest.of(0, 20), 1);
		when(usuarioRepository.findAllByActivoTrue(PageRequest.of(0, 20))).thenReturn(page);
		when(usuarioRepository.findAllByRolAndActivoTrue(Rol.SOPORTE, PageRequest.of(0, 20))).thenReturn(page);

		var adminResponse = service.listar(PageRequest.of(0, 20));
		var supportResponse = service.listarSoporte(PageRequest.of(0, 20));

		assertEquals(1, adminResponse.getTotalElements());
		assertEquals(1, supportResponse.getTotalElements());
		assertEquals("soporte@example.com", supportResponse.getContent().getFirst().correo());
		assertEquals(Rol.SOPORTE, supportResponse.getContent().getFirst().rol());
	}
}
