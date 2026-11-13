package com.empresa.tickets.security;

import java.util.Locale;

import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
		String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
		Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoNormalizado)
			.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
		return User.withUsername(usuario.getCorreo())
			.password(usuario.getPassword())
			.authorities("ROLE_" + usuario.getRol().name())
			.build();
	}
}
