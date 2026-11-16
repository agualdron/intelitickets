package com.empresa.tickets.security;

import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityActorAutenticadoProvider implements ActorAutenticadoProvider {

	private final UsuarioRepository usuarioRepository;

	public SecurityActorAutenticadoProvider(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public Usuario obtenerActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
			throw new AuthenticationCredentialsNotFoundException("Autenticación requerida");
		}
		return usuarioRepository.findByCorreoIgnoreCase(authentication.getName())
			.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Autenticación requerida"));
	}
}
