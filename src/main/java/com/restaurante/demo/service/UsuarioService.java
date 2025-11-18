package com.restaurante.demo.service;

import com.restaurante.demo.dto.LoginResponseDTO;
import com.restaurante.demo.model.Chef;
import com.restaurante.demo.model.Cliente;
import com.restaurante.demo.model.Usuario;
import com.restaurante.demo.repository.ChefRepository;
import com.restaurante.demo.repository.ClienteRepository;
import com.restaurante.demo.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final ChefRepository chefRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UsuarioService(ChefRepository chefRepository, ClienteRepository clienteRepository,UsuarioRepository usuarioRepository) {
        this.chefRepository = chefRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }
    
    
    /**
     * Authenticates a user and returns their ID and role.
     */
    public LoginResponseDTO login(String username, String password) {
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        
        
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String role = (user instanceof Chef) ? "Chef" : "Cliente";
        return new LoginResponseDTO(user.getUserId(), role);
    }

    // --- Chef Methods ---
    public Chef createChef(Chef chef) {
        
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
        // Note: Password changes would require more complex logic
        return chefRepository.save(existingChef);
    }

    // --- Cliente Methods ---
    public Cliente createCliente(Cliente cliente) {
        
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
