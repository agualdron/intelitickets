package com.empresa.tickets.dto.usuario;

import com.empresa.tickets.entity.enums.Rol;

public record UsuarioResumenResponse(Long id, String nombre, String correo, Rol rol) {
}
