package com.empresa.tickets.repository;

import com.empresa.tickets.entity.Usuario;
import com.empresa.tickets.entity.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCorreoIgnoreCase(String correo);

	boolean existsByCorreoIgnoreCase(String correo);

	Page<Usuario> findAllByActivoTrue(Pageable pageable);

	Page<Usuario> findAllByRolAndActivoTrue(Rol rol, Pageable pageable);
}
