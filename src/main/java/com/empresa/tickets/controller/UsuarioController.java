package com.empresa.tickets.controller;

import com.empresa.tickets.dto.auth.RegisterRequest;
import com.empresa.tickets.dto.usuario.UsuarioResumenResponse;
import com.empresa.tickets.service.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping
	public Page<UsuarioResumenResponse> listar(@ParameterObject Pageable pageable) {
		return usuarioService.listar(pageable);
	}

	@GetMapping("/soporte")
	public Page<UsuarioResumenResponse> listarSoporte(@ParameterObject Pageable pageable) {
		return usuarioService.listarSoporte(pageable);
	}

	@PostMapping
	public ResponseEntity<UsuarioResumenResponse> crear(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		usuarioService.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
