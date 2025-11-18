package com.restaurante.demo.repository;

import com.restaurante.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Conseguir usuario por username
    Optional<Usuario> findByUsername(String username);
}
