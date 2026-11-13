package com.empresa.tickets.mapper;

import com.empresa.tickets.dto.auth.AuthResponse;
import com.empresa.tickets.dto.auth.RegisterRequest;
import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.Rol;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

	public Usuario toEntity(RegisterRequest request, String encodedPassword, Rol rol) {
		return new Usuario(request.nombre().trim(), request.correo().trim(), encodedPassword, rol);
	}

	public AuthResponse toAuthResponse(Usuario usuario, String token) {
		return new AuthResponse(token, "Bearer", usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
			usuario.getRol());
	}
}
