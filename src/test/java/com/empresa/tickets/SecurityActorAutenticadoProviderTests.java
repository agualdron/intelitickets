package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.security.SecurityActorAutenticadoProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SecurityActorAutenticadoProviderTests {

	@Mock UsuarioRepository usuarioRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void requiresValidAuthentication() {
		SecurityContextHolder.clearContext();
		var provider = new SecurityActorAutenticadoProvider(usuarioRepository);

		assertThrows(AuthenticationCredentialsNotFoundException.class, provider::obtenerActor);
	}

	@Test
	void usesPersistedUserAndRoleAsActor() {
		var user = new Usuario("Laura Gómez", "laura.gomez@empresa.com", "hash", Rol.USUARIO);
		when(usuarioRepository.findByCorreoIgnoreCase(user.getCorreo())).thenReturn(Optional.of(user));
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
			user.getCorreo(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		var provider = new SecurityActorAutenticadoProvider(usuarioRepository);

		Usuario actor = provider.obtenerActor();

		assertEquals(user, actor);
		assertEquals(Rol.USUARIO, actor.getRol());
	}
}
