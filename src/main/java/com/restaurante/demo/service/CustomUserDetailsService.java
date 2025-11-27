package com.restaurante.demo.service;

import com.restaurante.demo.model.Chef;
import com.restaurante.demo.model.Usuario;
import com.restaurante.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Determine Roles based on inheritance type
        if (usuario instanceof Chef) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CHEF"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));
        }
        
        // Hardcoded Admin for prototype purposes, as requested in typical academic projects
        if (username.equalsIgnoreCase("admin")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                authorities
        );
    }
}