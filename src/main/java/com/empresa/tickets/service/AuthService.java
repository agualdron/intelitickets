package com.empresa.tickets.service;

import java.util.Locale;

import com.empresa.tickets.dto.auth.AuthResponse;
import com.empresa.tickets.dto.auth.LoginRequest;
import com.empresa.tickets.dto.auth.RegisterRequest;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.exception.ReglaNegocioException;
import com.empresa.tickets.mapper.UsuarioMapper;
import com.empresa.tickets.repository.UsuarioRepository;
import com.empresa.tickets.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final UsuarioMapper usuarioMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioMapper = usuarioMapper;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse registrar(RegisterRequest request) {
		String correo = normalizarCorreo(request.correo());
		if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
			throw new ReglaNegocioException("Ya existe un usuario con ese correo");
		}
		RegisterRequest normalizada = new RegisterRequest(request.nombre().trim(), correo, request.password());
		Usuario usuario = usuarioMapper.toEntity(normalizada, passwordEncoder.encode(request.password()), Rol.USUARIO);
		Usuario guardado = usuarioRepository.save(usuario);
		return usuarioMapper.toAuthResponse(guardado, jwtService.generarToken(guardado));
	}

	@Transactional(readOnly = true)
	public AuthResponse iniciarSesion(LoginRequest request) {
		String correo = normalizarCorreo(request.correo());
		authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(correo,
			request.password()));
		Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
			.orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
		return usuarioMapper.toAuthResponse(usuario, jwtService.generarToken(usuario));
	}

	private String normalizarCorreo(String correo) {
		return correo.trim().toLowerCase(Locale.ROOT);
	}
}
