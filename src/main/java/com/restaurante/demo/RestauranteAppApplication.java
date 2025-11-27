package com.restaurante.demo;

import com.restaurante.demo.model.Cliente;
import com.restaurante.demo.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class RestauranteAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestauranteAppApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Chequear si existe admin
			if (usuarioRepository.findByUsername("admin").isEmpty()) {
				System.out.println("--- Estableciendo cuenta admin ---");
				Cliente admin = new Cliente();
				admin.setNombre("System Administrator");
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123")); 
				admin.setDireccion("HQ");
				
				usuarioRepository.save(admin);
				System.out.println("--- ADMIN Creado: User: 'admin', Pass: 'admin123' ---");
			} else {
				System.out.println("--- Cuenta admin encontrada ---");
			}
		};
	}
}