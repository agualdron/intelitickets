package com.empresa.tickets.dto.auth;

import com.empresa.tickets.entity.enums.Rol;

public record AuthResponse(String token, String tokenType, Long id, String nombre, String correo, Rol rol) {
}
