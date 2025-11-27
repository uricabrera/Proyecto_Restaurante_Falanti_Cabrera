package com.restaurante.demo.service;

import com.restaurante.demo.dto.LoginResponseDTO;
import com.restaurante.demo.model.Chef;
import com.restaurante.demo.model.Cliente;
import com.restaurante.demo.model.Usuario;
import com.restaurante.demo.repository.ChefRepository;
import com.restaurante.demo.repository.ClienteRepository;
import com.restaurante.demo.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j // Report Section 6.3: Logging Infrastructure
public class UsuarioService {

    private final ChefRepository chefRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(ChefRepository chefRepository, 
                          ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.chefRepository = chefRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Authenticates a user and returns their ID and role.
     * Updated to use BCrypt comparison.
     */
    public LoginResponseDTO login(String username, String password) {
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Failed login attempt for user: {}", username);
            throw new RuntimeException("Invalid password");
        }

        String role = (user instanceof Chef) ? "Chef" : "Cliente";
        
        // Simple logic to detect Admin (in a real app, this would be a role in DB)
        if (username.equalsIgnoreCase("admin")) {
            role = "Admin";
        }
        
        log.info("User {} logged in successfully as {}", username, role);
        return new LoginResponseDTO(user.getUserId(), role);
    }

    // --- Chef Methods ---
    @Transactional
    public Chef createChef(Chef chef) {
        // Hash password before saving
        chef.setPassword(passwordEncoder.encode(chef.getPassword()));
        log.info("Creating new Chef: {}", chef.getUsername());
        return chefRepository.save(chef);
    }

    public Chef getChefById(Long id) {
        return chefRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chef not found with id: " + id));
    }
    
    public List<Chef> getAllChefs() {
        return chefRepository.findAll();
    }
    
    @Transactional
    public Chef updateChef(Long id, Chef chefDetails) {
        Chef existingChef = getChefById(id);
        existingChef.setNombre(chefDetails.getNombre());
        existingChef.setUsername(chefDetails.getUsername());
        existingChef.setStation(chefDetails.getStation());
        existingChef.setEfficiency(chefDetails.getEfficiency());
        // Note: Password updates should be handled via a separate secure endpoint
        return chefRepository.save(existingChef);
    }

    // --- Cliente Methods ---
    @Transactional
    public Cliente createCliente(Cliente cliente) {
        // Hash password before saving
        cliente.setPassword(passwordEncoder.encode(cliente.getPassword()));
        log.info("Creating new Cliente: {}", cliente.getUsername());
        return clienteRepository.save(cliente);
    }
    
    public Cliente getClienteById(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente not found with id: " + id));
    }

    public List<Cliente> getAllClientes() {
        return clienteRepository.findAll();
    }
}