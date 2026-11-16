package com.empresa.tickets.service;

import java.util.Locale;

import com.empresa.tickets.dto.auth.RegisterRequest;
import com.empresa.tickets.dto.usuario.UsuarioResumenResponse;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.Rol;
import com.empresa.tickets.exception.RecursoNoEncontradoException;
import com.empresa.tickets.exception.ReglaNegocioException;
import com.empresa.tickets.mapper.UsuarioMapper;
import com.empresa.tickets.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final UsuarioMapper usuarioMapper;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioMapper = usuarioMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public Page<UsuarioResumenResponse> listar(Pageable pageable) {
		return usuarioRepository.findAllByActivoTrue(pageable).map(usuarioMapper::toResumenResponse);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioResumenResponse crear(RegisterRequest request) {
		String correo = normalizarCorreo(request.correo());
		if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
			throw new ReglaNegocioException("Ya existe un usuario con ese correo");
		}
		var normalizada = new RegisterRequest(request.nombre().trim(), correo, request.password());
		Usuario usuario = usuarioMapper.toEntity(normalizada, passwordEncoder.encode(request.password()), Rol.SOPORTE);
		return usuarioMapper.toResumenResponse(usuarioRepository.save(usuario));
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public void eliminar(Long id) {
		Usuario usuario = usuarioRepository.findById(id)
			.orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
		usuario.desactivar();
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('SOPORTE')")
	public Page<UsuarioResumenResponse> listarSoporte(Pageable pageable) {
		return usuarioRepository.findAllByRolAndActivoTrue(Rol.SOPORTE, pageable).map(usuarioMapper::toResumenResponse);
	}

	private String normalizarCorreo(String correo) {
		return correo.trim().toLowerCase(Locale.ROOT);
	}
}
