package com.restaurante.demo.repository;

//Interfaz que incluye JPA para poder comunicarnos con la base de datos de manera eficiente

import com.restaurante.demo.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClienteRepository extends JpaRepository<Cliente, Long> {}
